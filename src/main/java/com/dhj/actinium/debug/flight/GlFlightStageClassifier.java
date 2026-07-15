package com.dhj.actinium.debug.flight;

import java.util.Objects;

/**
 * Maps stable Iris debug-label prefixes to coarse stages while preserving the full-label hash separately.
 */
public final class GlFlightStageClassifier {
    private GlFlightStageClassifier() {
    }

    /**
     * Classifies an existing stage label without substring creation or regular expressions.
     *
     * @param label existing Iris stage label
     * @return coarse stable flight-recorder stage
     */
    public static GlFlightStage classify(String label) {
        Objects.requireNonNull(label, "label");
        if (label.startsWith("composite:") || label.startsWith("framebuffer-output:")) {
            return GlFlightStage.COMPOSITE;
        }
        if (label.startsWith("final:")) {
            return GlFlightStage.POST_PROCESS;
        }
        if (label.startsWith("render-world-pass:") || label.startsWith("level:")) {
            return GlFlightStage.WORLD_RENDER;
        }
        if (label.startsWith("mixin:shadows:") || label.startsWith("shadow:")) {
            return GlFlightStage.SHADOW_RENDER;
        }
        if (label.startsWith("hand-") || label.startsWith("entity-renderer:")) {
            return GlFlightStage.ENTITY;
        }
        if (label.startsWith("terrain:")) {
            return GlFlightStage.TERRAIN;
        }
        if (label.startsWith("sky:")) {
            return GlFlightStage.SKY;
        }
        if (label.startsWith("particles:")) {
            return GlFlightStage.PARTICLES;
        }
        if (label.startsWith("weather:")) {
            return GlFlightStage.WEATHER;
        }
        if (label.startsWith("end-portal:")) {
            return GlFlightStage.END_PORTAL;
        }
        if (label.startsWith("minecraft:before-update-display")
            || label.startsWith("minecraft:after-update-display")) {
            return GlFlightStage.SWAP_BUFFERS;
        }
        if (label.startsWith("minecraft:")) {
            return GlFlightStage.FRAME;
        }
        if (label.startsWith("mixin:begin-world:") || label.startsWith("mixin:pre-sky-prepare:")) {
            return GlFlightStage.WORLD_RENDER;
        }
        if (label.startsWith("mixin:finalize:")) {
            return GlFlightStage.POST_PROCESS;
        }
        return GlFlightStage.UNKNOWN;
    }
}
