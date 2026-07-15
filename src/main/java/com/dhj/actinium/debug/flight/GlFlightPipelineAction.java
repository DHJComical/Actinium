package com.dhj.actinium.debug.flight;

/**
 * Identifies pipeline lifecycle operations stored in the first numeric event argument.
 */
public enum GlFlightPipelineAction {
    /** Records a transition between two shader dimension keys. */
    DIMENSION_CHANGE(1),
    /** Records construction of a world rendering pipeline. */
    CREATE(2),
    /** Records destruction of a world rendering pipeline. */
    DESTROY(3);

    private final int code;

    GlFlightPipelineAction(int code) {
        this.code = code;
    }

    /**
     * Returns the stable binary code written to a pipeline breadcrumb.
     *
     * @return stable action code
     */
    public int code() {
        return code;
    }

    /**
     * Resolves a stored pipeline action code for offline decoding.
     *
     * @param code stored numeric action code
     * @return matching pipeline action
     * @throws IllegalArgumentException when the code is unknown
     */
    public static GlFlightPipelineAction fromCode(long code) {
        for (GlFlightPipelineAction action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown GL flight pipeline action code: " + code);
    }
}
