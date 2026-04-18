package com.dhj.actinium.shadows;

import org.joml.Matrix4f;

public final class ActiniumInternalShadowRenderingState {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    private static boolean matricesAvailable;
    private static boolean active;

    private ActiniumInternalShadowRenderingState() {
    }

    public static void update(Matrix4f modelView, Matrix4f projection) {
        MODEL_VIEW.set(modelView);
        PROJECTION.set(projection);
        matricesAvailable = true;
    }

    public static void begin(Matrix4f modelView, Matrix4f projection) {
        update(modelView, projection);
        active = true;
    }

    public static void end() {
        active = false;
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
        MODEL_VIEW.identity();
        PROJECTION.identity();
    }
}
