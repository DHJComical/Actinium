package com.dhj.actinium.shader.uniform;

import org.joml.Vector4f;

public final class ActiniumCapturedRenderingState {
    private static final Vector4f CURRENT_ENTITY_COLOR = new Vector4f();

    private static int currentRenderedEntity = -1;
    private static int currentRenderedBlockEntity = -1;

    private ActiniumCapturedRenderingState() {
    }

    public static int getCurrentRenderedEntity() {
        return currentRenderedEntity;
    }

    public static int getCurrentRenderedBlockEntity() {
        return currentRenderedBlockEntity;
    }

    public static Vector4f getCurrentEntityColor() {
        return CURRENT_ENTITY_COLOR;
    }

    public static void setCurrentEntity(int entityId) {
        currentRenderedEntity = entityId;
    }

    public static void setCurrentBlockEntity(int blockEntityId) {
        currentRenderedBlockEntity = blockEntityId;
    }

    public static void setCurrentEntityColor(float red, float green, float blue, float alpha) {
        CURRENT_ENTITY_COLOR.set(red, green, blue, alpha);
    }

    public static void resetEntityState() {
        currentRenderedEntity = -1;
        currentRenderedBlockEntity = -1;
        CURRENT_ENTITY_COLOR.set(0.0f, 0.0f, 0.0f, 0.0f);
    }
}
