package com.dhj.actinium.render;

/**
 * Extends a vertex buffer with the four-component texture coordinate required for projective sampling.
 */
public interface ProjectiveTexCoordBuffer {
    /**
     * Writes one primary UV element as homogeneous S, T, R, and Q components.
     *
     * @param s homogeneous S coordinate
     * @param t homogeneous T coordinate
     * @param r homogeneous R coordinate
     * @param q homogeneous Q coordinate
     */
    void actinium$projectiveTexCoord(float s, float t, float r, float q);
}
