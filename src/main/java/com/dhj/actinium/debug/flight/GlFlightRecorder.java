package com.dhj.actinium.debug.flight;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Records allocation-free numeric rendering breadcrumbs in a crash-resilient memory-mapped ring.
 *
 * <p>Callers own the recorder lifetime and must call {@link #close()} on the normal client shutdown
 * path. A native process termination intentionally bypasses close and leaves the file marked as an
 * unclean recording.</p>
 */
public interface GlFlightRecorder extends AutoCloseable {
    /** System property that selects {@code off} or semantic {@code crash} capture. */
    String MODE_PROPERTY = "actinium.glFlightRecorder";

    /**
     * Returns the capture mode fixed when this recorder was initialized.
     *
     * @return the immutable capture mode
     */
    GlFlightRecorderMode mode();

    /**
     * Returns the file receiving committed events when recording is enabled.
     *
     * @return recording path, or an empty value for the disabled recorder
     */
    Optional<Path> recordingPath();

    /**
     * Records one numeric breadcrumb using the current thread and current wall/monotonic clocks.
     * @param kind broad event classification
     * @param phase operation boundary or instantaneous sample
     * @param stage stable rendering stage
     * @param frame caller-owned frame sequence, or {@code -1} before frames begin
     * @param argument0 event-specific numeric argument
     * @param argument1 event-specific numeric argument
     * @param argument2 event-specific numeric argument
     * @param argument3 event-specific numeric argument
     */
    void record(
        GlFlightEventKind kind,
        GlFlightEventPhase phase,
        GlFlightStage stage,
        long frame,
        long argument0,
        long argument1,
        long argument2,
        long argument3
    );

    /**
     * Marks the recording as cleanly closed and synchronizes it to storage.
     *
     * @throws IOException when the mapped recording cannot be persisted or closed
     */
    @Override
    void close() throws IOException;

    /**
     * Creates a recorder selected by {@link #MODE_PROPERTY} in the current game's diagnostics directory.
     * The default mode is off and creates no file.
     *
     * @return an initialized recorder owned by the caller
     * @throws IOException when an enabled recorder file cannot be created
     * @throws IllegalArgumentException when the property contains an unsupported mode
     */
    static GlFlightRecorder initialize() throws IOException {
        return GlFlightRecorderFactory.openConfigured();
    }

    /**
     * Opens a recorder at an explicit path, primarily for launch integration and diagnostic tooling.
     *
     * @param path new file that will contain the recording
     * @param mode capture mode; off returns a no-op recorder and creates no file
     * @param slotCount fixed number of events retained by the ring
     * @return an initialized recorder owned by the caller
     * @throws IOException when the enabled recorder file cannot be created
     */
    static GlFlightRecorder open(Path path, GlFlightRecorderMode mode, int slotCount) throws IOException {
        return GlFlightRecorderFactory.open(path, mode, slotCount);
    }
}
