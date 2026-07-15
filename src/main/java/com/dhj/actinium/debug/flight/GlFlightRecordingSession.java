package com.dhj.actinium.debug.flight;

import com.gtnewhorizons.angelica.glsm.hooks.GpuCheckpointType;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandPhase;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandType;

import java.io.IOException;
import java.util.Objects;

/**
 * Maintains paired process rendering lifecycle events for one enabled recorder.
 */
final class GlFlightRecordingSession implements AutoCloseable {
    private final GlFlightRecorder recorder;
    private long nextFrame;
    private long currentFrame = -1L;
    private boolean swapActive;
    private boolean closed;
    private final ThreadLocal<CommandContext> commandContext = ThreadLocal.withInitial(CommandContext::new);

    GlFlightRecordingSession(GlFlightRecorder recorder) {
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        if (recorder.mode() == GlFlightRecorderMode.OFF) {
            throw new IllegalArgumentException("Recording session requires an enabled recorder");
        }
        recorder.record(
            GlFlightEventKind.LIFECYCLE,
            GlFlightEventPhase.BEGIN,
            GlFlightStage.LIFECYCLE,
            -1L,
            ProcessHandle.current().pid(),
            0L,
            0L,
            0L
        );
    }

    static GlFlightRecordingSession create(GlFlightRecorder recorder) {
        Objects.requireNonNull(recorder, "recorder");
        return recorder.mode() == GlFlightRecorderMode.OFF ? null : new GlFlightRecordingSession(recorder);
    }

    long beginFrame() {
        if (currentFrame != -1L) {
            throw new IllegalStateException("GL flight recorder frame is already active");
        }
        currentFrame = nextFrame++;
        commandContext.get().beginFrame(currentFrame);
        recorder.record(
            GlFlightEventKind.FRAME,
            GlFlightEventPhase.BEGIN,
            GlFlightStage.FRAME,
            currentFrame,
            0L,
            0L,
            0L,
            0L
        );
        return currentFrame;
    }

    void endFrame() {
        if (currentFrame == -1L) {
            throw new IllegalStateException("GL flight recorder frame is not active");
        }
        if (swapActive) {
            throw new IllegalStateException("GL flight recorder swap is still active at frame end");
        }
        recorder.record(
            GlFlightEventKind.FRAME,
            GlFlightEventPhase.END,
            GlFlightStage.FRAME,
            currentFrame,
            0L,
            0L,
            0L,
            0L
        );
        commandContext.get().endFrame(currentFrame);
        currentFrame = -1L;
    }

    void beginSwap() {
        if (currentFrame == -1L) {
            throw new IllegalStateException("GL flight recorder swap began outside a frame");
        }
        if (swapActive) {
            throw new IllegalStateException("GL flight recorder swap is already active");
        }
        swapActive = true;
        recorder.record(
            GlFlightEventKind.SWAP,
            GlFlightEventPhase.BEGIN,
            GlFlightStage.SWAP_BUFFERS,
            currentFrame,
            0L,
            0L,
            0L,
            0L
        );
    }

    void endSwap() {
        if (!swapActive) {
            throw new IllegalStateException("GL flight recorder swap is not active");
        }
        recorder.record(
            GlFlightEventKind.SWAP,
            GlFlightEventPhase.END,
            GlFlightStage.SWAP_BUFFERS,
            currentFrame,
            0L,
            0L,
            0L,
            0L
        );
        swapActive = false;
    }

    void markStage(String label) {
        CommandContext context = commandContext.get();
        context.stage = GlFlightStageClassifier.classify(label);
        context.stageHash = GlFlightHash.stableHash(label);
        recorder.record(
            GlFlightEventKind.RENDER_STAGE,
            GlFlightEventPhase.INSTANT,
            context.stage,
            context.frame,
            context.stageHash,
            0L,
            0L,
            0L
        );
    }

    void gpuCommand(
        GpuCommandType type,
        GpuCommandPhase phase,
        int program,
        int drawFramebuffer,
        int operand0,
        int operand1
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(phase, "phase");
        CommandContext context = commandContext.get();
        recorder.record(
            GlFlightEventKind.GPU_COMMAND,
            switch (phase) {
                case ISSUED -> GlFlightEventPhase.INSTANT;
                case BEGIN -> GlFlightEventPhase.BEGIN;
                case END -> GlFlightEventPhase.END;
            },
            context.stage,
            context.frame,
            type.code(),
            context.stageHash,
            packInts(program, drawFramebuffer),
            packInts(operand0, operand1)
        );
    }

