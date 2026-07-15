package com.dhj.actinium.debug.flight;

import java.nio.file.Path;
import java.util.Optional;

final class DisabledGlFlightRecorder implements GlFlightRecorder {
    static final DisabledGlFlightRecorder INSTANCE = new DisabledGlFlightRecorder();

    private DisabledGlFlightRecorder() {
    }

    @Override
    public GlFlightRecorderMode mode() {
        return GlFlightRecorderMode.OFF;
    }

    @Override
    public Optional<Path> recordingPath() {
        return Optional.empty();
    }

    @Override
    public void record(
        GlFlightEventKind kind,
        GlFlightEventPhase phase,
        GlFlightStage stage,
        long frame,
        long argument0,
        long argument1,
        long argument2,
        long argument3
    ) {
    }

    @Override
    public void close() {
    }
}
