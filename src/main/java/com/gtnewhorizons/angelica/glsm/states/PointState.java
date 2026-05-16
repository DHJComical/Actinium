package com.gtnewhorizons.angelica.glsm.states;

/**
 * Point state (point size).
 * Ported from Angelica.
 */
public class PointState implements ISettableState<PointState> {
    private float size = 1.0f;

    public float getSize() { return size; }
    public void setSize(float size) { this.size = size; }

    @Override
    public PointState set(PointState state) {
        this.size = state.size;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof PointState pointState)) return false;
        return Float.compare(size, pointState.size) == 0;
    }

    @Override
    public PointState copy() {
        return new PointState().set(this);
    }
}
