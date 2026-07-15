package com.dhj.actinium.debug.flight;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlFlightIntegrationLogicTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void mapsStableStagePrefixesWithoutLosingTheOriginalLabelHash() throws IOException {
        CapturingRecorder recorder = new CapturingRecorder();
        GlFlightRecordingSession session = new GlFlightRecordingSession(recorder);
        session.beginFrame();

        session.markStage("composite:draw");
        session.markStage("render-world-pass:0:after-sky");
        session.markStage("mixin:shadows:entry");
        session.markStage("minecraft:before-update-display");
        session.markStage("unclassified:value");

        List<CapturedEvent> stages = recorder.eventsOfKind(GlFlightEventKind.RENDER_STAGE);
        assertEquals(
            List.of(
                GlFlightStage.COMPOSITE,
                GlFlightStage.WORLD_RENDER,
                GlFlightStage.SHADOW_RENDER,
                GlFlightStage.SWAP_BUFFERS,
                GlFlightStage.UNKNOWN
            ),
            stages.stream().map(CapturedEvent::stage).toList()
        );
        assertEquals(GlFlightHash.stableHash("composite:draw"), stages.getFirst().argument0());
        assertEquals(0xCEC64E155111225DL, GlFlightHash.stableHash("abc"));
        session.endFrame();
        session.close();
    }

    @Test
    void recordsPairedFrameSwapAndProcessLifecycle() throws IOException {
        CapturingRecorder recorder = new CapturingRecorder();
        GlFlightRecordingSession session = new GlFlightRecordingSession(recorder);

        assertEquals(0L, session.beginFrame());
        session.beginSwap();
        session.endSwap();
        session.endFrame();
        session.close();
        session.close();

        assertEquals(
            List.of(
                GlFlightEventKind.LIFECYCLE,
                GlFlightEventKind.FRAME,
                GlFlightEventKind.SWAP,
                GlFlightEventKind.SWAP,
                GlFlightEventKind.FRAME,
                GlFlightEventKind.LIFECYCLE
            ),
            recorder.events.stream().map(CapturedEvent::kind).toList()
        );
        assertEquals(
            List.of(
                GlFlightEventPhase.BEGIN,
                GlFlightEventPhase.BEGIN,
                GlFlightEventPhase.BEGIN,
                GlFlightEventPhase.END,
                GlFlightEventPhase.END,
                GlFlightEventPhase.END
            ),
            recorder.events.stream().map(CapturedEvent::phase).toList()
        );
        assertEquals(List.of(-1L, 0L, 0L, 0L, 0L, -1L), recorder.events.stream().map(CapturedEvent::frame).toList());
        assertEquals(1, recorder.closeCount);
        assertEquals(1L, recorder.events.stream()
            .filter(event -> event.kind() == GlFlightEventKind.LIFECYCLE)
            .filter(event -> event.phase() == GlFlightEventPhase.END)
            .count());
    }

    @Test
    void closeStillClosesRecorderAndPreservesBothFailures() {
        CapturingRecorder recorder = new CapturingRecorder();
        GlFlightRecordingSession session = new GlFlightRecordingSession(recorder);
        RuntimeException recordingFailure = new IllegalStateException("lifecycle end failed");
        IOException closeFailure = new IOException("recorder close failed");
        recorder.lifecycleEndFailure = recordingFailure;
        recorder.closeFailure = closeFailure;

        RuntimeException thrown = assertThrows(RuntimeException.class, session::close);

        assertSame(recordingFailure, thrown);
        assertEquals(1, recorder.closeCount);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(closeFailure, thrown.getSuppressed()[0]);
        assertDoesNotThrow(session::close);
        assertEquals(1, recorder.closeCount);
    }

    @Test
    void offModeCreatesNeitherSessionNorFile() throws IOException {
        Path path = temporaryDirectory.resolve("disabled.bin");
        GlFlightRecorder recorder = GlFlightRecorder.open(path, GlFlightRecorderMode.OFF, 4);

        assertNull(GlFlightRecordingSession.create(recorder));
        assertFalse(Files.exists(path));
        assertTrue(recorder.recordingPath().isEmpty());
    }

    @Test
    void recordsPipelineEventsWithStableDimensionHashes() throws IOException {
        CapturingRecorder recorder = new CapturingRecorder();
        GlFlightRecordingSession session = new GlFlightRecordingSession(recorder);
        session.beginFrame();

        session.dimensionChange("overworld", "the_end");
        session.beginPipelineDestroy("overworld");
        session.endPipelineDestroy("overworld");
        session.beginPipelineCreate("the_end");
        session.endPipelineCreate("the_end");

        List<CapturedEvent> events = recorder.eventsOfKind(GlFlightEventKind.PIPELINE);
        assertEquals(5, events.size());
        CapturedEvent dimensionChange = events.getFirst();
        assertEquals(GlFlightEventPhase.INSTANT, dimensionChange.phase());
        assertEquals(GlFlightPipelineAction.DIMENSION_CHANGE.code(), dimensionChange.argument0());
        assertEquals(GlFlightHash.stableHash("overworld"), dimensionChange.argument1());
        assertEquals(GlFlightHash.stableHash("the_end"), dimensionChange.argument2());
        assertPipelinePair(events.subList(1, 3), GlFlightPipelineAction.DESTROY, "overworld");
        assertPipelinePair(events.subList(3, 5), GlFlightPipelineAction.CREATE, "the_end");
        session.endFrame();
        session.close();
    }

    @Test
    void decoderPreservesEveryLongBitAcrossJsonParsing() throws IOException {
        long[] arguments = {
            Long.MIN_VALUE,
            -1L,
            GlFlightHash.stableHash("abc"),
            0x0123456789ABCDEFL
        };
        GlFlightEvent event = new GlFlightEvent(
            Long.MAX_VALUE,
            Long.MIN_VALUE + 1L,
            Long.MAX_VALUE - 1L,
            -1L,
            Long.MAX_VALUE - 2L,
            GlFlightEventKind.RENDER_STAGE,
            GlFlightEventPhase.INSTANT,
            GlFlightStage.COMPOSITE,
            arguments[0],
            arguments[1],
            arguments[2],
            arguments[3]
        );
        GlFlightTimeline timeline = new GlFlightTimeline(
            Path.of("diagnostics", "flight.bin"),
            1,
            GlFlightRecorderMode.CRASH,
            false,
            Long.MAX_VALUE - 3L,
            Long.MIN_VALUE + 2L,
            Long.MAX_VALUE - 4L,
            List.of(event)
        );
        StringWriter output = new StringWriter();

        GlFlightRecordingDecoder.writeJson(timeline, output);

        JsonObject root = JsonParser.parseReader(new StringReader(output.toString())).getAsJsonObject();
        assertEquals("twos-complement-hex-64", root.get("argumentEncoding").getAsString());
        assertDecimalString(root, "processId", timeline.processId());
        assertDecimalString(root, "startEpochMillis", timeline.startEpochMillis());
        assertDecimalString(root, "startMonotonicNanos", timeline.startMonotonicNanos());

        JsonObject decodedEvent = root.getAsJsonArray("events").get(0).getAsJsonObject();
        assertDecimalString(decodedEvent, "sequence", event.sequence());
        assertDecimalString(decodedEvent, "epochMillis", event.epochMillis());
        assertDecimalString(decodedEvent, "monotonicNanos", event.monotonicNanos());
        assertDecimalString(decodedEvent, "frame", event.frame());
        assertDecimalString(decodedEvent, "threadId", event.threadId());

        JsonArray decodedArguments = decodedEvent.getAsJsonArray("arguments");
        List<String> decodedHexadecimal = new ArrayList<>(decodedArguments.size());
        for (int index = 0; index < decodedArguments.size(); index++) {
            decodedHexadecimal.add(decodedArguments.get(index).getAsString());
        }
        assertEquals(List.of(
            "8000000000000000",
            "ffffffffffffffff",
            "cec64e155111225d",
            "0123456789abcdef"
        ), decodedHexadecimal);
        for (int index = 0; index < arguments.length; index++) {
            assertTrue(decodedArguments.get(index).getAsJsonPrimitive().isString());
            String hexadecimal = decodedArguments.get(index).getAsString();
            assertEquals(16, hexadecimal.length());
            assertEquals(arguments[index], Long.parseUnsignedLong(hexadecimal, 16));
        }
    }

    private static void assertDecimalString(JsonObject object, String field, long expected) {
        assertTrue(object.get(field).getAsJsonPrimitive().isString());
        assertEquals(expected, Long.parseLong(object.get(field).getAsString()));
    }

    private static void assertPipelinePair(
        List<CapturedEvent> events,
        GlFlightPipelineAction action,
        String dimension
    ) {
        assertEquals(List.of(GlFlightEventPhase.BEGIN, GlFlightEventPhase.END), events.stream().map(CapturedEvent::phase).toList());
        assertEquals(List.of((long) action.code(), (long) action.code()), events.stream().map(CapturedEvent::argument0).toList());
        long dimensionHash = GlFlightHash.stableHash(dimension);
        assertEquals(List.of(dimensionHash, dimensionHash), events.stream().map(CapturedEvent::argument1).toList());
    }

    private static final class CapturingRecorder implements GlFlightRecorder {
        private final List<CapturedEvent> events = new ArrayList<>();
        private int closeCount;
        private RuntimeException lifecycleEndFailure;
        private Exception closeFailure;

        @Override
        public GlFlightRecorderMode mode() {
            return GlFlightRecorderMode.CRASH;
        }

        @Override
        public Optional<Path> recordingPath() {
            return Optional.of(Path.of("capture.bin"));
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
            if (kind == GlFlightEventKind.LIFECYCLE
                && phase == GlFlightEventPhase.END
                && lifecycleEndFailure != null) {
                throw lifecycleEndFailure;
            }
            events.add(new CapturedEvent(kind, phase, stage, frame, argument0, argument1, argument2, argument3));
        }

        @Override
        public void close() throws IOException {
            closeCount++;
            if (closeFailure instanceof IOException ioException) {
                throw ioException;
            }
            if (closeFailure instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
        }

        List<CapturedEvent> eventsOfKind(GlFlightEventKind kind) {
            return events.stream().filter(event -> event.kind() == kind).toList();
        }
    }

    private record CapturedEvent(
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
}
