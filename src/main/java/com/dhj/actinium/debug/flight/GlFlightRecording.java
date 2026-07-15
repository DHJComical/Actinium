package com.dhj.actinium.debug.flight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Exposes the single process-wide flight recording lifecycle to Minecraft and Iris render hooks.
 */
public final class GlFlightRecording {
    private static final GlFlightRecordingSession STATE = initialize();

    private GlFlightRecording() {
    }

    /** Starts the next client frame when recording is enabled. */
    public static void beginFrame() {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.beginFrame();
        }
    }

    /** Finishes the active client frame when recording is enabled. */
    public static void endFrame() {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.endFrame();
        }
    }

    /** Marks entry into the native window buffer swap. */
    public static void beginSwap() {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.beginSwap();
        }
    }

    /** Marks successful return from the native window buffer swap. */
    public static void endSwap() {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.endSwap();
        }
    }

    /**
     * Records an existing Iris stage label as a coarse stage plus stable numeric hash.
     *
     * @param label existing stage label; this method does not retain or transform it into new text
     */
    public static void markStage(String label) {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.markStage(label);
        }
    }

    /**
     * Records a dimension transition before the previous shader pipeline is destroyed.
     *
     * @param previousDimension previous dimension key
     * @param currentDimension new dimension key
     */
    public static void dimensionChange(String previousDimension, String currentDimension) {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.dimensionChange(previousDimension, currentDimension);
        }
    }

    /** Records entry into shader-pipeline creation for the supplied dimension key. */
    public static void beginPipelineCreate(String dimension) {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.beginPipelineCreate(dimension);
        }
    }

    /** Records successful completion of shader-pipeline creation. */
    public static void endPipelineCreate(String dimension) {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.endPipelineCreate(dimension);
        }
    }

    /** Records entry into shader-pipeline destruction for the supplied dimension key. */
    public static void beginPipelineDestroy(String dimension) {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.beginPipelineDestroy(dimension);
        }
    }

    /** Records successful completion of shader-pipeline destruction. */
    public static void endPipelineDestroy(String dimension) {
        GlFlightRecordingSession state = STATE;
        if (state != null) {
            state.endPipelineDestroy(dimension);
        }
    }

    /**
     * Explicitly closes a normal client recording; persistence failures are logged without breaking shutdown.
     */
    public static void closeNormally() {
        GlFlightRecordingSession state = STATE;
        if (state == null) {
            return;
        }
        try {
            state.close();
        } catch (IOException | RuntimeException e) {
            Logging.LOGGER.error("Failed to close the GL flight recording", e);
        }
    }

    private static GlFlightRecordingSession initialize() {
        try {
            GlFlightRecorder recorder = GlFlightRecorder.initialize();
            GlFlightRecordingSession state = GlFlightRecordingSession.create(recorder);
            if (state != null) {
                Logging.LOGGER.info("GL flight recorder enabled: mode={} file={}", recorder.mode(), recorder.recordingPath().orElseThrow());
            }
            return state;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize explicitly enabled GL flight recorder", e);
        }
    }

    private static final class Logging {
        private static final Logger LOGGER = LogManager.getLogger("ActiniumGlFlightRecorder");
    }
}
