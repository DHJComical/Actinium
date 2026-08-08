package com.dhj.actinium.loading.fml.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Injects the macOS forward-compatible GLFW hint into LWJGLXX's {@code Display.create()}.
 *
 * <p>Cleanroom's lwjglxx shim reads its OpenGL version and profile from ForgeEarlyConfig but never sets
 * {@code GLFW_OPENGL_FORWARD_COMPAT}. GLFW requires that hint on macOS for OpenGL 3.2+ core contexts,
 * so without it a requested 3.3/4.1 context can fall back to a legacy 2.1 context.</p>
 */
public final class MacDisplayForwardCompatTransformer implements IClassTransformer {
    private static final List<String> TARGET_CLASSES = List.of(
        "org.lwjgl.opengl.Display",
        "org.lwjglx.opengl.Display"
    );
    private static final int GLFW_OPENGL_FORWARD_COMPAT = 139270;
    private static final int GLFW_TRUE = 1;
    private static final int LWJGL_PLATFORM_MACOSX = 2;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isTarget(name) && !isTarget(transformedName)) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);
        boolean transformed = false;

        for (MethodNode method : classNode.methods) {
            if (!"create".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode methodCall)
                    || !"glfwDefaultWindowHints".equals(methodCall.name)
                    || !"()V".equals(methodCall.desc)
                    || !isGlfwOwner(methodCall.owner)) {
                    continue;
                }

                method.instructions.insert(methodCall, forwardCompatibleHint());
                transformed = true;
                break;
            }
            break;
        }

        if (!transformed) {
            return basicClass;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isTarget(String className) {
        return className != null && TARGET_CLASSES.contains(className);
    }

    private static boolean isGlfwOwner(String owner) {
        return "org/lwjgl/glfw/GLFW".equals(owner) || "org/lwjgl3/glfw/GLFW".equals(owner);
    }

    private static InsnList forwardCompatibleHint() {
        LabelNode skip = new LabelNode();
        InsnList instructions = new InsnList();
        instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "org/lwjgl/LWJGLUtil",
            "getPlatform",
            "()I",
            false
        ));
        instructions.add(new InsnNode(Opcodes.ICONST_2));
        instructions.add(new JumpInsnNode(Opcodes.IF_ICMPNE, skip));
        instructions.add(new LdcInsnNode(GLFW_OPENGL_FORWARD_COMPAT));
        instructions.add(new InsnNode(Opcodes.ICONST_1));
        instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "org/lwjgl/glfw/GLFW",
            "glfwWindowHint",
            "(II)V",
            false
        ));
        instructions.add(skip);
        return instructions;
    }
}
