package com.gtnewhorizons.angelica.glsm.states;

import org.lwjgl.opengl.GL11;

public class DepthState implements ISettableState<DepthState> {
    private boolean enabled = true;
    private int func = GL11.GL_LESS;
    private double clearValue = 1.0D;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFunc() {
        return this.func;
    }

    public void setFunc(int func) {
        this.func = func;
    }

    public double getClearValue() {
        return this.clearValue;
    }

    public void setClearValue(double clearValue) {
        this.clearValue = clearValue;
    }

    @Override
    public DepthState set(DepthState state) {
        this.enabled = state.enabled;
        this.func = state.func;
        this.clearValue = state.clearValue;
        return this;
    }

    @Override
    public boolean sameAs(Object state) {
        if (!(state instanceof DepthState other)) {
            return false;
        }
        return this.enabled == other.enabled && this.func == other.func && Double.compare(this.clearValue, other.clearValue) == 0;
    }

    @Override
    public DepthState copy() {
        return new DepthState().set(this);
    }
}
