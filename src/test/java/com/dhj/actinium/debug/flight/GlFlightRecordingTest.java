package com.dhj.actinium.debug.flight;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlFlightRecordingTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsOnlyOffAndCrashModes() {
        assertEquals(GlFlightRecorderMode.OFF, GlFlightRecorderMode.parse("off"));
        assertEquals(GlFlightRecorderMode.CRASH, GlFlightRecorderMode.parse("crash"));
        assertThrows(IllegalArgumentException.class, () -> GlFlightRecorderMode.parse("detailed"));
    }

    @Test
    void retainsNewestCommittedEventsAfterRingWrap() throws IOException {
        Path path = temporaryDirectory.resolve("wrap.bin");
        try (GlFlightRecorder recorder = GlFlightRecorder.open(path, GlFlightRecorderMode.CRASH, 3)) {
            for (long frame = 1L; frame <= 5L; frame++) {
                recorder.record(
                    GlFlightEventKind.FRAME,
                    GlFlightEventPhase.INSTANT,
                    GlFlightStage.FRAME,
                    frame,
                    frame * 10L,
                    0L,
                    0L,
                    0L
                );
            }
        }

        GlFlightTimeline timeline = GlFlightRecordingReader.read(path);
        assertEquals(List.of(3L, 4L, 5L), timeline.events().stream().map(GlFlightEvent::sequence).toList());
        assertEquals(List.of(3L, 4L, 5L), timeline.events().stream().map(GlFlightEvent::frame).toList());
        assertEquals(List.of(30L, 40L, 50L), timeline.events().stream().map(GlFlightEvent::argument0).toList());
    }

    @Test
    void ignoresSlotWhoseCommitWasInterrupted() throws IOException {
        Path path = temporaryDirectory.resolve("partial.bin");
        try (GlFlightRecorder recorder = GlFlightRecorder.open(path, GlFlightRecorderMode.CRASH, 2)) {
            recordFrame(recorder, 1L);
            recordFrame(recorder, 2L);
        }

        ByteBuffer interruptedCommit = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        interruptedCommit.putLong(-2L).flip();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            while (interruptedCommit.hasRemaining()) {
                channel.write(
                    interruptedCommit,
                    GlFlightFormat.slotOffset(1) + GlFlightFormat.SLOT_COMMIT_SEQUENCE + interruptedCommit.position()
                );
            }
            channel.force(true);
        }

        GlFlightTimeline timeline = GlFlightRecordingReader.read(path);
        assertEquals(1, timeline.events().size());
        assertEquals(1L, timeline.events().getFirst().sequence());
    }

    @Test
    void marksOnlyExplicitNormalCloseAsClean() throws IOException {
        Path path = temporaryDirectory.resolve("close.bin");
        GlFlightRecorder recorder = GlFlightRecorder.open(path, GlFlightRecorderMode.CRASH, 2);
        recordFrame(recorder, 9L);

        assertEquals(path.toAbsolutePath(), recorder.recordingPath().orElseThrow());
        assertFalse(GlFlightRecordingReader.read(path).cleanClose());
        recorder.close();
        assertTrue(GlFlightRecordingReader.read(path).cleanClose());
        assertThrows(IllegalStateException.class, () -> recordFrame(recorder, 10L));
    }

    @Test
    void recoversCommittedEventsAfterChildProcessIsForciblyTerminated() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            Path path = temporaryDirectory.resolve("forced-crash.bin");
            Path argumentFile = createJavaArgumentFile(path);
            Process process = new ProcessBuilder(javaExecutable().toString(), "@" + argumentFile)
                .redirectErrorStream(true)
                .start();

            try (BufferedReader output = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line = CompletableFuture.supplyAsync(() -> readLine(output)).get(20L, TimeUnit.SECONDS);
                assertEquals("READY", line);
                assertTrue(process.destroyForcibly().waitFor(10L, TimeUnit.SECONDS));
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                    process.waitFor(10L, TimeUnit.SECONDS);
                }
            }

            GlFlightTimeline timeline = GlFlightRecordingReader.read(path);
            assertFalse(timeline.cleanClose());
            assertEquals(List.of(1L, 2L), timeline.events().stream().map(GlFlightEvent::sequence).toList());
            assertEquals(List.of(-1L, 77L), timeline.events().stream().map(GlFlightEvent::frame).toList());
            assertEquals(11L, timeline.events().getFirst().argument0());
        });
    }

    private Path createJavaArgumentFile(Path recordingPath) throws IOException {
        String classPath = System.getProperty("java.class.path").replace('\\', '/');
        String arguments = "-classpath\n"
            + quote(classPath) + "\n"
            + GlFlightRecorderCrashChild.class.getName() + "\n"
            + quote(recordingPath.toAbsolutePath().toString().replace('\\', '/')) + "\n";
        Path argumentFile = temporaryDirectory.resolve("child-java.args");
        Files.writeString(argumentFile, arguments, StandardCharsets.UTF_8);
        return argumentFile;
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static String quote(String value) {
        return '"' + value.replace("\"", "\\\"") + '"';
    }

    private static String readLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read crash child output", e);
        }
    }

    private static void recordFrame(GlFlightRecorder recorder, long frame) {
        recorder.record(
            GlFlightEventKind.FRAME,
            GlFlightEventPhase.INSTANT,
            GlFlightStage.FRAME,
            frame,
            0L,
            0L,
            0L,
            0L
        );
    }
}
