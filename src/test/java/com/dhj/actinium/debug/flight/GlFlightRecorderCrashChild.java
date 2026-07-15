package com.dhj.actinium.debug.flight;

import java.nio.file.Path;

/**
 * Test process that deliberately leaves a mapped recording open until its parent terminates it.
 */
public final class GlFlightRecorderCrashChild {
    private GlFlightRecorderCrashChild() {
    }

    /**
     * Creates committed breadcrumbs and waits without executing the recorder's normal close path.
     *
     * @param arguments first argument is the recording file path
     * @throws Exception when test-process initialization fails
     */
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one recording path");
        }

        GlFlightRecorder recorder = GlFlightRecorder.open(
            Path.of(arguments[0]),
            GlFlightRecorderMode.CRASH,
            8
        );
        recorder.record(
            GlFlightEventKind.LIFECYCLE,
            GlFlightEventPhase.BEGIN,
            GlFlightStage.LIFECYCLE,
            -1L,
            11L,
            12L,
            13L,
            14L
        );
        recorder.record(
            GlFlightEventKind.FRAME,
            GlFlightEventPhase.BEGIN,
            GlFlightStage.FRAME,
            77L,
            21L,
            22L,
            23L,
            24L
        );
        System.out.println("READY");
        System.out.flush();
        Thread.sleep(Long.MAX_VALUE);
    }
}
