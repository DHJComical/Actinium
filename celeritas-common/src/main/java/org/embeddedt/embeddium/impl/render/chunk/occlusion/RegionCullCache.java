package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;

import java.util.Arrays;

import static org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion.REGION_BLOCK_HEIGHT;
import static org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion.REGION_BLOCK_LENGTH;
import static org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion.REGION_BLOCK_WIDTH;

/**
 * Per-region memoization of the two per-section visibility predicates used by {@link OcclusionCuller}.
 *
 * <p>Sections are grouped into 8x4x8-section regions (128x64x128 blocks) with dense ids. Once per search a region
 * is classified as outside, fully inside, or partially inside. The last result is inconclusive and requires the
 * caller to run the exact per-section tests.</p>
 */
final class RegionCullCache {
    /** Padding for the region box, including the maximum model extent and floating-point slack. */
    private static final float FRUSTUM_PAD = 1.125f + 0.03125f;

    /** Slack on distance bounds to account for camera-relative floating-point error. */
    private static final float DISTANCE_EPSILON = 0.001f;

    private static final int INITIAL_CAPACITY = 64;

    private byte[] classification = new byte[INITIAL_CAPACITY];
    private int[] stamp = new int[INITIAL_CAPACITY];

    private int currentStamp;

    private Viewport viewport;
    private CameraTransform camera;
    private float maxDistance;

    RegionCullCache() {
        Arrays.fill(this.stamp, -1);
    }

    /**
     * Starts a search and isolates all subsequent classifications from prior searches.
     *
     * @param viewport current camera and frustum
     * @param searchDistance maximum render distance in blocks
     * @param numRegions exclusive upper bound of dense region ids used by the search
     */
    void begin(Viewport viewport, float searchDistance, int numRegions) {
        this.viewport = viewport;
        this.camera = viewport.getTransform();
        this.maxDistance = searchDistance;
        this.currentStamp++;

        if (numRegions >= this.stamp.length) {
            this.grow(numRegions);
        }
    }

    /**
     * Returns the cached classification for a dense region id, computing it on first use in this search.
     */
    int classify(int regionId, int regionOriginX, int regionOriginY, int regionOriginZ) {
        if (this.stamp[regionId] != this.currentStamp) {
            this.stamp[regionId] = this.currentStamp;
            this.classification[regionId] = (byte) this.compute(regionOriginX, regionOriginY, regionOriginZ);
        }

        return this.classification[regionId];
    }

    private void grow(int minimumCapacity) {
        int capacity = Math.max(minimumCapacity, this.stamp.length * 2);
        int oldLength = this.stamp.length;

        this.stamp = Arrays.copyOf(this.stamp, capacity);
        this.classification = Arrays.copyOf(this.classification, capacity);
        Arrays.fill(this.stamp, oldLength, capacity, -1);
    }

    private int compute(int regionOriginX, int regionOriginY, int regionOriginZ) {
        final CameraTransform transform = this.camera;
        final float maxDistance = this.maxDistance;

        final int ax = regionOriginX - transform.intX;
        final int bx = ax + REGION_BLOCK_WIDTH;
        final int ay = regionOriginY - transform.intY;
        final int by = ay + REGION_BLOCK_HEIGHT;
        final int az = regionOriginZ - transform.intZ;
        final int bz = az + REGION_BLOCK_LENGTH;

        // Signed range of the nearest-point distance component for all member section boxes.
        final float loX = nearestToZero(ax, ax + 16) - transform.fracX;
        final float hiX = nearestToZero(bx - 16, bx) - transform.fracX;
        final float loY = nearestToZero(ay, ay + 16) - transform.fracY;
        final float hiY = nearestToZero(by - 16, by) - transform.fracY;
        final float loZ = nearestToZero(az, az + 16) - transform.fracZ;
        final float hiZ = nearestToZero(bz - 16, bz) - transform.fracZ;

        // Upper bound on every member's absolute distance component.
        final float farX = maxAbs(loX, hiX) + DISTANCE_EPSILON;
        final float farY = maxAbs(loY, hiY) + DISTANCE_EPSILON;
        final float farZ = maxAbs(loZ, hiZ) + DISTANCE_EPSILON;

        // Lower bound on every member's absolute distance component.
        final float nearX = Math.max(0.0f, minAbs(loX, hiX) - DISTANCE_EPSILON);
        final float nearY = Math.max(0.0f, minAbs(loY, hiY) - DISTANCE_EPSILON);
        final float nearZ = Math.max(0.0f, minAbs(loZ, hiZ) - DISTANCE_EPSILON);

        final float maxDistanceSq = maxDistance * maxDistance;
        boolean distanceOut = ((nearX * nearX) + (nearZ * nearZ)) >= maxDistanceSq || nearY >= maxDistance;

        if (distanceOut) {
            return Frustum.OUTSIDE;
        }

        boolean distanceIn = ((farX * farX) + (farZ * farZ)) < maxDistanceSq && farY < maxDistance;

        int result = this.viewport.intersectCameraRelativeBox(
                (ax - transform.fracX) - FRUSTUM_PAD,
                (ay - transform.fracY) - FRUSTUM_PAD,
                (az - transform.fracZ) - FRUSTUM_PAD,
                (bx - transform.fracX) + FRUSTUM_PAD,
                (by - transform.fracY) + FRUSTUM_PAD,
                (bz - transform.fracZ) + FRUSTUM_PAD);

        if (result == Frustum.FULLY_INSIDE) {
            return distanceIn ? Frustum.FULLY_INSIDE : Frustum.PARTIALLY_INSIDE;
        }

        return result;
    }

    private static int nearestToZero(int min, int max) {
        int clamped = 0;
        if (min > 0) {
            clamped = min;
        }
        if (max < 0) {
            clamped = max;
        }
        return clamped;
    }

    private static float maxAbs(float lo, float hi) {
        return Math.max(Math.abs(lo), Math.abs(hi));
    }

    private static float minAbs(float lo, float hi) {
        if (lo > 0.0f) {
            return lo;
        }
        if (hi < 0.0f) {
            return -hi;
        }
        return 0.0f;
    }
}
