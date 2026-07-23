package org.taumc.celeritas;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CeleritasCompatBridgeJarTest {
    private static final String ENTRY_CLASS = "org/taumc/celeritas/CeleritasVintage.class";
    private static final String MIXIN_CLASS =
            "org/taumc/celeritas/compat/mixin/CeleritasTileEntityRendererDispatcherMixin.class";
    private static final String LEGACY_RENDERER_ACCESS_CLASS =
            "org/taumc/celeritas/compat/LegacyRendererAccess.class";
    private static final String LEGACY_RENDERER_CLASS =
            "org/taumc/celeritas/impl/render/terrain/compile/pipeline/VintageBlockRenderer.class";
    private static final String MIXIN_CONFIG = "celeritas-compat-bridge.mixin.json";
    private static final String TARGET_OWNER =
            "net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher";
    private static final String RENDER_DESCRIPTOR = "(Lnet/minecraft/tileentity/TileEntity;FI)V";
    private static final String BLOCK_ACCESS_OWNER = "net/minecraft/world/IBlockAccess";
    private static final String WORLD_TYPE_DESCRIPTOR = "()Lnet/minecraft/world/WorldType;";

    @Test
    void publishesCompleteForgeModJar() throws IOException {
        try (JarFile jar = openBridgeJar()) {
            for (String entry : List.of(
                    ENTRY_CLASS, "mcmod.info", MIXIN_CONFIG, MIXIN_CLASS, LEGACY_RENDERER_ACCESS_CLASS)) {
                assertNotNull(jar.getJarEntry(entry), "Missing compatibility bridge entry " + entry);
            }
            assertDeclaredMixinClassesPresent(jar, readJsonObject(jar, MIXIN_CONFIG));

            Attributes manifest = jar.getManifest().getMainAttributes();
            assertEquals("CRL", manifest.getValue("ModType"));
            assertEquals(MIXIN_CONFIG, manifest.getValue("MixinConfigs"));

            JsonArray metadata = readJsonArray(jar, "mcmod.info");
            JsonObject mod = metadata.get(0).getAsJsonObject();
            assertEquals("celeritas", mod.get("modid").getAsString());
            assertEquals(System.getProperty("actinium.modVersion"), mod.get("version").getAsString());
        }
    }

    @Test
    void entrypointRetainsLegacyForgeAbiAndBridgeResponsibilities() throws IOException {
        try (JarFile jar = openBridgeJar(); InputStream stream = jar.getInputStream(jar.getJarEntry(ENTRY_CLASS))) {
            ClassNode entrypoint = readClass(stream);
            assertEquals(0, entrypoint.access & Opcodes.ACC_FINAL, "The upstream entrypoint ABI is extensible");
            assertTrue(entrypoint.fields.stream().anyMatch(field -> field.name.equals("MODID")
                    && field.desc.equals("Ljava/lang/String;") && field.value.equals("celeritas")));
            assertTrue(entrypoint.fields.stream().anyMatch(field -> field.name.equals("VERSION")
                    && field.desc.equals("Ljava/lang/String;") && field.value == null));

            AnnotationNode modAnnotation = entrypoint.visibleAnnotations.stream()
                    .filter(annotation -> annotation.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))
                    .findFirst()
                    .orElseThrow();
            Map<String, Object> values = annotationValues(modAnnotation);
            assertEquals("celeritas", values.get("modid"));
            assertEquals(Boolean.TRUE, values.get("useMetadata"));
            assertEquals("[1.12.2]", values.get("acceptedMinecraftVersions"));
            assertEquals("*", values.get("acceptableRemoteVersions"));
            assertEquals(Boolean.TRUE, values.get("clientSideOnly"));
            assertEquals("required-after:actinium", values.get("dependencies"));
            assertFalse(values.containsKey("version"), "Forge metadata must remain the only version source");

            MethodNode construction = entrypoint.methods.stream()
                    .filter(method -> method.name.equals("construction")
                            && method.desc.equals("(Lnet/minecraftforge/fml/common/event/FMLConstructionEvent;)V"))
                    .findFirst()
                    .orElseThrow();
            assertMethodCall(construction, "net/minecraftforge/fml/common/Loader", "instance");
            assertMethodCall(construction, "net/minecraftforge/fml/common/Loader", "getIndexedModList");
            assertMethodCall(construction, "net/minecraftforge/fml/common/ModContainer", "getVersion");
            assertMethodCall(construction, "org/taumc/celeritas/compat/CeleritasLegacyEventBridge", "install");
        }
    }

    @Test
    void remapsMinecraftCallsToProductionNames() throws IOException {
        try (JarFile jar = openBridgeJar(); InputStream stream = jar.getInputStream(jar.getJarEntry(MIXIN_CLASS))) {
            ClassNode mixin = readClass(stream);
            MethodInsnNode renderCall = findTargetRenderCall(mixin.methods);

            assertEquals("func_180546_a", renderCall.name);
        }
    }

    @Test
    void remapsLegacyRendererWorldTypeCallThroughVanillaBlockAccess() throws IOException {
        try (JarFile jar = openBridgeJar();
             InputStream stream = jar.getInputStream(jar.getJarEntry(LEGACY_RENDERER_CLASS))) {
            ClassNode renderer = readClass(stream);
            MethodInsnNode worldTypeCall = findMethodCall(
                    renderer.methods, BLOCK_ACCESS_OWNER, WORLD_TYPE_DESCRIPTOR);

            assertEquals("func_175624_G", worldTypeCall.name);
        }
    }

    @Test
    void managedMixinPackageContainsOnlyDeclaredMixinImplementations() throws IOException {
        try (JarFile jar = openBridgeJar()) {
            JsonObject config = readJsonObject(jar, MIXIN_CONFIG);
            String packagePath = config.get("package").getAsString().replace('.', '/');
            Set<String> declaredClasses = declaredMixinClasses(config, packagePath);
            Set<String> packagedClasses = new HashSet<>();

            jar.stream()
                    .map(entry -> entry.getName())
                    .filter(name -> name.startsWith(packagePath + "/") && name.endsWith(".class"))
                    .forEach(packagedClasses::add);

            assertEquals(declaredClasses, packagedClasses,
                    "The managed Mixin package must not contain directly loaded bridge contracts");
            assertFalse(LEGACY_RENDERER_ACCESS_CLASS.startsWith(packagePath + "/"),
                    "The renderer access contract must remain outside the managed Mixin package");
        }
    }

    @Test
    void legacyRendererReferencesAccessContractOutsideManagedMixinPackage() throws IOException {
        try (JarFile jar = openBridgeJar();
             InputStream stream = jar.getInputStream(jar.getJarEntry(LEGACY_RENDERER_CLASS))) {
            ClassNode renderer = readClass(stream);
            assertTrue(referencesType(renderer, "org/taumc/celeritas/compat/LegacyRendererAccess"));
            assertFalse(referencesType(renderer, "org/taumc/celeritas/compat/mixin/LegacyRendererAccess"));
        }
    }

    private static boolean referencesType(ClassNode classNode, String internalName) {
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof TypeInsnNode typeInstruction
                        && typeInstruction.desc.equals(internalName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static MethodInsnNode findTargetRenderCall(List<MethodNode> methods) {
        return findMethodCall(methods, TARGET_OWNER, RENDER_DESCRIPTOR);
    }

    private static MethodInsnNode findMethodCall(List<MethodNode> methods, String owner, String descriptor) {
        for (MethodNode method : methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode methodCall
                        && methodCall.owner.equals(owner)
                        && methodCall.desc.equals(descriptor)) {
                    return methodCall;
                }
            }
        }
        throw new AssertionError("Missing method call " + owner + "." + descriptor);
    }

    private static void assertMethodCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode methodCall
                    && methodCall.owner.equals(owner) && methodCall.name.equals(name)) {
                return;
            }
        }
        throw new AssertionError("Missing method call " + owner + "." + name);
    }

    private static JarFile openBridgeJar() throws IOException {
        String path = System.getProperty("actinium.compatBridgeJar");
        assertNotNull(path, "Gradle must provide the remapped compatibility bridge path");
        return new JarFile(Path.of(path).toFile());
    }

    private static ClassNode readClass(InputStream stream) throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static JsonArray readJsonArray(JarFile jar, String entry) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                jar.getInputStream(jar.getJarEntry(entry)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }

    private static JsonObject readJsonObject(JarFile jar, String entry) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                jar.getInputStream(jar.getJarEntry(entry)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void assertDeclaredMixinClassesPresent(JarFile jar, JsonObject config) {
        String packagePath = config.get("package").getAsString().replace('.', '/');
        for (String section : List.of("mixins", "client", "server")) {
            JsonArray declarations = config.getAsJsonArray(section);
            if (declarations == null) {
                continue;
            }
            for (var declaration : declarations) {
                String classEntry = packagePath + "/" + declaration.getAsString() + ".class";
                assertNotNull(jar.getJarEntry(classEntry),
                        MIXIN_CONFIG + " declares missing Mixin class " + classEntry);
            }
        }
    }

    private static Set<String> declaredMixinClasses(JsonObject config, String packagePath) {
        Set<String> result = new HashSet<>();
        for (String section : List.of("mixins", "client", "server")) {
            JsonArray declarations = config.getAsJsonArray(section);
            if (declarations == null) {
                continue;
            }
            for (var declaration : declarations) {
                result.add(packagePath + "/" + declaration.getAsString() + ".class");
            }
        }
        return result;
    }

    private static Map<String, Object> annotationValues(AnnotationNode annotation) {
        Map<String, Object> result = new HashMap<>();
        for (int index = 0; index < annotation.values.size(); index += 2) {
            result.put((String) annotation.values.get(index), annotation.values.get(index + 1));
        }
        return result;
    }
}
