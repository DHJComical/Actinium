package com.dhj.actinium.debug.flight;

/**
 * Selects whether semantic rendering breadcrumbs are retained before a native crash.
 */
public enum GlFlightRecorderMode {
    /** Disables the recorder without creating a diagnostic file. */
    OFF(0),
    /** Retains lifecycle, frame, render-stage, pipeline, and presentation breadcrumbs. */
    CRASH(1);

    private final int code;

    GlFlightRecorderMode(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }

    static GlFlightRecorderMode fromCode(int code) {
        for (GlFlightRecorderMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown GL flight recorder mode code: " + code);
    }

    static GlFlightRecorderMode parse(String value) {
        return switch (value) {
            case "off" -> OFF;
            case "crash" -> CRASH;
            default -> throw new IllegalArgumentException(
                "Invalid actinium.glFlightRecorder value '" + value + "'; expected off or crash"
            );
        };
    }
}
