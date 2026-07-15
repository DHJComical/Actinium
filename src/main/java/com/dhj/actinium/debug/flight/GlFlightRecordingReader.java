package com.dhj.actinium.debug.flight;

import java.io.EOFException;
import java.io.IOException;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads a flight recorder file into a structured timeline without requiring the game runtime.
 */
public final class GlFlightRecordingReader {
    private GlFlightRecordingReader() {
    }

    /**
     * Reads valid committed slots, ignores interrupted writes, and orders wrapped events by sequence.
     *
     * @param path flight recording file
     * @return validated recording metadata and ordered events
     * @throws IOException when the file is truncated, unreadable, or has an unsupported committed value
     */
    public static GlFlightTimeline read(Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath();
        try (FileChannel channel = FileChannel.open(absolutePath, StandardOpenOption.READ)) {
            ByteBuffer header = allocate(GlFlightFormat.HEADER_SIZE);
            readFully(channel, header, 0L);
            validateHeader(header);

            int slotCount = header.getInt(GlFlightFormat.HEADER_SLOT_COUNT);
            long expectedSize = GlFlightFormat.fileSize(slotCount);
            if (expectedSize > Integer.MAX_VALUE) {
                throw new IOException("GL flight recording exceeds the supported mapped format size");
            }
            if (channel.size() < expectedSize) {
                throw new EOFException("Truncated GL flight recording: expected " + expectedSize + " bytes, found " + channel.size());
            }

            List<GlFlightEvent> events = readEvents(channel, slotCount);
            events.sort(Comparator.comparingLong(GlFlightEvent::sequence));
            return new GlFlightTimeline(
                absolutePath,
                header.getInt(GlFlightFormat.HEADER_VERSION),
                readMode(header),
                header.getInt(GlFlightFormat.HEADER_CLEAN_CLOSE) == 1,
                header.getLong(GlFlightFormat.HEADER_PROCESS_ID),
                header.getLong(GlFlightFormat.HEADER_START_EPOCH_MILLIS),
                header.getLong(GlFlightFormat.HEADER_START_NANOS),
                events
            );
        }
    }

    private static List<GlFlightEvent> readEvents(FileChannel channel, int slotCount) throws IOException {
        List<GlFlightEvent> events = new ArrayList<>(slotCount);
        ByteBuffer slot = allocate(GlFlightFormat.SLOT_SIZE);
        ByteBuffer commit = allocate(Long.BYTES);
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            long slotOffset = GlFlightFormat.slotOffset(slotIndex);
            long firstCommit = readCommit(channel, commit, slotOffset);
            if (firstCommit <= 0L) {
                continue;
            }
            VarHandle.acquireFence();
            readFully(channel, slot, slotOffset);
            if (slot.getLong(GlFlightFormat.SLOT_COMMIT_SEQUENCE) != firstCommit) {
                continue;
            }
            GlFlightEvent event = readEvent(slot, firstCommit);
            VarHandle.acquireFence();
            long secondCommit = readCommit(channel, commit, slotOffset);
            if (firstCommit == secondCommit) {
                events.add(event);
            }
        }
        return events;
    }

    private static long readCommit(FileChannel channel, ByteBuffer commit, long slotOffset) throws IOException {
        readFully(channel, commit, slotOffset + GlFlightFormat.SLOT_COMMIT_SEQUENCE);
        return commit.getLong();
    }

    private static GlFlightEvent readEvent(ByteBuffer slot, long sequence) throws IOException {
        try {
            return new GlFlightEvent(
                sequence,
                slot.getLong(GlFlightFormat.SLOT_EPOCH_MILLIS),
                slot.getLong(GlFlightFormat.SLOT_NANOS),
                slot.getLong(GlFlightFormat.SLOT_FRAME),
                slot.getLong(GlFlightFormat.SLOT_THREAD_ID),
                GlFlightEventKind.fromCode(slot.getInt(GlFlightFormat.SLOT_KIND)),
                GlFlightEventPhase.fromCode(slot.getInt(GlFlightFormat.SLOT_PHASE)),
                GlFlightStage.fromCode(slot.getInt(GlFlightFormat.SLOT_STAGE)),
                slot.getLong(GlFlightFormat.SLOT_ARGUMENT_0),
                slot.getLong(GlFlightFormat.SLOT_ARGUMENT_1),
                slot.getLong(GlFlightFormat.SLOT_ARGUMENT_2),
                slot.getLong(GlFlightFormat.SLOT_ARGUMENT_3)
            );
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid committed GL flight event at sequence " + sequence, e);
        }
    }

    private static void validateHeader(ByteBuffer header) throws IOException {
        if (header.getLong(GlFlightFormat.HEADER_MAGIC) != GlFlightFormat.MAGIC) {
            throw new IOException("Not an Actinium GL flight recording");
        }
        int version = header.getInt(GlFlightFormat.HEADER_VERSION);
        if (version != GlFlightFormat.VERSION) {
            throw new IOException("Unsupported GL flight recording version: " + version);
        }
        if (header.getInt(GlFlightFormat.HEADER_SIZE_OFFSET) != GlFlightFormat.HEADER_SIZE
            || header.getInt(GlFlightFormat.HEADER_SLOT_SIZE) != GlFlightFormat.SLOT_SIZE) {
            throw new IOException("Invalid GL flight recording layout");
        }
        if (header.getInt(GlFlightFormat.HEADER_SLOT_COUNT) <= 0) {
            throw new IOException("Invalid GL flight recording slot count");
        }
        int cleanClose = header.getInt(GlFlightFormat.HEADER_CLEAN_CLOSE);
        if (cleanClose != 0 && cleanClose != 1) {
            throw new IOException("Invalid GL flight recording close state: " + cleanClose);
        }
        readMode(header);
    }

    private static GlFlightRecorderMode readMode(ByteBuffer header) throws IOException {
        try {
            GlFlightRecorderMode mode = GlFlightRecorderMode.fromCode(header.getInt(GlFlightFormat.HEADER_MODE));
            if (mode == GlFlightRecorderMode.OFF) {
                throw new IllegalArgumentException("Mapped recording cannot have off mode");
            }
            return mode;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid GL flight recording mode", e);
        }
    }

    private static ByteBuffer allocate(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        buffer.clear();
        while (buffer.hasRemaining()) {
            int count = channel.read(buffer, position + buffer.position());
            if (count < 0) {
                throw new EOFException("Unexpected end of GL flight recording");
            }
        }
        buffer.flip();
    }
}
