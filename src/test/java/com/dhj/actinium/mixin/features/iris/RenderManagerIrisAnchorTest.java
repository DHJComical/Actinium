package com.dhj.actinium.mixin.features.iris;

import net.minecraft.client.renderer.entity.RenderManager;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the injection anchor of {@link RenderManagerIrisMixin}: the entity
 * context hook must attach to the entry point of
 * {@code RenderManager.renderEntity}, never to a call site inside its body.
 *
 * <p>LagGoggles (through TickCentral's {@code RenderManagerTransformer}) rewrites
 * this method before Mixin sees the class: the original body moves into a
 * {@code laggoggles_trueRender} method and the entry method becomes a forwarder
 * to {@code RenderManagerAdapter.redirectRenderEntity}. The rewrite is simulated
 * here on the real dev {@code RenderManager} class, the same way
 * {@link SomniaEntityRendererAnchorTest} simulates Somnia's {@code renderWorld}
 * rewrite, so the failure mode of issue #166 stays reproducible without the mod.
 */
class RenderManagerIrisAnchorTest {

    private static final String ENTRY_NAME = "renderEntity";
    private static final String ENTRY_DESC = "(Lnet/minecraft/entity/Entity;DDDFFZ)V";
    private static final String MOVED_BODY_NAME = "laggoggles_trueRender";
    private static final String RENDER_MANAGER = "net/minecraft/client/renderer/entity/RenderManager";
    private static final String RENDER = "net/minecraft/client/renderer/entity/Render";
    private static final String ADAPTER = "com/github/terminatornl/laggoggles/tickcentral/RenderManagerAdapter";
    private static final String CALL_SITE_REDIRECT = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String CALL_SITE_WRAP_OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";

    @Test
    void entryPointAnchorSurvivesLagGogglesBodyHoist() throws Exception {
        ClassNode renderManager = readClass(RenderManager.class, "RenderManager.class");
        MethodNode vanillaEntry = findMethod(renderManager, ENTRY_NAME, ENTRY_DESC);
        assertNotNull(vanillaEntry, "The vanilla entry method the hook targets must exist");
        assertTrue(
            hasDoRenderCall(vanillaEntry),
            "Precondition: the vanilla body must contain the Render#doRender call site a redirect would anchor on"
        );

        applyLagGogglesRewrite(renderManager);

        MethodNode entryAfterRewrite = findMethod(renderManager, ENTRY_NAME, ENTRY_DESC);
        assertNotNull(
            entryAfterRewrite,
            "The entry method must survive the rewrite, otherwise the render entity hook cannot be applied at all"
        );
        assertFalse(
            hasDoRenderCall(entryAfterRewrite),
            "A call site redirect inside the entry method would find no target after the rewrite, which is the"
                + " InjectionError that aborted the RenderManager class transform in issue #166"
        );

        MethodNode movedBody = findMethod(renderManager, MOVED_BODY_NAME, ENTRY_DESC);
        assertNotNull(movedBody, "The rewrite must move the original body into the LagGoggles method");
        assertTrue(hasDoRenderCall(movedBody), "The moved body must keep the entity render call");
        assertTrue(
            callsMethod(
                entryAfterRewrite,
                ADAPTER,
                "redirectRenderEntity",
                "(L" + RENDER_MANAGER + ";" + ENTRY_DESC.substring(1)
            ),
            "The entry method must forward the render to the hoisted body, so wrapping the entry point still"
                + " surrounds the entity render"
        );
    }

    @Test
    void renderEntityHookDoesNotAnchorOnCallSites() throws Exception {
        ClassNode mixin = readClass(RenderManagerIrisMixin.class, "RenderManagerIrisMixin.class");

        boolean anchorsEntryPoint = false;
        for (MethodNode handler : mixin.methods) {
            for (AnnotationNode annotation : annotationsOf(handler)) {
                if (!selectsRenderEntity(annotation)) {
                    continue;
                }
                assertFalse(
                    CALL_SITE_REDIRECT.equals(annotation.desc) || CALL_SITE_WRAP_OPERATION.equals(annotation.desc),
                    handler.name + " anchors on a call site inside " + ENTRY_NAME
                        + ", which LagGoggles moves into " + MOVED_BODY_NAME
                );
                anchorsEntryPoint = true;
            }
        }

        assertTrue(anchorsEntryPoint, "RenderManagerIrisMixin must keep hooking " + ENTRY_NAME + ENTRY_DESC);
    }

    /**
     * Mirrors {@code RenderManagerTransformer#transform}: same signature, body moved, forwarder added.
     * The transformer additionally rewrites the adapter's own {@code renderEntity} invocation to
     * {@code laggoggles_trueRender}, which is what connects the forwarder to the hoisted body.
     */
    private static void applyLagGogglesRewrite(ClassNode renderManager) {
        MethodNode body = null;
        for (MethodNode method : renderManager.methods) {
            if (method.desc.endsWith(";DDDFFZ)V")) {
                body = method;
                break;
            }
        }
        assertNotNull(body, "The LagGoggles transformer must find the render method by its descriptor");

        MethodNode forwarder = new MethodNode(
            Opcodes.ASM9,
            body.access,
            body.name,
            body.desc,
            body.signature,
            body.exceptions.toArray(new String[0])
        );
        forwarder.instructions = new InsnList();
        forwarder.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        forwarder.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        forwarder.instructions.add(new VarInsnNode(Opcodes.DLOAD, 2));
        forwarder.instructions.add(new VarInsnNode(Opcodes.DLOAD, 4));
        forwarder.instructions.add(new VarInsnNode(Opcodes.DLOAD, 6));
        forwarder.instructions.add(new VarInsnNode(Opcodes.FLOAD, 8));
        forwarder.instructions.add(new VarInsnNode(Opcodes.FLOAD, 9));
        forwarder.instructions.add(new VarInsnNode(Opcodes.ILOAD, 10));
        forwarder.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            ADAPTER,
            "redirectRenderEntity",
            "(L" + RENDER_MANAGER + ";" + body.desc.substring(1),
            false
        ));
        forwarder.instructions.add(new InsnNode(Opcodes.RETURN));
        renderManager.methods.add(forwarder);

        body.name = MOVED_BODY_NAME;
    }

    private static boolean hasDoRenderCall(MethodNode method) {
        return callsMethod(method, RENDER, "doRender", "(Lnet/minecraft/entity/Entity;DDDFF)V");
    }

    private static boolean callsMethod(MethodNode method, String owner, String name, String desc) {
        for (int index = 0; index < method.instructions.size(); index++) {
            if (method.instructions.get(index) instanceof MethodInsnNode call
                && call.owner.equals(owner)
                && call.name.equals(name)
                && call.desc.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    private static MethodNode findMethod(ClassNode owner, String name, String desc) {
        for (MethodNode method : owner.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    private static List<AnnotationNode> annotationsOf(MethodNode method) {
        if (method.visibleAnnotations == null) {
            return List.of();
        }
        return method.visibleAnnotations;
    }

    /** Reads the {@code method} selector of an injection annotation, tolerating a single value or an array. */
    private static boolean selectsRenderEntity(AnnotationNode annotation) {
        if (annotation.values == null) {
            return false;
        }
        for (int index = 0; index + 1 < annotation.values.size(); index += 2) {
            if (!"method".equals(annotation.values.get(index))) {
                continue;
            }
            Object value = annotation.values.get(index + 1);
            if (value instanceof String selector) {
                return selector.contains(ENTRY_NAME + ENTRY_DESC);
            }
            if (value instanceof List<?> selectors) {
                for (Object selector : selectors) {
                    if (selector instanceof String text && text.contains(ENTRY_NAME + ENTRY_DESC)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ClassNode readClass(Class<?> owner, String resourceName) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream stream = owner.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new AssertionError(resourceName + " is not on the test classpath");
            }
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }
}
