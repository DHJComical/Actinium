package com.dhj.actinium.debug.flight;

/**
 * Identifies the persistent streaming source synchronized at the end of a rendered frame.
 */
public enum GlFlightStreamingSource {
    /** Angelica's persistent tessellator streaming buffers. */
    TESSELLATOR(1),
    /** Actinium's persistent BufferBuilder streaming buffers. */
    BUFFER_BUILDER(2);

    private final int code;

    GlFlightStreamingSource(int code) {
        this.code = code;
    }

    /**
     * Returns the stable binary code written to the first event argument.
     *
     * @return stable source code
     */
    public int code() {
        return code;
    }

    /**
     * Resolves a stored source code during offline analysis.
     *
     * @param code stored numeric source code
     * @return matching streaming source
     * @throws IllegalArgumentException when the code is unknown
     */
    public static GlFlightStreamingSource fromCode(long code) {
        for (GlFlightStreamingSource source : values()) {
            if (source.code == code) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown GL flight streaming source code: " + code);
    }
}
