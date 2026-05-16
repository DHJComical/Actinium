package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * Per-texture-unit storage for texgen mode and plane coefficients.
 * Ported from Angelica.
 */
public class TexGenState {
    private int modeS = GL11.GL_EYE_LINEAR;
    private int modeT = GL11.GL_EYE_LINEAR;
    private int modeR = GL11.GL_EYE_LINEAR;
    private int modeQ = GL11.GL_EYE_LINEAR;

    private final float[] objectPlaneS = {1, 0, 0, 0};
    private final float[] objectPlaneT = {0, 1, 0, 0};
    private final float[] objectPlaneR = {0, 0, 0, 0};
    private final float[] objectPlaneQ = {0, 0, 0, 0};

    private final float[] eyePlaneS = {1, 0, 0, 0};
    private final float[] eyePlaneT = {0, 1, 0, 0};
    private final float[] eyePlaneR = {0, 0, 0, 0};
    private final float[] eyePlaneQ = {0, 0, 0, 0};

    private static final Vector4f tempPlane = new Vector4f();
    private static final Matrix4f tempInverse = new Matrix4f();

    public int getMode(int coord) {
        switch (coord) {
            case GL11.GL_S: return modeS;
            case GL11.GL_T: return modeT;
            case GL11.GL_R: return modeR;
            case GL11.GL_Q: return modeQ;
            default: return 0;
        }
    }

    public void setMode(int coord, int mode) {
        switch (coord) {
            case GL11.GL_S: modeS = mode; break;
            case GL11.GL_T: modeT = mode; break;
            case GL11.GL_R: modeR = mode; break;
            case GL11.GL_Q: modeQ = mode; break;
        }
    }

    public float[] getObjectPlane(int coord) {
        switch (coord) {
            case GL11.GL_S: return objectPlaneS;
            case GL11.GL_T: return objectPlaneT;
            case GL11.GL_R: return objectPlaneR;
            case GL11.GL_Q: return objectPlaneQ;
            default: return null;
        }
    }

    public float[] getEyePlane(int coord) {
        switch (coord) {
            case GL11.GL_S: return eyePlaneS;
            case GL11.GL_T: return eyePlaneT;
            case GL11.GL_R: return eyePlaneR;
            case GL11.GL_Q: return eyePlaneQ;
            default: return null;
        }
    }

    public void setObjectPlane(int coord, double a, double b, double c, double d) {
        float[] plane = getObjectPlane(coord);
        if (plane != null) {
            plane[0] = (float) a; plane[1] = (float) b; plane[2] = (float) c; plane[3] = (float) d;
        }
    }

    public void setEyePlane(int coord, double a, double b, double c, double d, Matrix4f modelView) {
        float[] plane = getEyePlane(coord);
        if (plane != null) {
            modelView.invert(tempInverse);
            tempPlane.set((float) a, (float) b, (float) c, (float) d);
            tempInverse.transformTranspose(tempPlane);
            plane[0] = tempPlane.x; plane[1] = tempPlane.y; plane[2] = tempPlane.z; plane[3] = tempPlane.w;
        }
    }
}
