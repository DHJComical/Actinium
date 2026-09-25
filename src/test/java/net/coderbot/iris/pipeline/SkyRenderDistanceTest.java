package net.coderbot.iris.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyRenderDistanceTest {
    @Test
    void keepsAtLeastEightChunksForSkyRendering() {
        assertEquals(8, SkyRenderDistance.effectiveChunks(1));
        assertEquals(8, SkyRenderDistance.effectiveChunks(2));
        assertEquals(8, SkyRenderDistance.effectiveChunks(3));
    }

    @Test
    void preservesLargerRenderDistances() {
        assertEquals(8, SkyRenderDistance.effectiveChunks(8));
        assertEquals(32, SkyRenderDistance.effectiveChunks(32));
    }

    @Test
    void convertsEffectiveChunksToBlocks() {
        assertEquals(128, SkyRenderDistance.effectiveBlocks(1));
        assertEquals(128, SkyRenderDistance.effectiveBlocks(3));
        assertEquals(128, SkyRenderDistance.effectiveBlocks(8));
    }

    @Test
    void farBlocksKeepsTheConfiguredBoundaryWithoutNeighborGating() {
        assertEquals(192, SkyRenderDistance.farBlocks(12, 0));
        assertEquals(128, SkyRenderDistance.farBlocks(8, 0));
    }

    @Test
    void farBlocksDropsTheOuterRingsThatNeighborGatingLeavesUnrendered() {
        assertEquals(176, SkyRenderDistance.farBlocks(12, 1));
        assertEquals(160, SkyRenderDistance.farBlocks(12, 2));
    }

    @Test
    void farBlocksDoesNotApplyTheSkyRenderingFloor() {
        // The floor keeps the sky, cloud, and celestial geometry of small render distances intact. Reporting it through
        // far would place the Distant Horizons transition beyond the rendered terrain, leaving a gap, so the far
        // boundary must stay below it whenever the configured distance does.
        assertEquals(16, SkyRenderDistance.farBlocks(2, 1));
        assertEquals(112, SkyRenderDistance.farBlocks(8, 1));
        assertEquals(128, SkyRenderDistance.farBlocks(9, 1));
        assertEquals(128, SkyRenderDistance.effectiveBlocks(2));
    }
}
