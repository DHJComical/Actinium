package com.gtnewhorizons.angelica.glsm.redirect;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the GLSM redirector rewrites vanilla
 * {@code net.minecraft.client.renderer.GlStateManager} calls (e.g. the color set
 * used by JourneyMap's map grid) to the GLSM state cache. This is the mechanism
 * that keeps the FFP shader's {@code u_CurrentColor} uniform in sync with vanilla
 * fixed-function color calls on the core-profile context.
 */
class GLSMRedirectorTest {

    private static final String VANILLA_GL_STATE_MANAGER = "net/minecraft/client/renderer/GlStateManager";
    private static final String GLSM_GL_STATE_MANAGER = "com/gtnewhorizons/angelica/glsm/GLStateManager";

    @Test
    void rewritesVanillaGlStateManagerColorToGlsmCache() {
        byte[] classBytes = generateClassCallingVanillaColor();
        GLSMRedirector redirector = new GLSMRedirector();

        assertTrue(redirector.shouldTransform(classBytes), "class referencing GlStateManager.color must be a redirect candidate");

        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, 0);
        boolean changed = redirector.transformClassNode("sample/Class", cn);
        assertTrue(changed, "GlStateManager.color call must be rewritten");

        MethodNode render = cn.methods.stream()
            .filter(m -> m.name.equals("render"))
            .findFirst()
            .orElseThrow();
        boolean redirected = false;
        for (AbstractInsnNode node : render.instructions) {
            if (node instanceof MethodInsnNode call
                && call.getOpcode() == Opcodes.INVOKESTATIC
                && call.owner.equals(GLSM_GL_STATE_MANAGER)
                && call.name.equals("glColor4f")) {
                redirected = true;
                break;
            }
        }
        assertTrue(redirected, "call must be redirected to GLSM GLStateManager.glColor4f");
    }

    @Test
    void rewritesVanillaRotateDoubleToGlsmGlRotatefPreservingDescriptor() {
        // NTM-CE calls GlStateManager.rotate(double, float, float, float) from its tile entity
        // item renderers (issue #64); the redirector must rewrite it to the GLSM GLStateManager
        // with the same (DFFF)V descriptor so the runtime link matches an existing overload.
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "sample/Rotator", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "render", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(90.0D); // double angle
        mv.visitInsn(Opcodes.FCONST_0);
        mv.visitInsn(Opcodes.FCONST_1);
        mv.visitInsn(Opcodes.FCONST_0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, VANILLA_GL_STATE_MANAGER, "rotate", "(DFFF)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
        cw.visitEnd();

        byte[] classBytes = cw.toByteArray();
        GLSMRedirector redirector = new GLSMRedirector();

        assertTrue(redirector.shouldTransform(classBytes), "class referencing GlStateManager.rotate must be a redirect candidate");

        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, 0);
        boolean changed = redirector.transformClassNode("sample/Rotator", cn);
        assertTrue(changed, "GlStateManager.rotate call must be rewritten");

        MethodNode render = cn.methods.stream()
            .filter(m -> m.name.equals("render"))
            .findFirst()
            .orElseThrow();
        boolean redirected = false;
        for (AbstractInsnNode node : render.instructions) {
            if (node instanceof MethodInsnNode call
                && call.getOpcode() == Opcodes.INVOKESTATIC
                && call.owner.equals(GLSM_GL_STATE_MANAGER)
                && call.name.equals("glRotatef")
                && call.desc.equals("(DFFF)V")) {
                redirected = true;
                break;
            }
        }
        assertTrue(redirected, "GlStateManager.rotate(DFFF) must be redirected to GLSM GLStateManager.glRotatef(DFFF)");
    }

    @Test
    void untouchedClassWithoutGlReferencesIsNotTransformed() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "sample/Plain", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "ping", "()I", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();

        GLSMRedirector redirector = new GLSMRedirector();
        assertEquals(false, redirector.shouldTransform(cw.toByteArray()));
    }

    private static byte[] generateClassCallingVanillaColor() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "sample/Class", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "render", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(0.5F);
        mv.visitLdcInsn(0.5F);
        mv.visitLdcInsn(0.5F);
        mv.visitLdcInsn(0.5F);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, VANILLA_GL_STATE_MANAGER, "color", "(FFFF)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
