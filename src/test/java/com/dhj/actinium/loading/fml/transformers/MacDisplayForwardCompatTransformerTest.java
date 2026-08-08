package com.dhj.actinium.loading.fml.transformers;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacDisplayForwardCompatTransformerTest {

    @Test
    void insertsMacForwardCompatHintIntoLwjglDisplayCreate() throws Exception {
        byte[] original = readDisplayClass();
        MacDisplayForwardCompatTransformer transformer = new MacDisplayForwardCompatTransformer();

        byte[] transformed = transformer.transform(
            "org.lwjgl.opengl.Display",
            "org.lwjgl.opengl.Display",
            original
        );

        ClassNode classNode = new ClassNode();
        new ClassReader(transformed).accept(classNode, 0);
        MethodNode create = classNode.methods.stream()
            .filter(method -> "create".equals(method.name) && "()V".equals(method.desc))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Display.create()V not found"));

        boolean insertedHint = false;
        for (AbstractInsnNode instruction = create.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode methodCall
                && Opcodes.INVOKESTATIC == methodCall.getOpcode()
                && "org/lwjgl/glfw/GLFW".equals(methodCall.owner)
                && "glfwWindowHint".equals(methodCall.name)
                && "(II)V".equals(methodCall.desc)) {
                insertedHint = true;
                break;
            }
        }

        assertTrue(insertedHint, "Forward-compatible GLFW hint call was not injected");
    }

    @Test
    void usesTheMacForwardCompatibleHintConstant() throws Exception {
        byte[] original = readDisplayClass();
        MacDisplayForwardCompatTransformer transformer = new MacDisplayForwardCompatTransformer();

        byte[] transformed = transformer.transform(
            "org.lwjgl.opengl.Display",
            "org.lwjgl.opengl.Display",
            original
        );

        ClassNode classNode = new ClassNode();
        new ClassReader(transformed).accept(classNode, 0);
        MethodNode create = classNode.methods.stream()
            .filter(method -> "create".equals(method.name) && "()V".equals(method.desc))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Display.create()V not found"));

        boolean hasForwardCompatConstant = false;
        for (AbstractInsnNode instruction = create.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof LdcInsnNode ldc
                && ldc.cst instanceof Integer value
                && value == 139270) {
                hasForwardCompatConstant = true;
                break;
            }
        }

        assertTrue(hasForwardCompatConstant, "GLFW_OPENGL_FORWARD_COMPAT constant was not injected");
    }

    private static byte[] readDisplayClass() throws Exception {
        try (InputStream stream = MacDisplayForwardCompatTransformerTest.class.getClassLoader()
            .getResourceAsStream("org/lwjgl/opengl/Display.class")) {
            assertNotNull(stream, "Display.class is not on the test classpath");
            return stream.readAllBytes();
        }
    }
}
