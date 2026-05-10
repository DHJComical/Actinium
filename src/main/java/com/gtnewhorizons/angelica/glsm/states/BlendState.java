package com.gtnewhorizons.angelica.glsm.states;

import org.lwjgl.opengl.GL11;

public final class BlendState {
    private boolean enabled;
    private int srcRgb = GL11.GL_ONE;
    private int dstRgb = GL11.GL_ZERO;
    private int srcAlpha = GL11.GL_ONE;
    private int dstAlpha = GL11.GL_ZERO;

    public BlendState() {
    }

    public BlendState(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        this.setAll(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSrcRgb() {
        return this.srcRgb;
    }

    public int getDstRgb() {
        return this.dstRgb;
    }

    public int getSrcAlpha() {
        return this.srcAlpha;
    }

    public int getDstAlpha() {
        return this.dstAlpha;
    }

    public void setAll(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        this.srcRgb = srcRgb;
        this.dstRgb = dstRgb;
        this.srcAlpha = srcAlpha;
        this.dstAlpha = dstAlpha;
    }

    public void set(BlendState other) {
        this.enabled = other.enabled;
        this.srcRgb = other.srcRgb;
        this.dstRgb = other.dstRgb;
        this.srcAlpha = other.srcAlpha;
        this.dstAlpha = other.dstAlpha;
    }
}
