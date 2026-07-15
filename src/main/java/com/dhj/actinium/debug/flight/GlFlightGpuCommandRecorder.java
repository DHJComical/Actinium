package com.dhj.actinium.debug.flight;

import com.gtnewhorizons.angelica.glsm.hooks.GpuCheckpointType;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandPhase;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandRecorder;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandType;

/**
 * Bridges primitive GLSM command callbacks into the process-wide GL flight recorder.
 */
public enum GlFlightGpuCommandRecorder implements GpuCommandRecorder {
    /** Singleton callback installed only while flight recording is enabled. */
    INSTANCE;

    @Override
    public void record(
        GpuCommandType type,
        GpuCommandPhase phase,
        int program,
        int drawFramebuffer,
        int operand0,
        int operand1
    ) {
        GlFlightRecording.gpuCommand(type, phase, program, drawFramebuffer, operand0, operand1);
    }

    @Override
    public void checkpoint(GpuCheckpointType type, long checkpointId, int commandCode) {
        GlFlightRecording.gpuCheckpoint(type, checkpointId, commandCode);
    }
}
