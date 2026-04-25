package com.dhj.actinium.shader.uniform;

import org.joml.Vector4f;

public final class ActiniumCapturedRenderingState {
    private static final Vector4f CURRENT_ENTITY_COLOR = new Vector4f();

    private static int currentRenderedEntity = -1;

    private ActiniumCapturedRenderingState() {
    }

    public static int getCurrentRenderedEntity() {
        return currentRenderedEntity;
    }

    public static Vector4f getCurrentEntityColor() {
        return CURRENT_ENTITY_COLOR;
    }

    public static void setCurrentEntity(int entityId) {
        currentRenderedEntity = entityId;
    }

    public static void setCurrentEntityColor(float red, float green, float blue, float alpha) {
        CURRENT_ENTITY_COLOR.set(red, green, blue, alpha);
    }

    public static void resetEntityState() {
        currentRenderedEntity = -1;
        CURRENT_ENTITY_COLOR.set(0.0f, 0.0f, 0.0f, 0.0f);
    }
}
