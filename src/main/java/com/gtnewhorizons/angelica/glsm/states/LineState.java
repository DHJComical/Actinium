package com.gtnewhorizons.angelica.glsm.states;

/**
 * Tracks GL line state: width, stipple factor, and stipple pattern.
 * Ported from Angelica.
 */
public class LineState implements ISettableState<LineState> {
    private float width = 1.0f;
    private int stippleFactor = 1;
    private short stipplePattern = (short) 0xFFFF;

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public int getStippleFactor() { return stippleFactor; }
    public void setStippleFactor(int stippleFactor) { this.stippleFactor = stippleFactor; }
    public short getStipplePattern() { return stipplePattern; }
    public void setStipplePattern(short stipplePattern) { this.stipplePattern = stipplePattern; }

    @Override
    public LineState set(LineState state) {
        this.width = state.width;
        this.stippleFactor = state.stippleFactor;
        this.stipplePattern = state.stipplePattern;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof LineState lineState)) return false;
        return Float.compare(width, lineState.width) == 0
            && stippleFactor == lineState.stippleFactor
            && stipplePattern == lineState.stipplePattern;
    }

    @Override
    public LineState copy() {
        return new LineState().set(this);
    }
}
