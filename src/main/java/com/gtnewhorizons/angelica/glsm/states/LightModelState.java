package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL12;

/**
 * Light model state (ambient, local viewer, two-side lighting).
 * Ported from Angelica.
 */
public class LightModelState implements ISettableState<LightModelState> {
    public final Vector4f ambient;
    public int colorControl;
    public float localViewer;
    public float twoSide;

    public LightModelState() {
        ambient = new Vector4f(0.2F, 0.2F, 0.2F, 1.0F);
        colorControl = GL12.GL_SINGLE_COLOR;
        localViewer = 0.0F;
        twoSide = 0.0F;
    }

    public Vector4f getAmbient() { return ambient; }
    public int getColorControl() { return colorControl; }
    public void setColorControl(int colorControl) { this.colorControl = colorControl; }
    public float getLocalViewer() { return localViewer; }
    public void setLocalViewer(float localViewer) { this.localViewer = localViewer; }
    public float getTwoSide() { return twoSide; }
    public void setTwoSide(float twoSide) { this.twoSide = twoSide; }

    @Override
    public LightModelState set(LightModelState state) {
        this.ambient.set(state.ambient);
        this.colorControl = state.colorControl;
        this.localViewer = state.localViewer;
        this.twoSide = state.twoSide;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof LightModelState lms)) return false;
        return ambient.equals(lms.ambient)
            && colorControl == lms.colorControl
            && Float.compare(localViewer, lms.localViewer) == 0
            && Float.compare(twoSide, lms.twoSide) == 0;
    }

    @Override
    public LightModelState copy() {
        return new LightModelState().set(this);
    }
}
