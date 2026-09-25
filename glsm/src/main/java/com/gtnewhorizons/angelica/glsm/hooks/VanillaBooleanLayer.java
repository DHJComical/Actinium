package com.gtnewhorizons.angelica.glsm.hooks;

/** Separates a vanilla boolean value from a shader-owned effective value. */
public interface VanillaBooleanLayer {
    /** Returns whether the shader currently owns the effective value. */
    boolean isOverrideHeld();

    /** Returns the deferred vanilla value. */
    boolean getVanilla();

    /** Stores a vanilla value without replacing the shader override. */
    void setVanilla(boolean enabled);
}
