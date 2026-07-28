package com.dhj.actinium.compat.ichunutil;

/**
 * Tests world-space boxes against a camera-specific frustum without exposing the camera implementation.
 */
@FunctionalInterface
public interface WorldBoxVisibility {
    /**
     * Tests whether the supplied world-space axis-aligned box is visible.
     */
    boolean isBoxVisible(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    );
}
