package net.coderbot.iris.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyRenderDistanceTest {
    @Test
    void keepsAtLeastFourChunksForSkyRendering() {
        assertEquals(4, SkyRenderDistance.effectiveChunks(1));
        assertEquals(4, SkyRenderDistance.effectiveChunks(2));
        assertEquals(4, SkyRenderDistance.effectiveChunks(3));
    }

    @Test
    void preservesLargerRenderDistances() {
        assertEquals(4, SkyRenderDistance.effectiveChunks(4));
        assertEquals(8, SkyRenderDistance.effectiveChunks(8));
        assertEquals(32, SkyRenderDistance.effectiveChunks(32));
    }

    @Test
    void convertsEffectiveChunksToBlocks() {
        assertEquals(64, SkyRenderDistance.effectiveBlocks(1));
        assertEquals(64, SkyRenderDistance.effectiveBlocks(3));
        assertEquals(128, SkyRenderDistance.effectiveBlocks(8));
    }
}
