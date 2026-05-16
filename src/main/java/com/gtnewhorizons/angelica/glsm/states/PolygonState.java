package com.gtnewhorizons.angelica.glsm.states;

import org.lwjgl.opengl.GL11;

/**
 * Polygon state: face culling, polygon mode, offset.
 * Ported from Angelica.
 */
public class PolygonState implements ISettableState<PolygonState> {
    private int cullFace = GL11.GL_BACK;
    private int frontFace = GL11.GL_CCW;
    private int polygonMode = GL11.GL_FILL;
    private float offsetFactor = 0.0f;
    private float offsetUnits = 0.0f;

    public int getCullFace() { return cullFace; }
    public void setCullFace(int cullFace) { this.cullFace = cullFace; }
    public int getFrontFace() { return frontFace; }
    public void setFrontFace(int frontFace) { this.frontFace = frontFace; }
    public int getPolygonMode() { return polygonMode; }
    public void setPolygonMode(int polygonMode) { this.polygonMode = polygonMode; }
    public float getOffsetFactor() { return offsetFactor; }
    public void setOffsetFactor(float offsetFactor) { this.offsetFactor = offsetFactor; }
    public float getOffsetUnits() { return offsetUnits; }
    public void setOffsetUnits(float offsetUnits) { this.offsetUnits = offsetUnits; }

    @Override
    public PolygonState set(PolygonState state) {
        this.cullFace = state.cullFace;
        this.frontFace = state.frontFace;
        this.polygonMode = state.polygonMode;
        this.offsetFactor = state.offsetFactor;
        this.offsetUnits = state.offsetUnits;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof PolygonState ps)) return false;
        return cullFace == ps.cullFace
            && frontFace == ps.frontFace
            && polygonMode == ps.polygonMode
            && Float.compare(offsetFactor, ps.offsetFactor) == 0
            && Float.compare(offsetUnits, ps.offsetUnits) == 0;
    }

    @Override
    public PolygonState copy() {
        return new PolygonState().set(this);
    }
}
