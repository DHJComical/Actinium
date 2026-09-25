package com.dhj.actinium.render.frustum;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

/**
 * Exposes the JOML view matrices rebuilt each frame by the clipping helper mixin,
 * so frustum consumers outside the mixin package can reach them.
 */
public interface IClippingHelper {
    /** Combined projection*modelview frustum for box tests. */
    FrustumIntersection celeritas$getJomlFrustum();

    /** Rotation+projection only (no camera translation); feeds the raster occluder. */
    Matrix4f celeritas$getVpMatrix();
}
