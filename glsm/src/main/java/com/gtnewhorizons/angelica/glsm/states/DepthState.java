package com.gtnewhorizons.angelica.glsm.states;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL11;

@Getter @Setter
public class DepthState implements ISettableState<DepthState> {
    protected boolean enabled = true;
    protected int func = GL11.GL_LESS;
    protected boolean maskEnabled = true;
    protected double clearValue = 1.0;

    @Override
    public DepthState set(DepthState state) {
        this.enabled = state.enabled;
        this.func = state.func;
        this.maskEnabled = state.maskEnabled;
        this.clearValue = state.clearValue;
        return this;
    }

    /** Restores depth comparison state while retaining the active depth-write mask. */
    public void setExceptMask(DepthState state) {
        final boolean savedMask = maskEnabled;
        set(state);
        maskEnabled = savedMask;
    }

    @Override
    public boolean sameAs(Object state) {
        if (this == state) return true;
        if (!(state instanceof DepthState depthState)) return false;
        return enabled == depthState.enabled
            && func == depthState.func
            && maskEnabled == depthState.maskEnabled
            && Double.compare(clearValue, depthState.clearValue) == 0;
    }
    @Override
    public DepthState copy() {
        return new DepthState().set(this);
    }

}
