package com.dhj.actinium.debug.flight;

/**
 * Identifies whether a breadcrumb starts, finishes, or samples an operation.
 */
public enum GlFlightEventPhase {
    /** Starts an operation that should later have a matching {@link #END} event. */
    BEGIN(1),
    /** Finishes an operation previously marked with {@link #BEGIN}. */
    END(2),
    /** Samples state at a single point without opening an operation. */
    INSTANT(3);

    private final int code;

    GlFlightEventPhase(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }

    static GlFlightEventPhase fromCode(int code) {
        for (GlFlightEventPhase phase : values()) {
            if (phase.code == code) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown GL flight event phase code: " + code);
    }
}
