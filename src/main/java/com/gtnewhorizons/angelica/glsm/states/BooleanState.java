package com.gtnewhorizons.angelica.glsm.states;

public class BooleanState {
    protected boolean enabled;

    public BooleanState(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
