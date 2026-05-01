package com.dhj.actinium.shadows;

import org.joml.Matrix4f;

public final class ActiniumInternalShadowRenderingState {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    private static boolean matricesAvailable;
    private static boolean active;
    private static boolean renderShadowEntities = true;
    private static boolean renderShadowPlayer = true;
    private static boolean renderShadowBlockEntities = true;

    private ActiniumInternalShadowRenderingState() {
    }

    public static void update(Matrix4f modelView, Matrix4f projection) {
        MODEL_VIEW.set(modelView);
        PROJECTION.set(projection);
        matricesAvailable = true;
    }

    public static void begin(Matrix4f modelView, Matrix4f projection) {
        begin(modelView, projection, true, true, true);
    }

    public static void begin(Matrix4f modelView,
                             Matrix4f projection,
                             boolean renderShadowEntities,
                             boolean renderShadowPlayer,
                             boolean renderShadowBlockEntities) {
        update(modelView, projection);
        active = true;
        ActiniumInternalShadowRenderingState.renderShadowEntities = renderShadowEntities;
        ActiniumInternalShadowRenderingState.renderShadowPlayer = renderShadowPlayer;
        ActiniumInternalShadowRenderingState.renderShadowBlockEntities = renderShadowBlockEntities;
    }

    public static void end() {
        active = false;
        renderShadowEntities = true;
        renderShadowPlayer = true;
        renderShadowBlockEntities = true;
    }

    public static boolean areShadowsCurrentlyBeingRendered() {
        return active;
    }

    public static boolean fillShadowMatrices(Matrix4f modelView, Matrix4f projection) {
        if (!matricesAvailable) {
            return false;
        }

        modelView.set(MODEL_VIEW);
        projection.set(PROJECTION);
        return true;
    }

    public static void clear() {
        matricesAvailable = false;
        active = false;
        renderShadowEntities = true;
        renderShadowPlayer = true;
        renderShadowBlockEntities = true;
        MODEL_VIEW.identity();
        PROJECTION.identity();
    }

    public static boolean shouldRenderShadowEntities() {
        return renderShadowEntities;
    }

    public static boolean shouldRenderShadowPlayer() {
        return renderShadowPlayer;
    }

    public static boolean shouldRenderShadowBlockEntities() {
        return renderShadowBlockEntities;
    }
}
