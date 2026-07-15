package com.gtnewhorizons.angelica.glsm.hooks;

/**
 * Identifies lifecycle events for non-blocking GPU completion checkpoints.
 */
public enum GpuCheckpointType {
    /** A fence was inserted after a low-frequency command or pass boundary. */
    ISSUED(1),
    /** A later zero-timeout poll observed the fence as complete. */
    COMPLETED(2),
    /** The fixed checkpoint ring was full; no fence was inserted. */
    OVERFLOW(3),
    /** The driver rejected fence creation or reported a failed wait. */
    FAILED(4),
    /** Marks exposure immediately before entering {@code glFenceSync}; it does not identify the root cause. */
    FENCE_CALL_BEGIN(5),
    /** Confirms that {@code glFenceSync} returned to Java. */
    FENCE_CALL_RETURNED(6),
    /** Marks exposure immediately before entering {@code glClientWaitSync}; it does not identify the root cause. */
    WAIT_CALL_BEGIN(7),
    /** Records a zero-timeout wait returning with the fence still pending. */
    WAIT_CALL_PENDING(8),
    /** Records a zero-timeout wait returning with the fence completed. */
    WAIT_CALL_COMPLETED(9),
    /** Records a zero-timeout wait returning with a driver-reported failure. */
    WAIT_CALL_FAILED(10),
    /** Marks exposure immediately before entering {@code glDeleteSync}; it does not identify the root cause. */
    DELETE_CALL_BEGIN(11),
    /** Confirms that {@code glDeleteSync} returned to Java. */
    DELETE_CALL_RETURNED(12);

    private final int code;

    GpuCheckpointType(int code) {
        this.code = code;
    }

    /**
     * Returns the stable numeric code persisted in a flight recording.
     *
     * @return stable checkpoint lifecycle code
     */
    public int code() {
        return code;
    }

    /**
     * Resolves a persisted checkpoint lifecycle code during offline analysis.
     *
     * @param code persisted checkpoint lifecycle code
     * @return matching checkpoint lifecycle type
     * @throws IllegalArgumentException when the code is unknown
     */
    public static GpuCheckpointType fromCode(long code) {
        for (GpuCheckpointType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown GPU checkpoint code: " + code);
    }
}
