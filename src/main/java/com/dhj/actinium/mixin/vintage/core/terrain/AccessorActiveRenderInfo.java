package com.dhj.actinium.mixin.vintage.core.terrain;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(ActiveRenderInfo.class)
public interface AccessorActiveRenderInfo {
    @Accessor("PROJECTION")
    static FloatBuffer getProjectionMatrix() {
        throw new AssertionError();
    }

    @Accessor("MODELVIEW")
    static FloatBuffer getModelViewMatrix() {
        throw new AssertionError();
    }

    @Accessor("PROJECTION")
    static void setProjectionMatrix(FloatBuffer matrix) {
        throw new AssertionError();
    }

    @Accessor("MODELVIEW")
    static void setModelViewMatrix(FloatBuffer matrix) {
        throw new AssertionError();
    }

    @Accessor("OBJECTCOORDS")
    static FloatBuffer getObjectCoords() {
        throw new AssertionError();
    }

    @Accessor("OBJECTCOORDS")
    static void setObjectCoords(FloatBuffer buffer) {
        throw new AssertionError();
    }

    @Accessor("VIEWPORT")
    static IntBuffer getViewportBuffer() {
        throw new AssertionError();
    }

    @Accessor("VIEWPORT")
    static void setViewportBuffer(IntBuffer buffer) {
        throw new AssertionError();
    }

    @Accessor("position")
    static Vec3d getPosition() {
        throw new AssertionError();
    }

    @Accessor("position")
    static void setPosition(Vec3d position) {
        throw new AssertionError();
    }

    @Accessor("rotationX")
    static float getRotationX() {
        throw new AssertionError();
    }

    @Accessor("rotationX")
    static void setRotationX(float rotationX) {
        throw new AssertionError();
    }

    @Accessor("rotationXZ")
    static float getRotationXZ() {
        throw new AssertionError();
    }

    @Accessor("rotationXZ")
    static void setRotationXZ(float rotationXZ) {
        throw new AssertionError();
    }

    @Accessor("rotationZ")
    static float getRotationZ() {
        throw new AssertionError();
    }

    @Accessor("rotationZ")
    static void setRotationZ(float rotationZ) {
        throw new AssertionError();
    }

    @Accessor("rotationYZ")
    static float getRotationYZ() {
        throw new AssertionError();
    }

    @Accessor("rotationYZ")
    static void setRotationYZ(float rotationYZ) {
        throw new AssertionError();
    }

    @Accessor("rotationXY")
    static float getRotationXY() {
        throw new AssertionError();
    }

    @Accessor("rotationXY")
    static void setRotationXY(float rotationXY) {
        throw new AssertionError();
    }
}
