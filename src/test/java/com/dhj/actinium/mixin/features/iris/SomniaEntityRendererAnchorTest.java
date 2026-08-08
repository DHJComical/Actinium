package com.dhj.actinium.mixin.features.iris;

import net.minecraft.client.renderer.EntityRenderer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SomniaEntityRendererAnchorTest {

    @Test
    void afterRenderWorldAnchorSurvivesSomniaRenderWorldRewrite() throws Exception {
        ClassNode entityRenderer = readEntityRenderer();
        MethodNode updateCameraAndRender = entityRenderer.methods.stream()
            .filter(method -> "updateCameraAndRender".equals(method.name) && "(FJ)V".equals(method.desc))
            .findFirst()
            .orElseThrow(() -> new AssertionError("EntityRenderer.updateCameraAndRender(FJ)V not found"));

        List<AbstractInsnNode> instructions = new ArrayList<>();
        boolean hasVanillaRenderWorldCall = false;
        int lastSomniaRenderWorldCallIndex = -1;
        for (AbstractInsnNode instruction = updateCameraAndRender.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            instructions.add(instruction);
            if (instruction instanceof MethodInsnNode methodCall
                && "net/minecraft/client/renderer/EntityRenderer".equals(methodCall.owner)
                && "renderWorld".equals(methodCall.name)
                && "(FJ)V".equals(methodCall.desc)
                && methodCall.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                hasVanillaRenderWorldCall = true;
                lastSomniaRenderWorldCallIndex = instructions.size() - 1;
                methodCall.owner = "com/kingrunes/somnia/common/util/SomniaUtil";
                methodCall.setOpcode(Opcodes.INVOKESTATIC);
            }
        }

        assertTrue(hasVanillaRenderWorldCall, "Somnia rewrite scenario must start from a vanilla renderWorld call");
        assertFalse(
            instructions.stream()
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .anyMatch(call -> "net/minecraft/client/renderer/EntityRenderer".equals(call.owner)
                    && "renderWorld".equals(call.name)
                    && "(FJ)V".equals(call.desc)),
            "The old renderWorld-call anchor would not exist after Somnia rewrites it"
        );

        List<FieldInsnNode> renderEndNanoTimeWrites = instructions.stream()
            .filter(FieldInsnNode.class::isInstance)
            .map(FieldInsnNode.class::cast)
            .filter(field -> field.getOpcode() == Opcodes.PUTFIELD
                && "net/minecraft/client/renderer/EntityRenderer".equals(field.owner)
                && "renderEndNanoTime".equals(field.name)
                && "J".equals(field.desc))
            .toList();
        assertFalse(renderEndNanoTimeWrites.isEmpty(),
            "The new renderEndNanoTime anchor must still exist in updateCameraAndRender");
        assertTrue(instructions.indexOf(renderEndNanoTimeWrites.getFirst()) > lastSomniaRenderWorldCallIndex,
            "The renderEndNanoTime anchor must follow the world render call");
    }

    private static ClassNode readEntityRenderer() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream stream = EntityRenderer.class.getResourceAsStream("EntityRenderer.class")) {
            if (stream == null) {
                throw new AssertionError("EntityRenderer.class resource is not on the test classpath");
            }
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }
}
