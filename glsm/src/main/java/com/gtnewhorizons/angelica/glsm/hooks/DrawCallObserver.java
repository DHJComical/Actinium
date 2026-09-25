package com.gtnewhorizons.angelica.glsm.hooks;

/** Observes the vertex inputs selected immediately before a native draw submission. */
@FunctionalInterface
public interface DrawCallObserver {
    /**
     * Receives the vertex flags derived for the pending draw.
     *
     * @param vertexFlags GLSM vertex-format flags active for the draw
     */
    void beforeDraw(int vertexFlags);
}
