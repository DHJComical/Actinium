package org.embeddedt.embeddium.impl.render.viewport.frustum;

/** Provides frustum tests and classifications for camera-relative boxes. */
public interface Frustum {
    /** Classification returned when a box is fully inside the frustum. */
    int FULLY_INSIDE = 0;

    /** Classification returned when a box intersects or is inside the frustum. */
    int PARTIALLY_INSIDE = 1;

    /** Classification returned when a box is outside the frustum. */
    int OUTSIDE = 2;

    boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    /**
     * Classifies an axis-aligned box. Implementations that only expose a
     * boolean test conservatively report visible boxes as partial.
     */
    default int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return testAab(minX, minY, minZ, maxX, maxY, maxZ) ? PARTIALLY_INSIDE : OUTSIDE;
    }
}
