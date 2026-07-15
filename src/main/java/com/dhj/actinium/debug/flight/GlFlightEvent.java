package com.dhj.actinium.debug.flight;

/**
 * Represents one fully committed breadcrumb recovered from a flight recording.
 *
 * @param sequence monotonically increasing recording sequence
 * @param epochMillis wall-clock time in Unix milliseconds
 * @param monotonicNanos process-local monotonic time from {@link System#nanoTime()}
 * @param frame caller-owned frame sequence, or {@code -1} before frames began
 * @param threadId Java thread identifier that emitted the event
 * @param kind broad event classification
 * @param phase operation boundary or instantaneous sample
 * @param stage stable rendering stage
 * @param argument0 event-specific numeric argument
 * @param argument1 event-specific numeric argument
 * @param argument2 event-specific numeric argument
 * @param argument3 event-specific numeric argument
 */
public record GlFlightEvent(
    long sequence,
    long epochMillis,
    long monotonicNanos,
    long frame,
    long threadId,
    GlFlightEventKind kind,
    GlFlightEventPhase phase,
    GlFlightStage stage,
    long argument0,
    long argument1,
    long argument2,
    long argument3
) {
}
