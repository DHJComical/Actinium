package com.dhj.actinium.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Projects render-relative portal positions with the active model-view and projection matrices.
 */
final class EndPortalProjection {
    private static final double MINIMUM_DIVISOR = 1.0E-7D;

    private final Matrix4f transform;

    /**
     * Captures the matrix product used by the draw that will consume the generated vertices.
     *
     * @param projection active projection matrix
     * @param modelView active model-view matrix
     */
    EndPortalProjection(Matrix4fc projection, Matrix4fc modelView) {
        this.transform = new Matrix4f(projection).mul(modelView);
        if (!isFinite(this.transform)) {
            throw new IllegalArgumentException("Portal projection matrix must be finite");
        }
    }

    /**
     * Transforms one render-relative position into homogeneous clip coordinates.
     *
     * @param x render-relative X coordinate
     * @param y render-relative Y coordinate
     * @param z render-relative Z coordinate
     * @return homogeneous clip position
     */
    ClipPosition clip(double x, double y, double z) {
        return new ClipPosition(
            transform.m00() * x + transform.m10() * y + transform.m20() * z + transform.m30(),
            transform.m01() * x + transform.m11() * y + transform.m21() * z + transform.m31(),
            transform.m02() * x + transform.m12() * y + transform.m22() * z + transform.m32(),
            transform.m03() * x + transform.m13() * y + transform.m23() * z + transform.m33()
        );
    }

    private static boolean isFinite(Matrix4fc matrix) {
        return Float.isFinite(matrix.m00()) && Float.isFinite(matrix.m01())
            && Float.isFinite(matrix.m02()) && Float.isFinite(matrix.m03())
            && Float.isFinite(matrix.m10()) && Float.isFinite(matrix.m11())
            && Float.isFinite(matrix.m12()) && Float.isFinite(matrix.m13())
            && Float.isFinite(matrix.m20()) && Float.isFinite(matrix.m21())
            && Float.isFinite(matrix.m22()) && Float.isFinite(matrix.m23())
            && Float.isFinite(matrix.m30()) && Float.isFinite(matrix.m31())
            && Float.isFinite(matrix.m32()) && Float.isFinite(matrix.m33());
    }

    /**
     * Homogeneous clip-space position before perspective division.
     *
     * @param x clip X
     * @param y clip Y
     * @param z clip Z
     * @param w clip W
     */
    record ClipPosition(double x, double y, double z, double w) {
        ClipPosition {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Double.isFinite(w)) {
                throw new IllegalArgumentException("Clip position must be finite");
            }
        }
    }

    /**
     * Homogeneous texture coordinate consumed by fixed-function projective texture sampling.
     *
     * @param s homogeneous S coordinate
     * @param t homogeneous T coordinate
     * @param r homogeneous R coordinate
     * @param q homogeneous Q coordinate
     */
    record ProjectiveTexCoord(double s, double t, double r, double q) {
        ProjectiveTexCoord {
            if (!Double.isFinite(s) || !Double.isFinite(t) || !Double.isFinite(r) || !Double.isFinite(q)) {
                throw new IllegalArgumentException("Projective texture coordinate must be finite");
            }
        }

        /**
         * Divides S by Q for shader-pack UV input.
         *
         * @return divided U coordinate
         */
        double dividedU() {
            requireDivisor();
            return s / q;
        }

        /**
         * Divides T by Q for shader-pack UV input.
         *
         * @return divided V coordinate
         */
        double dividedV() {
            requireDivisor();
            return t / q;
        }

        private void requireDivisor() {
            if (Math.abs(q) < MINIMUM_DIVISOR) {
                throw new IllegalStateException("Projective texture Q is too close to zero: " + q);
            }
        }
    }
}
