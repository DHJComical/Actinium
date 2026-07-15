package com.dhj.actinium.debug.flight;

/**
 * Names stable rendering stages that remain meaningful when Java and game logs are unavailable.
 */
public enum GlFlightStage {
    /** Covers recorder and client lifecycle transitions. */
    LIFECYCLE(1),
    /** Covers OpenGL context creation and destruction. */
    GL_CONTEXT(2),
    /** Covers one complete client frame. */
    FRAME(3),
    /** Covers camera setup and the outer world render call. */
    CAMERA_RENDER(4),
    /** Covers the main world render pipeline. */
    WORLD_RENDER(5),
    /** Covers a shader-pack shadow render. */
    SHADOW_RENDER(6),
    /** Covers terrain rendering. */
    TERRAIN(7),
    /** Covers block-entity rendering. */
    BLOCK_ENTITY(8),
    /** Covers entity rendering. */
    ENTITY(9),
    /** Covers particle rendering. */
    PARTICLES(10),
    /** Covers weather rendering. */
    WEATHER(11),
    /** Covers sky rendering. */
    SKY(12),
    /** Covers end-portal rendering and precomposition. */
    END_PORTAL(13),
    /** Covers shader-pack post-processing. */
    POST_PROCESS(14),
    /** Covers final framebuffer composition. */
    COMPOSITE(15),
    /** Covers the native window buffer swap. */
    SWAP_BUFFERS(16),
    /** Covers shader-pipeline creation, selection, and destruction. */
    SHADER_PIPELINE(17),
    /** Covers a diagnostic event that has no more specific stable stage. */
    UNKNOWN(255);

    private final int code;

    GlFlightStage(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }

    static GlFlightStage fromCode(int code) {
        for (GlFlightStage stage : values()) {
            if (stage.code == code) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown GL flight stage code: " + code);
    }
}
