package com.dhj.actinium.render;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndPortalLayersTest {
    @Test
    void createsRequestedLayerCountAndVanillaBlendSequence() {
        List<EndPortalLayers.Layer> layers = EndPortalLayers.create(11, 0.25F);

        assertEquals(11, layers.size());
        assertEquals(EndPortalLayers.Texture.END_SKY, layers.getFirst().texture());
        assertEquals(EndPortalLayers.Blend.ALPHA, layers.getFirst().blend());
        assertEquals(EndPortalLayers.Texture.END_PORTAL, layers.get(1).texture());
        assertEquals(EndPortalLayers.Blend.ADDITIVE, layers.get(1).blend());
    }

    @Test
    void vanillaLayersScrollWithTime() {
        EndPortalLayers.Layer earlier = EndPortalLayers.create(3, 0.1F).get(2);
        EndPortalLayers.Layer later = EndPortalLayers.create(3, 0.2F).get(2);

        assertNotEquals(
            new Uv(earlier.u(0.25F, 0.75F), earlier.v(0.25F, 0.75F)),
            new Uv(later.u(0.25F, 0.75F), later.v(0.25F, 0.75F))
        );
    }

    @Test
    void everyVanillaLayerUsesADifferentUvTransform() {
        List<EndPortalLayers.Layer> layers = EndPortalLayers.create(4, 0.25F);

        for (int index = 1; index < layers.size(); index++) {
            EndPortalLayers.Layer previous = layers.get(index - 1);
            EndPortalLayers.Layer current = layers.get(index);
            assertNotEquals(previous.u(0.2F, 0.7F), current.u(0.2F, 0.7F));
            assertNotEquals(previous.v(0.2F, 0.7F), current.v(0.2F, 0.7F));
        }
    }

    @Test
    void supportsEveryVanillaDistanceTierAndGatewayExtraLayer() {
        for (int layerCount : List.of(1, 3, 5, 7, 9, 11, 13, 14, 15, 16)) {
            assertEquals(layerCount, EndPortalLayers.create(layerCount, 0.25F).size());
        }
    }

    @Test
    void reproducesVanillaSeededLayerColors() {
        List<EndPortalLayers.Layer> layers = EndPortalLayers.create(2, 0.25F);
        Random expected = new Random(31100L);

        assertLayerColor(layers.getFirst(), expected, 0.15F);
        assertLayerColor(layers.get(1), expected, 2.0F / 17.0F);
    }

    @Test
    void appliesVanillaHalfScaleToLayerTranslations() {
        EndPortalLayers.Layer first = EndPortalLayers.create(1, 0.25F).getFirst();

        assertEquals(0.5F + 8.5F, first.offsetU());
        assertEquals(0.5F + (2.0F + 1.0F / 1.5F) * 0.25F * 0.5F, first.offsetV());
    }

    @Test
    void vanillaUvTransformsStayFiniteAcrossAllLayers() {
        List<EndPortalLayers.Layer> layers = EndPortalLayers.create(16, 0.999F);

        for (EndPortalLayers.Layer layer : layers) {
            assertTrue(Double.isFinite(layer.u(-256.5D, 384.25D)));
            assertTrue(Double.isFinite(layer.v(-256.5D, 384.25D)));
        }
    }

    private static void assertLayerColor(EndPortalLayers.Layer layer, Random expected, float scale) {
        assertEquals((expected.nextFloat() * 0.5F + 0.1F) * scale, layer.red());
        assertEquals((expected.nextFloat() * 0.5F + 0.4F) * scale, layer.green());
        assertEquals((expected.nextFloat() * 0.5F + 0.5F) * scale, layer.blue());
    }

    private record Uv(double u, double v) {
    }
}
