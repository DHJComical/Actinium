package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;

/**
 * Stores eye-space clip plane equations for core profile emulation.
 * Ported from Angelica.
 */
public class ClipPlaneState {
    private final float[][] eyePlanes = new float[8][4];

    private static final Vector4f tempPlane = new Vector4f();
    private static final Matrix4f tempInverse = new Matrix4f();

    public void setPlane(int index, double a, double b, double c, double d, Matrix4f modelView) {
        final float[] dest = eyePlanes[index];
        modelView.invert(tempInverse);
        tempPlane.set((float) a, (float) b, (float) c, (float) d);
        tempInverse.transformTranspose(tempPlane);
        dest[0] = tempPlane.x;
        dest[1] = tempPlane.y;
        dest[2] = tempPlane.z;
        dest[3] = tempPlane.w;
    }

    public void putEyePlane(int index, FloatBuffer buf) {
        final float[] p = eyePlanes[index];
        buf.put(p[0]).put(p[1]).put(p[2]).put(p[3]);
    }

    public float[] getEyePlane(int index) {
        return eyePlanes[index];
    }
}
