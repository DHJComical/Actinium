package com.gtnewhorizons.angelica.glsm.hooks;

public interface DeferredDepthColorHandler {
    /** Returns whether a shader depth/color override must remain effective. */
    boolean isOverrideHeld();

    boolean isDepthColorLocked();
    void deferDepthEnable(boolean enabled);
    void deferColorMask(boolean r, boolean g, boolean b, boolean a);
}
