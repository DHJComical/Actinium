package com.gtnewhorizons.angelica.glsm.states;

/**
 * Integer parameter state for GL state tracking.
 * Ported from Angelica.
 */
public class IntegerState implements ISettableState<IntegerState> {
    private int value;

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    @Override
    public IntegerState set(IntegerState state) {
        this.value = state.value;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof IntegerState integerState)) return false;
        return value == integerState.value;
    }

    @Override
    public IntegerState copy() {
        return new IntegerState().set(this);
    }
}
