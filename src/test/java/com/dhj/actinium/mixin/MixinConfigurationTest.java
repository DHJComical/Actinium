package com.dhj.actinium.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinConfigurationTest {
    private static final String MIXIN_DESCRIPTOR = Type.getDescriptor(Mixin.class);
    private static final String BRIDGE_CONFIG = "celeritas-compat-bridge.mixin.json";
    private static final String EXPECTED_REFMAP = "mixins.actinium-refmap.json";
    private static final List<String> MAIN_CONFIGS = List.of(
        "mixins.actinium.vintage.json",
        "mixins.actinium.iris.json",
        "mixins.actinium.dh.json",
        "mixins.actinium.gibbed.json",
        "mixins.actinium.ichunutil.json",
        "mixins.actinium.lumenized.json",
        "mixins.actinium.revoui.json"
    );
    private static final List<String> CONFIGS = Stream.concat(
        Stream.of(BRIDGE_CONFIG),
        MAIN_CONFIGS.stream()
    ).toList();

    @Test
    void everyDeclaredMixinClassExists() throws IOException {
        ClassLoader classLoader = MixinConfigurationTest.class.getClassLoader();

        for (String configName : CONFIGS) {
            JsonObject config = readConfig(classLoader, configName);
            String packageName = config.get("package").getAsString();
            assertDeclaredClassesExist(classLoader, configName, packageName, config.getAsJsonArray("mixins"));
            assertDeclaredClassesExist(classLoader, configName, packageName, config.getAsJsonArray("client"));
            assertDeclaredClassesExist(classLoader, configName, packageName, config.getAsJsonArray("server"));
        }
    }

    @Test
    void everyCompiledMainMixinIsDeclaredExactlyOnce() throws IOException, URISyntaxException {
        ClassLoader classLoader = MixinConfigurationTest.class.getClassLoader();
        Map<String, String> declaredMixins = declaredMainMixins(classLoader);
        Set<String> compiledMixins = compiledMainMixins(classLoader);

        assertEquals(compiledMixins, declaredMixins.keySet());
    }

    @Test
    void earlyAndLateLoadersCoverEveryMainMixinConfig() throws IOException {
        ClassLoader classLoader = MixinConfigurationTest.class.getClassLoader();
        Set<String> earlyConfigs = compiledMixinConfigs(
            classLoader, "com/dhj/actinium/mixins/MixinEarly.class", "<clinit>");
        Set<String> lateConfigs = compiledMixinConfigs(
            classLoader, "com/dhj/actinium/mixins/MixinLate.class", "configsFor");
        Set<String> allLoadedConfigs = new HashSet<>(earlyConfigs);

        assertTrue(earlyConfigs.stream().noneMatch(lateConfigs::contains),
            "A Mixin config cannot be both early and late");
        assertFalse(earlyConfigs.contains(BRIDGE_CONFIG),
            "The compatibility bridge is loaded through the Forge manifest, not MixinEarly");
        assertFalse(lateConfigs.contains(BRIDGE_CONFIG),
            "The compatibility bridge is loaded through the Forge manifest, not MixinLate");
        allLoadedConfigs.addAll(lateConfigs);
        assertEquals(Set.copyOf(MAIN_CONFIGS), allLoadedConfigs);
    }

    @Test
    void mainConfigsDeclareExpectedMetadataAndRefmapNames() throws IOException {
        ClassLoader classLoader = MixinConfigurationTest.class.getClassLoader();

        for (String configName : MAIN_CONFIGS) {
            JsonObject config = readConfig(classLoader, configName);
            assertEquals("0.8.5", config.get("minVersion").getAsString(), configName + " minVersion");
            assertEquals("JAVA_8", config.get("compatibilityLevel").getAsString(), configName + " compatibilityLevel");
            assertTrue(config.get("required").getAsBoolean(), configName + " required");

            String refmap = config.get("refmap").getAsString();
            assertFalse(refmap.isBlank(), configName + " must declare a non-empty refmap");
            assertEquals(EXPECTED_REFMAP, refmap, configName + " refmap");
        }
    }

    @Test
    void bridgeConfigCarriesLoaderSpecificMetadata() throws IOException {
        ClassLoader classLoader = MixinConfigurationTest.class.getClassLoader();
        JsonObject config = readConfig(classLoader, BRIDGE_CONFIG);

        assertEquals("0.8.7", config.get("minVersion").getAsString());
        assertEquals("JAVA_8", config.get("compatibilityLevel").getAsString());
        assertEquals("@env(MOD)", config.get("target").getAsString());
        assertTrue(config.get("required").getAsBoolean());
        assertFalse(config.has("refmap"),
            "The compatibility bridge is loaded through the Forge manifest and must not share Actinium's refmap");
    }

    private static Set<String> compiledMixinConfigs(
        ClassLoader classLoader,
        String resourceName,
        String methodName
    ) throws IOException {
        ClassNode node = readClass(classLoader, resourceName);
        MethodNode configMethod = node.methods.stream()
            .filter(method -> methodName.equals(method.name))
            .findFirst()
            .orElseThrow(() -> new AssertionError(resourceName + " has no " + methodName + " method"));
        Set<String> configs = new HashSet<>();

        for (var instruction = configMethod.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof LdcInsnNode ldc
                && ldc.cst instanceof String value
                && value.endsWith(".json")) {
                configs.add(value);
            }
        }
        return configs;
    }

    private static JsonObject readConfig(ClassLoader classLoader, String configName) throws IOException {
        try (Reader reader = new InputStreamReader(
            Objects.requireNonNull(classLoader.getResourceAsStream(configName), "Missing " + configName),
            StandardCharsets.UTF_8
        )) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Map<String, String> declaredMainMixins(ClassLoader classLoader) throws IOException {
        Map<String, String> declarations = new HashMap<>();
        for (String configName : MAIN_CONFIGS) {
            JsonObject config = readConfig(classLoader, configName);
            String packageName = config.get("package").getAsString();
            addDeclarations(declarations, configName, packageName, config.getAsJsonArray("mixins"));
            addDeclarations(declarations, configName, packageName, config.getAsJsonArray("client"));
            addDeclarations(declarations, configName, packageName, config.getAsJsonArray("server"));
        }
        return declarations;
    }

    private static void addDeclarations(
        Map<String, String> declarations,
        String configName,
        String packageName,
        JsonArray entries
    ) {
        if (entries == null) {
            return;
        }
        for (var entry : entries) {
            String className = packageName + "." + entry.getAsString();
            assertNull(declarations.put(className, configName), className + " is declared by multiple Mixin configs");
        }
    }

    private static Set<String> compiledMainMixins(ClassLoader classLoader)
        throws IOException, URISyntaxException {
        String anchorResource = "com/dhj/actinium/mixin/core/terrain/BufferBuilderMixin.class";
        URL anchorUrl = Objects.requireNonNull(classLoader.getResource(anchorResource), "Missing " + anchorResource);
        assertEquals("file", anchorUrl.getProtocol(), "Compiled Mixin classes must be available as files during tests");

        Path mixinRoot = Path.of(anchorUrl.toURI()).getParent().getParent().getParent();
        Set<String> mixins = new HashSet<>();
        try (Stream<Path> paths = Files.walk(mixinRoot)) {
            for (Path classFile : paths.filter(path -> path.getFileName().toString().endsWith(".class")).toList()) {
                ClassNode node = new ClassNode();
                try (var stream = Files.newInputStream(classFile)) {
                    new ClassReader(stream).accept(node,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                if (hasMixinAnnotation(node.visibleAnnotations) || hasMixinAnnotation(node.invisibleAnnotations)) {
                    mixins.add(node.name.replace('/', '.'));
                }
            }
        }
        return mixins;
    }

    private static boolean hasMixinAnnotation(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream().anyMatch(annotation -> MIXIN_DESCRIPTOR.equals(annotation.desc));
    }

    private static ClassNode readClass(ClassLoader classLoader, String resourceName) throws IOException {
        ClassNode node = new ClassNode();
        try (var stream = Objects.requireNonNull(classLoader.getResourceAsStream(resourceName),
            "Missing " + resourceName)) {
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }

    private static void assertDeclaredClassesExist(
        ClassLoader classLoader,
        String configName,
        String packageName,
        JsonArray declarations
    ) {
        if (declarations == null) {
            return;
        }

        for (var declaration : declarations) {
            String className = packageName + "." + declaration.getAsString();
            String resourceName = className.replace('.', '/') + ".class";
            assertNotNull(classLoader.getResource(resourceName), configName + " references missing " + className);
            assertFalse(className.contains(".."), configName + " contains an invalid class name");
        }
    }
}
