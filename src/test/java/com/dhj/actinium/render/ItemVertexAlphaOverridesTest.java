package com.dhj.actinium.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the query the fast lit item path relies on: it must report active exactly while a
 * registered source scales item vertex alpha, since a single missed frame is enough to bake a
 * transparent display list into the item cache (issue #145).
 */
class ItemVertexAlphaOverridesTest {

    @AfterEach
    void clearRegisteredOverrides() {
        ItemVertexAlphaOverrides.clear();
    }

    @Test
    void reportsInactiveWithoutRegisteredSources() {
        assertFalse(ItemVertexAlphaOverrides.isActive());
    }

    @Test
    void ignoresSourcesThatDoNotScaleAlpha() {
        ItemVertexAlphaOverrides.register(() -> 1.0F);

        assertFalse(ItemVertexAlphaOverrides.isActive());
    }

    @Test
    void reportsActiveWhileASourceScalesAlpha() {
        ItemVertexAlphaOverrides.register(() -> 0.25F);

        assertTrue(ItemVertexAlphaOverrides.isActive());
    }

    @Test
    void tracksASourceThatStartsAndStopsScaling() {
        float[] multiplier = { 1.0F };
        ItemVertexAlphaOverrides.register(() -> multiplier[0]);

        assertFalse(ItemVertexAlphaOverrides.isActive());

        multiplier[0] = 0.0F;
        assertTrue(ItemVertexAlphaOverrides.isActive());

        multiplier[0] = 1.0F;
        assertFalse(ItemVertexAlphaOverrides.isActive());
    }

    @Test
    void reportsActiveWhenAnyOfSeveralSourcesScales() {
        ItemVertexAlphaOverrides.register(() -> 1.0F);
        ItemVertexAlphaOverrides.register(() -> 0.5F);

        assertTrue(ItemVertexAlphaOverrides.isActive());
    }

    @Test
    void stopsReportingAfterTheSourcesAreCleared() {
        ItemVertexAlphaOverrides.register(() -> 0.5F);
        assertTrue(ItemVertexAlphaOverrides.isActive());

        ItemVertexAlphaOverrides.clear();

        assertFalse(ItemVertexAlphaOverrides.isActive());
    }
}
