package com.dhj.actinium.debug.flight;

import java.nio.file.Path;
import java.util.List;

/**
 * Contains recording metadata and the ordered, fully committed events recovered for offline analysis.
 *
 * @param path source recording path
 * @param formatVersion binary file format version
 * @param mode capture mode stored by the writer
 * @param cleanClose whether the client completed the explicit normal close path
 * @param processId process that created the recording
 * @param startEpochMillis writer initialization time in Unix milliseconds
 * @param startMonotonicNanos writer initialization value from {@link System#nanoTime()}
 * @param events immutable timeline ordered by recording sequence
 */
public record GlFlightTimeline(
    Path path,
    int formatVersion,
    GlFlightRecorderMode mode,
    boolean cleanClose,
    long processId,
    long startEpochMillis,
    long startMonotonicNanos,
    List<GlFlightEvent> events
) {
    /**
     * Makes the event timeline immutable before exposing it to diagnostic tooling.
     */
    public GlFlightTimeline {
        events = List.copyOf(events);
    }
}
