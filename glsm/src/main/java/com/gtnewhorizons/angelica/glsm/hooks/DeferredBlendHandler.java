package com.gtnewhorizons.angelica.glsm.hooks;

public interface DeferredBlendHandler {
    /** Returns whether a shader blend override must remain effective. */
    boolean isOverrideHeld();

    boolean isBlendLocked();
    void deferBlendModeToggle(boolean enabled);
    void deferBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha);
    void flushDeferredBlend();
}
