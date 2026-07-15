package com.dhj.actinium.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the material and affine UV parameters for the Core Profile replacement of vanilla portal layers.
 */
final class EndPortalLayers {
    private static final long COLOR_SEED = 31100L;
    private static final int MAX_VANILLA_LAYERS = 16;

    private EndPortalLayers() {
    }

    /**
     * Creates all layers needed for one portal draw.
     *
     * @param vanillaLayerCount distance-dependent layer count returned by the vanilla renderer
     * @param vanillaAnimationTime vanilla animation time in the range {@code [0, 1)}
     * @return immutable ordered portal layers
     */
    static List<Layer> create(int vanillaLayerCount, float vanillaAnimationTime) {
        if (vanillaLayerCount < 1 || vanillaLayerCount > MAX_VANILLA_LAYERS) {
            throw new IllegalArgumentException("vanillaLayerCount must be in [1, 16]: " + vanillaLayerCount);
        }
        requireUnitInterval(vanillaAnimationTime, "vanillaAnimationTime");

        Random random = new Random(COLOR_SEED);
        List<Layer> layers = new ArrayList<>(vanillaLayerCount);
        for (int pass = 0; pass < vanillaLayerCount; pass++) {
            layers.add(vanillaLayer(pass, vanillaAnimationTime, random));
        }
        return List.copyOf(layers);
    }

    private static Layer vanillaLayer(int pass, float animationTime, Random random) {
        float layer = pass + 1.0F;
        float colorScale = pass == 0 ? 0.15F : 2.0F / (18.0F - pass);
        float red = (random.nextFloat() * 0.5F + 0.1F) * colorScale;
        float green = (random.nextFloat() * 0.5F + 0.4F) * colorScale;
        float blue = (random.nextFloat() * 0.5F + 0.5F) * colorScale;

        float rotationDegrees = (layer * layer * 4321.0F + layer * 9.0F) * 2.0F;
        float rotation = (float) Math.toRadians(rotationDegrees);
        float scale = (4.5F - layer / 4.0F) * 0.5F;
        float cosine = (float) Math.cos(rotation) * scale;
        float sine = (float) Math.sin(rotation) * scale;
        float scroll = (2.0F + layer / 1.5F) * animationTime;

        return new Layer(
            pass == 0 ? Texture.END_SKY : Texture.END_PORTAL,
            pass == 0 ? Blend.ALPHA : Blend.ADDITIVE,
            red,
            green,
            blue,
            0.5F + 8.5F / layer,
            0.5F + scroll * 0.5F,
            cosine,
            -sine,
            sine,
            cosine
        );
    }

    private static void requireUnitInterval(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F || value >= 1.0F) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1): " + value);
        }
    }

    /**
     * Selects the texture bound for a layer.
     */
    enum Texture {
        END_SKY,
        END_PORTAL
    }

    /**
     * Describes the blend state required while drawing a layer.
     */
    enum Blend {
        ALPHA,
        ADDITIVE
    }

    /**
     * Contains one layer's texture, color, blend mode, and two-dimensional affine UV transform.
     *
     * @param texture texture used by the layer
     * @param blend required blending behavior
     * @param red red vertex color component
     * @param green green vertex color component
     * @param blue blue vertex color component
     * @param offsetU affine U translation
     * @param offsetV affine V translation
     * @param transformUU affine U-from-U coefficient
     * @param transformUV affine U-from-V coefficient
     * @param transformVU affine V-from-U coefficient
     * @param transformVV affine V-from-V coefficient
     */
    record Layer(
        Texture texture,
        Blend blend,
        float red,
        float green,
        float blue,
        float offsetU,
        float offsetV,
        float transformUU,
        float transformUV,
        float transformVU,
        float transformVV
    ) {
        /**
         * Resolves the transformed U coordinate.
         *
         * @param sourceU untransformed U coordinate
         * @param sourceV untransformed V coordinate
         * @return transformed U coordinate
         */
        double u(double sourceU, double sourceV) {
            return offsetU + sourceU * transformUU + sourceV * transformUV;
        }

        /**
         * Resolves the transformed V coordinate.
         *
         * @param sourceU untransformed U coordinate
         * @param sourceV untransformed V coordinate
         * @return transformed V coordinate
         */
        double v(double sourceU, double sourceV) {
            return offsetV + sourceU * transformVU + sourceV * transformVV;
        }

        /**
         * Applies this layer after projection without discarding the homogeneous Q coordinate.
         *
         * @param clip active projection result for the vertex
         * @return complete projective texture coordinate
         */
        EndPortalProjection.ProjectiveTexCoord projective(EndPortalProjection.ClipPosition clip) {
            return new EndPortalProjection.ProjectiveTexCoord(
                offsetU * clip.w() + transformUU * clip.x() + transformUV * clip.y(),
                offsetV * clip.w() + transformVU * clip.x() + transformVV * clip.y(),
                clip.z(),
                clip.w()
            );
        }
    }
}
