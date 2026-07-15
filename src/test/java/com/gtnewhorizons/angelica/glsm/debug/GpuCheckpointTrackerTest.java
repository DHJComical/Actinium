package com.gtnewhorizons.angelica.glsm.debug;

import com.gtnewhorizons.angelica.glsm.hooks.GpuCheckpointType;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandPhase;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandRecorder;
import com.gtnewhorizons.angelica.glsm.hooks.GpuCommandType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpuCheckpointTrackerTest {
    @Test
    void preservesPendingFencesWhenSwitchingContexts() {
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations();
        Object firstContext = new Object();
        Object secondContext = new Object();

        state.checkpoint(recorder, GpuCommandType.CLEAR.code(), firstContext, syncOperations);
        state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), secondContext, syncOperations);
        syncOperations.statuses.put(1L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        state.checkpoint(recorder, GpuCommandType.GENERATE_MIPMAP.code(), firstContext, syncOperations);

        assertEquals(
            List.of(
                new CheckpointEvent(GpuCheckpointType.ISSUED, 0L, GpuCommandType.CLEAR.code()),
                new CheckpointEvent(GpuCheckpointType.ISSUED, 1L, GpuCommandType.BLIT_FRAMEBUFFER.code()),
                new CheckpointEvent(GpuCheckpointType.COMPLETED, 0L, GpuCommandType.CLEAR.code()),
                new CheckpointEvent(GpuCheckpointType.ISSUED, 2L, GpuCommandType.GENERATE_MIPMAP.code())
            ),
            recorder.lifecycleEvents()
        );
        assertEquals(List.of(1L), syncOperations.deletedSyncs);
    }

    @Test
    void recordsNativeFenceAndWaitExposureAroundCalls() {
        List<String> trace = new ArrayList<>();
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder(trace);
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations(trace);
        Object context = new Object();

        state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);

        assertEquals(
            List.of(
                "checkpoint:FENCE_CALL_BEGIN",
                "native:fence",
                "checkpoint:FENCE_CALL_RETURNED",
                "checkpoint:ISSUED"
            ),
            trace
        );

        trace.clear();
        state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), context, syncOperations);

        assertEquals(
            List.of(
                "checkpoint:WAIT_CALL_BEGIN",
                "native:wait",
                "checkpoint:WAIT_CALL_PENDING",
                "checkpoint:FENCE_CALL_BEGIN",
                "native:fence",
                "checkpoint:FENCE_CALL_RETURNED",
                "checkpoint:ISSUED"
            ),
            trace
        );
    }

    @Test
    void leavesTheExposureBreadcrumbWhenANativeSyncCallFailsFast() {
        List<String> trace = new ArrayList<>();
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder(trace);
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations(trace);
        Object context = new Object();
        syncOperations.fenceFailure = new IllegalStateException("fence failed");

        assertThrows(
            IllegalStateException.class,
            () -> state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations)
        );
        assertEquals(List.of("checkpoint:FENCE_CALL_BEGIN", "native:fence"), trace);

        trace.clear();
        syncOperations.fenceFailure = null;
        state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);
        trace.clear();
        syncOperations.pollFailure = new IllegalStateException("wait failed");

        assertThrows(
            IllegalStateException.class,
            () -> state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), context, syncOperations)
        );
        assertEquals(List.of("checkpoint:WAIT_CALL_BEGIN", "native:wait"), trace);
    }

    @Test
    void recordsNativeFailureResultsAfterTheCallsReturn() {
        GpuCheckpointTracker.State zeroFenceState = new GpuCheckpointTracker.State();
        CapturingCommandRecorder zeroFenceRecorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations zeroFenceOperations = new FakeGpuSyncOperations();
        zeroFenceOperations.returnZeroFence = true;

        zeroFenceState.checkpoint(
            zeroFenceRecorder,
            GpuCommandType.CLEAR.code(),
            new Object(),
            zeroFenceOperations
        );

        assertEquals(
            List.of(
                GpuCheckpointType.FENCE_CALL_BEGIN,
                GpuCheckpointType.FENCE_CALL_RETURNED,
                GpuCheckpointType.FAILED
            ),
            zeroFenceRecorder.checkpoints.stream().map(CheckpointEvent::type).toList()
        );

        GpuCheckpointTracker.State failedWaitState = new GpuCheckpointTracker.State();
        CapturingCommandRecorder failedWaitRecorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations failedWaitOperations = new FakeGpuSyncOperations();
        Object context = new Object();
        failedWaitState.checkpoint(
            failedWaitRecorder,
            GpuCommandType.BLIT_FRAMEBUFFER.code(),
            context,
            failedWaitOperations
        );
        failedWaitRecorder.checkpoints.clear();
        failedWaitOperations.statuses.put(1L, GpuCheckpointTracker.GpuSyncStatus.FAILED);

        failedWaitState.checkpoint(
            failedWaitRecorder,
            GpuCommandType.GENERATE_MIPMAP.code(),
            context,
            failedWaitOperations
        );

        assertEquals(
            List.of(
                GpuCheckpointType.WAIT_CALL_BEGIN,
                GpuCheckpointType.WAIT_CALL_FAILED,
                GpuCheckpointType.DELETE_CALL_BEGIN,
                GpuCheckpointType.DELETE_CALL_RETURNED,
                GpuCheckpointType.FAILED
            ),
            failedWaitRecorder.checkpoints.subList(0, 5).stream().map(CheckpointEvent::type).toList()
        );
    }

    @Test
    void recordsDeleteExposureAroundReturnAndLeavesBeginOnFastFail() {
        List<String> trace = new ArrayList<>();
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder(trace);
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations(trace);
        Object context = new Object();
        state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);
        syncOperations.statuses.put(1L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        trace.clear();

        state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), context, syncOperations);

        assertEquals(
            List.of(
                "checkpoint:WAIT_CALL_BEGIN",
                "native:wait",
                "checkpoint:WAIT_CALL_COMPLETED",
                "checkpoint:DELETE_CALL_BEGIN",
                "native:delete",
                "checkpoint:DELETE_CALL_RETURNED",
                "checkpoint:COMPLETED"
            ),
            trace.subList(0, 7)
        );

        GpuCheckpointTracker.State failingState = new GpuCheckpointTracker.State();
        trace.clear();
        recorder = new CapturingCommandRecorder(trace);
        syncOperations = new FakeGpuSyncOperations(trace);
        failingState.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);
        syncOperations.statuses.put(1L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        syncOperations.deleteFailure = new IllegalStateException("delete failed");
        trace.clear();
        CapturingCommandRecorder failingRecorder = recorder;
        FakeGpuSyncOperations failingOperations = syncOperations;

        assertThrows(
            IllegalStateException.class,
            () -> failingState.checkpoint(
                failingRecorder,
                GpuCommandType.BLIT_FRAMEBUFFER.code(),
                context,
                failingOperations
            )
        );
        assertEquals(
            List.of(
                "checkpoint:WAIT_CALL_BEGIN",
                "native:wait",
                "checkpoint:WAIT_CALL_COMPLETED",
                "checkpoint:DELETE_CALL_BEGIN",
                "native:delete"
            ),
            trace
        );
    }

    @Test
    void stopsPollingAtTheFirstPendingFence() {
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations();
        Object context = new Object();
        state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);
        state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), context, syncOperations);
        state.checkpoint(recorder, GpuCommandType.GENERATE_MIPMAP.code(), context, syncOperations);
        syncOperations.polledSyncs.clear();
        syncOperations.statuses.put(2L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);

        state.checkpoint(recorder, GpuCommandType.TEX_STORAGE_2D.code(), context, syncOperations);

        assertEquals(List.of(1L), syncOperations.polledSyncs);
        assertEquals(List.of(), recorder.eventsOfType(GpuCheckpointType.COMPLETED));
    }

    @Test
    void reclaimsConsecutiveCompletedFencesThenStopsAtPendingHead() {
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations();
        Object context = new Object();
        for (int index = 0; index < 4; index++) {
            state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);
        }
        syncOperations.polledSyncs.clear();
        syncOperations.statuses.put(1L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        syncOperations.statuses.put(2L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        syncOperations.statuses.put(4L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);

        state.checkpoint(recorder, GpuCommandType.TEX_STORAGE_3D.code(), context, syncOperations);

        assertEquals(List.of(1L, 2L, 3L), syncOperations.polledSyncs);
        assertEquals(List.of(1L, 2L), syncOperations.deletedSyncs);
        assertEquals(
            List.of(
                new CheckpointEvent(GpuCheckpointType.COMPLETED, 0L, GpuCommandType.CLEAR.code()),
                new CheckpointEvent(GpuCheckpointType.COMPLETED, 1L, GpuCommandType.CLEAR.code())
            ),
            recorder.eventsOfType(GpuCheckpointType.COMPLETED)
        );
    }

    @Test
    void ringWrapAroundPreservesFifoOrderAndOverflowIds() {
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations();
        Object context = new Object();

        for (int index = 0; index < GpuCheckpointTracker.CHECKPOINT_CAPACITY + 2; index++) {
            state.checkpoint(recorder, GpuCommandType.CLEAR.code(), context, syncOperations);
        }

        List<CheckpointEvent> overflows = recorder.eventsOfType(GpuCheckpointType.OVERFLOW);
        assertEquals(
            List.of(new CheckpointEvent(
                GpuCheckpointType.OVERFLOW,
                GpuCheckpointTracker.OVERFLOW_CHECKPOINT_ID,
                GpuCommandType.CLEAR.code()
            )),
            overflows
        );
        assertEquals(GpuCheckpointTracker.CHECKPOINT_CAPACITY, syncOperations.fenceCount);
        assertEquals(
            GpuCheckpointTracker.CHECKPOINT_CAPACITY - 1L,
            recorder.eventsOfType(GpuCheckpointType.ISSUED).getLast().checkpointId()
        );

        syncOperations.statuses.put(1L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        syncOperations.polledSyncs.clear();
        state.checkpoint(recorder, GpuCommandType.GENERATE_MIPMAP.code(), context, syncOperations);

        assertEquals(GpuCheckpointTracker.CHECKPOINT_CAPACITY + 1, syncOperations.fenceCount);
        assertEquals(List.of(1L, 2L), syncOperations.polledSyncs);
        assertEquals(
            GpuCheckpointTracker.CHECKPOINT_CAPACITY,
            recorder.eventsOfType(GpuCheckpointType.ISSUED).getLast().checkpointId()
        );

        syncOperations.polledSyncs.clear();
        state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), context, syncOperations);
        assertEquals(List.of(2L), syncOperations.polledSyncs);
        assertEquals(2, recorder.eventsOfType(GpuCheckpointType.OVERFLOW).size());

        syncOperations.statuses.put(2L, GpuCheckpointTracker.GpuSyncStatus.COMPLETED);
        syncOperations.polledSyncs.clear();
        state.checkpoint(recorder, GpuCommandType.TEX_STORAGE_3D.code(), context, syncOperations);
        assertEquals(List.of(2L, 3L), syncOperations.polledSyncs);
        assertEquals(GpuCheckpointTracker.CHECKPOINT_CAPACITY + 2, syncOperations.fenceCount);
        assertEquals(List.of(1L, 2L), syncOperations.deletedSyncs);
    }

    @Test
    void contextCapacityOverflowIsRecordedOnceAcrossKnownContextPolling() {
        GpuCheckpointTracker.State state = new GpuCheckpointTracker.State();
        CapturingCommandRecorder recorder = new CapturingCommandRecorder();
        FakeGpuSyncOperations syncOperations = new FakeGpuSyncOperations();
        Object[] contexts = new Object[GpuCheckpointTracker.CONTEXT_CAPACITY + 2];
        for (int index = 0; index < contexts.length; index++) {
            contexts[index] = new Object();
        }
        for (int index = 0; index < GpuCheckpointTracker.CONTEXT_CAPACITY; index++) {
            state.checkpoint(recorder, GpuCommandType.CLEAR.code(), contexts[index], syncOperations);
        }

        state.checkpoint(recorder, GpuCommandType.BLIT_FRAMEBUFFER.code(), contexts[4], syncOperations);
        state.checkpoint(recorder, GpuCommandType.CLEAR.code(), contexts[0], syncOperations);
        state.checkpoint(recorder, GpuCommandType.GENERATE_MIPMAP.code(), contexts[4], syncOperations);
        state.checkpoint(recorder, GpuCommandType.TEX_STORAGE_2D.code(), contexts[5], syncOperations);

        assertEquals(
            List.of(new CheckpointEvent(
                GpuCheckpointType.OVERFLOW,
                GpuCheckpointTracker.OVERFLOW_CHECKPOINT_ID,
                GpuCommandType.BLIT_FRAMEBUFFER.code()
            )),
            recorder.eventsOfType(GpuCheckpointType.OVERFLOW)
        );
    }

    private static final class CapturingCommandRecorder implements GpuCommandRecorder {
        private final List<CheckpointEvent> checkpoints = new ArrayList<>();
        private final List<String> trace;

        CapturingCommandRecorder() {
            this(new ArrayList<>());
        }

        CapturingCommandRecorder(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public void record(
            GpuCommandType type,
            GpuCommandPhase phase,
            int program,
            int drawFramebuffer,
            int operand0,
            int operand1
        ) {
            throw new AssertionError("Command recording is outside this tracker test");
        }

        @Override
        public void checkpoint(GpuCheckpointType type, long checkpointId, int commandCode) {
            trace.add("checkpoint:" + type.name());
            checkpoints.add(new CheckpointEvent(type, checkpointId, commandCode));
        }

        List<CheckpointEvent> eventsOfType(GpuCheckpointType type) {
            return checkpoints.stream().filter(event -> event.type() == type).toList();
        }

        List<CheckpointEvent> lifecycleEvents() {
            return checkpoints.stream().filter(event -> switch (event.type()) {
                case ISSUED, COMPLETED, OVERFLOW, FAILED -> true;
                default -> false;
            }).toList();
        }
    }

    private static final class FakeGpuSyncOperations implements GpuCheckpointTracker.GpuSyncOperations {
        private final Map<Long, GpuCheckpointTracker.GpuSyncStatus> statuses = new HashMap<>();
        private final List<Long> polledSyncs = new ArrayList<>();
        private final List<Long> deletedSyncs = new ArrayList<>();
        private final List<String> trace;
        private RuntimeException fenceFailure;
        private RuntimeException pollFailure;
        private RuntimeException deleteFailure;
        private boolean returnZeroFence;
        private int fenceCount;

        FakeGpuSyncOperations() {
            this(new ArrayList<>());
        }

        FakeGpuSyncOperations(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public long fence() {
            trace.add("native:fence");
            if (fenceFailure != null) {
                throw fenceFailure;
            }
            if (returnZeroFence) {
                return 0L;
            }
            return ++fenceCount;
        }

        @Override
        public GpuCheckpointTracker.GpuSyncStatus poll(long sync) {
            trace.add("native:wait");
            if (pollFailure != null) {
                throw pollFailure;
            }
            polledSyncs.add(sync);
            return statuses.getOrDefault(sync, GpuCheckpointTracker.GpuSyncStatus.PENDING);
        }

        @Override
        public void delete(long sync) {
            trace.add("native:delete");
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            deletedSyncs.add(sync);
        }
    }

    private record CheckpointEvent(GpuCheckpointType type, long checkpointId, int commandCode) {
    }
}
