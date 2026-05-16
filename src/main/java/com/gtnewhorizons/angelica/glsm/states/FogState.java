package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;

public class FogState {
    private int fogMode = GL11.GL_LINEAR;
    private float density = 1.0F;
    private float start = 0.0F;
    private float end = 1.0F;
    private float fogAlpha = 1.0F;
    private final Vector3d fogColor = new Vector3d();

    public int getFogMode() {
        return this.fogMode;
    }

    public void setFogMode(int fogMode) {
        this.fogMode = fogMode;
    }

    public float getDensity() {
        return this.density;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public float getStart() {
        return this.start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return this.end;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    public float getFogAlpha() {
        return this.fogAlpha;
    }

    public void setFogAlpha(float fogAlpha) {
        this.fogAlpha = fogAlpha;
    }

    public Vector3d getFogColor() {
        return this.fogColor;
    }

    public void set(FogState other) {
        this.fogMode = other.fogMode;
        this.density = other.density;
        this.start = other.start;
        this.end = other.end;
        this.fogAlpha = other.fogAlpha;
        this.fogColor.set(other.fogColor);
    }
}