    void gpuCheckpoint(GpuCheckpointType type, long checkpointId, int commandCode) {
        Objects.requireNonNull(type, "type");
        CommandContext context = commandContext.get();
        recorder.record(
            GlFlightEventKind.GPU_CHECKPOINT,
            GlFlightEventPhase.INSTANT,
            context.stage,
            context.frame,
            type.code(),
            checkpointId,
            commandCode,
            context.stageHash
        );
    }

    void beginStreamingSync(GlFlightStreamingSource source) {
        recordStreamingSync(GlFlightEventPhase.BEGIN, source);
    }

    void endStreamingSync(GlFlightStreamingSource source) {
        recordStreamingSync(GlFlightEventPhase.END, source);
    }

    void dimensionChange(String previousDimension, String currentDimension) {
        recorder.record(
            GlFlightEventKind.PIPELINE,
            GlFlightEventPhase.INSTANT,
            GlFlightStage.SHADER_PIPELINE,
            currentFrame,
            GlFlightPipelineAction.DIMENSION_CHANGE.code(),
            GlFlightHash.stableNullableHash(previousDimension),
            GlFlightHash.stableHash(currentDimension),
            0L
        );
    }

    void beginPipelineCreate(String dimension) {
        recordPipelineBoundary(GlFlightEventPhase.BEGIN, GlFlightPipelineAction.CREATE, dimension);
    }

    void endPipelineCreate(String dimension) {
        recordPipelineBoundary(GlFlightEventPhase.END, GlFlightPipelineAction.CREATE, dimension);
    }

    void beginPipelineDestroy(String dimension) {
        recordPipelineBoundary(GlFlightEventPhase.BEGIN, GlFlightPipelineAction.DESTROY, dimension);
    }

    void endPipelineDestroy(String dimension) {
        recordPipelineBoundary(GlFlightEventPhase.END, GlFlightPipelineAction.DESTROY, dimension);
    }

    long currentFrame() {
        return currentFrame;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        Exception failure = null;
        try {
            recorder.record(
                GlFlightEventKind.LIFECYCLE,
                GlFlightEventPhase.END,
                GlFlightStage.LIFECYCLE,
                currentFrame,
                ProcessHandle.current().pid(),
                0L,
                0L,
                0L
            );
        } catch (RuntimeException e) {
            failure = e;
        }

        try {
            recorder.close();
        } catch (IOException | RuntimeException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        rethrowCloseFailure(failure);
    }

    private void recordPipelineBoundary(
        GlFlightEventPhase phase,
        GlFlightPipelineAction action,
        String dimension
    ) {
        recorder.record(
            GlFlightEventKind.PIPELINE,
            phase,
            GlFlightStage.SHADER_PIPELINE,
            currentFrame,
            action.code(),
            GlFlightHash.stableHash(dimension),
            0L,
            0L
        );
    }

    private void recordStreamingSync(GlFlightEventPhase phase, GlFlightStreamingSource source) {
        Objects.requireNonNull(source, "source");
        recorder.record(
            GlFlightEventKind.RENDER_STAGE,
            phase,
            GlFlightStage.STREAMING_SYNC,
            currentFrame,
            source.code(),
            0L,
            0L,
            0L
        );
    }

    private static void rethrowCloseFailure(Exception failure) throws IOException {
        if (failure instanceof IOException ioException) {
            throw ioException;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
    }

    static long packInts(int high, int low) {
        return (long) high << 32 | low & 0xFFFFFFFFL;
    }

    /** Holds command attribution that must never leak between issuing threads. */
    private static final class CommandContext {
        private long frame = -1L;
        private GlFlightStage stage = GlFlightStage.FRAME;
        private long stageHash;

        void beginFrame(long frame) {
            this.frame = frame;
            stage = GlFlightStage.FRAME;
            stageHash = 0L;
        }

        void endFrame(long expectedFrame) {
            if (frame != expectedFrame) {
                throw new IllegalStateException("GL flight command context does not own the active frame");
            }
            frame = -1L;
            stage = GlFlightStage.FRAME;
            stageHash = 0L;
        }
    }
}
