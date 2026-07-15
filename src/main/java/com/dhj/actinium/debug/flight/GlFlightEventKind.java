package com.dhj.actinium.debug.flight;

/**
 * Classifies a breadcrumb so offline analysis can filter the timeline without parsing text.
 */
public enum GlFlightEventKind {
    /** Records creation, shutdown, and major runtime state changes. */
    LIFECYCLE(1),
    /** Delimits a rendered frame. */
    FRAME(2),
    /** Delimits a semantic portion of the rendering pipeline. */
    RENDER_STAGE(3),
    /** Records shader-pipeline creation, binding, and destruction. */
    PIPELINE(4),
    /** Delimits buffer presentation to the operating system. */
    SWAP(5),
    /** Records a GPU command immediately before native submission. */
    GPU_COMMAND(6),
    /** Records insertion and non-blocking completion of a GPU fence checkpoint. */
    GPU_CHECKPOINT(7);

    private final int code;

    GlFlightEventKind(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }

    static GlFlightEventKind fromCode(int code) {
        for (GlFlightEventKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown GL flight event kind code: " + code);
    }
}
