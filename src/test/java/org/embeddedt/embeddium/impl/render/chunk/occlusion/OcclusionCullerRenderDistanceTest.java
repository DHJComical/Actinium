package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chunk search must be bounded by horizontal distance only: vanilla 1.12
 * has no vertical cutoff, and mods such as Depths Update can extend the world
 * height, so a fixed vertical truncation would wrongly drop in-bounds terrain
 * directly below or above the camera.
 */
class OcclusionCullerRenderDistanceTest {
    // 12-chunk render distance, in blocks.
    private static final float MAX_DISTANCE = 12 * 16.0f;

    @Test
    void sectionFarBelowCameraIsWithinRenderDistance() {
        // Camera at Y=1024 with a section directly underneath at the bottom of the world.
        CameraTransform camera = new CameraTransform(8.0, 1024.0, 8.0);

        assertTrue(OcclusionCuller.isWithinRenderDistance(camera, 0, 0, 0, MAX_DISTANCE));
    }

    @Test
    void sectionFarAboveCameraIsWithinRenderDistance() {
        CameraTransform camera = new CameraTransform(8.0, 64.0, 8.0);

        assertTrue(OcclusionCuller.isWithinRenderDistance(camera, 0, 64, 0, MAX_DISTANCE));
    }

    @Test
    void sectionBelowZeroInExtendedHeightWorldIsWithinRenderDistance() {
        // Depths Update style extended height: sections exist below Y=0.
        CameraTransform camera = new CameraTransform(8.0, 64.0, 8.0);

        assertTrue(OcclusionCuller.isWithinRenderDistance(camera, 0, -32, 0, MAX_DISTANCE));
    }

    @Test
    void sectionJustPastFormerVerticalCutoffIsWithinRenderDistance() {
        // The section's nearest point sits exactly one maxDistance above the
        // camera; the horizontal test alone must keep it in range.
        CameraTransform camera = new CameraTransform(8.0, 64.0, 8.0);

        assertTrue(OcclusionCuller.isWithinRenderDistance(camera, 0, 16, 0, MAX_DISTANCE));
    }

    @Test
    void horizontallyOutOfRangeSectionIsRejected() {
        CameraTransform camera = new CameraTransform(8.0, 64.0, 8.0);

        assertFalse(OcclusionCuller.isWithinRenderDistance(camera, 20, 4, 0, MAX_DISTANCE));
    }

    @Test
    void regionFarBelowCameraPassesDistanceTest() {
        // Region directly underneath a high camera; only the horizontal
        // distance may reject it.
        RegionCullCache cache = beginCache(8.0, 1024.0, 8.0);

        assertEquals(RegionCullCache.PARTIAL_DISTANCE_IN, cache.classify(0, 0, 0, 0));
    }

    @Test
    void regionBelowZeroInExtendedHeightWorldPassesDistanceTest() {
        RegionCullCache cache = beginCache(8.0, 64.0, 8.0);

        assertEquals(RegionCullCache.PARTIAL_DISTANCE_IN, cache.classify(0, 0, -512, 0));
    }

    @Test
    void horizontallyDistantRegionIsOutside() {
        RegionCullCache cache = beginCache(8.0, 64.0, 8.0);

        assertEquals(RegionCullCache.OUTSIDE, cache.classify(0, 1024, 0, 0));
    }

    // A frustum that reports everything visible keeps the region
    // classification focused on the distance test alone.
    private static RegionCullCache beginCache(double x, double y, double z) {
        Viewport viewport = new Viewport((minX, minY, minZ, maxX, maxY, maxZ) -> true, new Vector3d(x, y, z));
        RegionCullCache cache = new RegionCullCache();
        cache.begin(viewport, MAX_DISTANCE, 1);
        return cache;
    }
}
