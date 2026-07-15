package com.dhj.actinium.render;

import java.util.List;

/**
 * Defines the shader-pack precomposition tiers and their context-free color math.
 */
final class EndPortalCompositeLogic {
    static final int MINIMUM_PRECOMPOSED_LAYER_COUNT = 14;
    static final int MAXIMUM_PRECOMPOSED_LAYER_COUNT = 16;

    private EndPortalCompositeLogic() {
    }

    /** Returns whether the expensive near-distance shader path should be precomposed. */
    static boolean shouldPrecompose(boolean shaderPackInUse, int layerCount) {
        return shaderPackInUse
            && layerCount >= MINIMUM_PRECOMPOSED_LAYER_COUNT
            && layerCount <= MAXIMUM_PRECOMPOSED_LAYER_COUNT;
    }

    /** Returns whether a cached tier has not yet been rendered in the current frame. */
    static boolean needsUpdate(long lastUpdatedFrame, long currentFrame) {
        if (currentFrame < 0L) {
            throw new IllegalArgumentException("currentFrame must not be negative: " + currentFrame);
        }
        return lastUpdatedFrame != currentFrame;
    }

    /**
     * Evaluates the same opaque-sky followed by additive-layer equation used by the GPU compositor.
     */
    static Color composite(
        List<EndPortalLayers.Layer> layers,
        TextureSampler sampler,
        double projectedX,
        double projectedY
    ) {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("layers must not be empty");
        }

        float red = 0.0F;
        float green = 0.0F;
        float blue = 0.0F;
        for (EndPortalLayers.Layer layer : layers) {
            Color sample = sampler.sample(
                layer.texture(),
                layer.u(projectedX, projectedY),
                layer.v(projectedX, projectedY)
            );
            red += sample.red * layer.red();
            green += sample.green * layer.green();
            blue += sample.blue * layer.blue();
        }
        return new Color(red, green, blue, 1.0F);
    }

    /** Samples one source texture at transformed portal coordinates. */
    @FunctionalInterface
    interface TextureSampler {
        Color sample(EndPortalLayers.Texture texture, double u, double v);
    }

    /** Linear RGBA color used by the context-free compositor model. */
    record Color(float red, float green, float blue, float alpha) {
    }
}
