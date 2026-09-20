package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.nio.DoubleBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClipPlaneStateTest {

    private static double[] eyePlane(ClipPlaneState state, int index) {
        DoubleBuffer buf = DoubleBuffer.allocate(4);
        state.getEyePlane(index, buf);
        return new double[]{buf.get(0), buf.get(1), buf.get(2), buf.get(3)};
    }

    @Test
    void identityModelViewKeepsObjectPlane() {
        ClipPlaneState state = new ClipPlaneState();
        state.setPlane(0, 1.0, 0.0, 0.0, -5.0, new Matrix4f());
        double[] p = eyePlane(state, 0);
        assertEquals(1.0, p[0], 1e-6);
        assertEquals(0.0, p[1], 1e-6);
        assertEquals(0.0, p[2], 1e-6);
        assertEquals(-5.0, p[3], 1e-6);
    }

    @Test
    void translatedModelViewShiftsPlaneEquation() {
        // Object plane x = 5 under a +2 x translation maps to eye plane x = 7:
        // eyePlane = transpose(inverse(MV)) * objectPlane
        ClipPlaneState state = new ClipPlaneState();
        state.setPlane(1, 1.0, 0.0, 0.0, -5.0, new Matrix4f().translate(2.0f, 0.0f, 0.0f));
        double[] p = eyePlane(state, 1);
        assertEquals(1.0, p[0], 1e-6);
        assertEquals(-7.0, p[3], 1e-6);
    }

    @Test
    void planesAreIndependentPerIndex() {
        ClipPlaneState state = new ClipPlaneState();
        state.setPlane(0, 1.0, 0.0, 0.0, -5.0, new Matrix4f());
        state.setPlane(2, 0.0, 1.0, 0.0, -3.0, new Matrix4f());
        double[] p0 = eyePlane(state, 0);
        double[] p2 = eyePlane(state, 2);
        assertEquals(-5.0, p0[3], 1e-6);
        assertEquals(1.0, p2[1], 1e-6);
        assertEquals(-3.0, p2[3], 1e-6);
        double[] p1 = eyePlane(state, 1);
        assertEquals(0.0, p1[0], 1e-6, "untouched plane stays zeroed");
        assertEquals(0.0, p1[3], 1e-6);
    }
}
