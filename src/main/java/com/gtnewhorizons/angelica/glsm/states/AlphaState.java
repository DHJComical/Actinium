package com.gtnewhorizons.angelica.glsm.states;

import org.lwjgl.opengl.GL11;

public class AlphaState implements ISettableState<AlphaState> {
    private int function = GL11.GL_ALWAYS;
    private float reference = 0.0F;

    public int getFunction() {
        return this.function;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public float getReference() {
        return this.reference;
    }

    public void setReference(float reference) {
        this.reference = reference;
    }

    @Override
    public AlphaState set(AlphaState state) {
        this.function = state.function;
        this.reference = state.reference;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (!(state instanceof AlphaState other)) {
            return false;
        }
        return this.function == other.function && Float.compare(this.reference, other.reference) == 0;
    }

    @Override
    public AlphaState copy() {
        return new AlphaState().set(this);
    }
}
