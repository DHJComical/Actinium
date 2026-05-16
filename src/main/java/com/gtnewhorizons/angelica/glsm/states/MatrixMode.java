package com.gtnewhorizons.angelica.glsm.states;

import org.lwjgl.opengl.GL11;

/**
 * Current matrix mode tracking (MODELVIEW, PROJECTION, TEXTURE, COLOR).
 * Ported from Angelica.
 */
public class MatrixMode implements ISettableState<MatrixMode> {
    protected int mode = GL11.GL_MODELVIEW;

    public int getMode() { return mode; }

    public void setMode(int mode) {
        if (mode != GL11.GL_MODELVIEW && mode != GL11.GL_PROJECTION
            && mode != GL11.GL_TEXTURE && mode != GL11.GL_COLOR) {
            return;
        }
        this.mode = mode;
    }

    public int getMatrix() {
        switch (mode) {
            case GL11.GL_MODELVIEW: return GL11.GL_MODELVIEW_MATRIX;
            case GL11.GL_PROJECTION: return GL11.GL_PROJECTION_MATRIX;
            case GL11.GL_TEXTURE: return GL11.GL_TEXTURE_MATRIX;
            default: throw new IllegalStateException("Unexpected mode: " + mode);
        }
    }

    @Override
    public MatrixMode set(MatrixMode state) {
        this.mode = state.mode;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof MatrixMode matrixMode)) return false;
        return mode == matrixMode.mode;
    }

    @Override
    public MatrixMode copy() {
        return new MatrixMode().set(this);
    }
}
