package com.dhj.actinium.loading.fml.transformers;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
                && "org/lwjgl3/glfw/GLFW".equals(methodCall.owner)
                && "glfwWindowHint".equals(methodCall.name)
                && "(II)V".equals(methodCall.desc)
                && isForwardCompatHintCall(methodCall)) {
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

    private static boolean isForwardCompatHintCall(MethodInsnNode methodCall) {
        AbstractInsnNode hintValue = methodCall.getPrevious();
        AbstractInsnNode hintConstant = hintValue == null ? null : hintValue.getPrevious();
        return hintConstant instanceof LdcInsnNode constant
            && constant.cst instanceof Integer value
            && value == GLFW_OPENGL_FORWARD_COMPAT;
    }

    @Test
    void recomputesStackMapFramesForEveryBranchTarget() throws Exception {
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

        Set<LabelNode> jumpTargets = new HashSet<>();
        for (AbstractInsnNode instruction = create.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof JumpInsnNode jump) {
                jumpTargets.add(jump.label);
            }
        }

        assertFalse(jumpTargets.isEmpty(), "Display.create should contain branch targets");
        for (LabelNode target : jumpTargets) {
            AbstractInsnNode frame = target.getNext();
            while (frame instanceof LabelNode || frame instanceof LineNumberNode) {
                frame = frame.getNext();
            }
            assertTrue(frame instanceof FrameNode, "Branch target is missing a stack map frame");
        }
    }

    @Test
    void transformedDisplayPassesJvmVerification() throws Exception {
        byte[] original = readDisplayClass();
        byte[] transformed = new MacDisplayForwardCompatTransformer().transform(
            "org.lwjgl.opengl.Display",
            "org.lwjgl.opengl.Display",
            original
        );

        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("org.lwjgl.opengl.Display".equals(name)) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            loaded = defineClass(name, transformed, 0, transformed.length);
                        }
                        if (resolve) {
                            resolveClass(loaded);
                        }
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };

        Class<?> display = Class.forName("org.lwjgl.opengl.Display", false, loader);
        assertNotNull(display, "Transformed Display class did not load");
    }

    private static byte[] readDisplayClass() throws Exception {
        try (InputStream stream = MacDisplayForwardCompatTransformerTest.class.getClassLoader()
            .getResourceAsStream("org/lwjgl/opengl/Display.class")) {
            assertNotNull(stream, "Display.class is not on the test classpath");
            return stream.readAllBytes();
        }
    }

    private static final int GLFW_OPENGL_FORWARD_COMPAT = 139270;
}
