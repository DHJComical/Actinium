package com.gtnewhorizons.angelica.glsm.hooks;

/** Separates a vanilla structured state from a shader-owned effective state. */
public interface VanillaStateLayer<T> {
    /** Returns whether the shader currently owns the effective value. */
    boolean isOverrideHeld();

    /** Copies the deferred vanilla value into {@code into}. */
    void readVanilla(T into);

    /** Stores a vanilla value without replacing the shader override. */
    void writeVanilla(T from);

    static boolean isHeld(VanillaStateLayer<?> layer) {
        return layer != null && layer.isOverrideHeld();
    }

    static <T> void capture(VanillaStateLayer<T> layer, T slot) {
        if (isHeld(layer)) {
            layer.readVanilla(slot);
        }
    }

    static <T> boolean restore(VanillaStateLayer<T> layer, T saved) {
        if (!isHeld(layer)) {
            return false;
        }
        layer.writeVanilla(saved);
        return true;
    }
}
