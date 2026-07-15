package com.dhj.actinium.debug.flight;

import java.io.IOException;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class MappedGlFlightRecorder implements GlFlightRecorder {
    private final GlFlightRecorderMode mode;
    private final Path path;
    private final int slotCount;
    private final FileChannel channel;
    private final MappedByteBuffer mapped;
    private final AtomicLong nextSequence = new AtomicLong(1L);
    private final AtomicInteger activeWriters = new AtomicInteger();
    private final AtomicBoolean open = new AtomicBoolean(true);

    MappedGlFlightRecorder(Path path, GlFlightRecorderMode mode, int slotCount) throws IOException {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath();
        if (mode == GlFlightRecorderMode.OFF) {
            throw new IllegalArgumentException("Mapped recorder cannot be opened in off mode");
        }
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.slotCount = slotCount;

        long fileSize = GlFlightFormat.fileSize(slotCount);
        if (fileSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Mapped recorder file exceeds the Java buffer size limit");
        }
        channel = FileChannel.open(this.path, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE);
        boolean initialized = false;
        try {
            channel.position(fileSize - 1L);
            channel.write(ByteBuffer.wrap(new byte[1]));
            mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0L, fileSize);
            mapped.order(ByteOrder.LITTLE_ENDIAN);
            initializeHeader();
            mapped.force();
            channel.force(true);
            initialized = true;
        } finally {
            if (!initialized) {
                channel.close();
            }
        }
    }

    @Override
    public GlFlightRecorderMode mode() {
        return mode;
    }

    @Override
    public Optional<Path> recordingPath() {
        return Optional.of(path);
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
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(stage, "stage");
        if (!open.get()) {
            throw new IllegalStateException("GL flight recorder is closed");
        }
        activeWriters.incrementAndGet();
        try {
            if (!open.get()) {
                throw new IllegalStateException("GL flight recorder is closed");
            }
            writeEvent(kind, phase, stage, frame, argument0, argument1, argument2, argument3);
        } finally {
            activeWriters.decrementAndGet();
        }
    }

    private void writeEvent(
        GlFlightEventKind kind,
        GlFlightEventPhase phase,
        GlFlightStage stage,
        long frame,
        long argument0,
        long argument1,
        long argument2,
        long argument3
    ) {
        long sequence = nextSequence.getAndIncrement();
        if (sequence <= 0L) {
            throw new IllegalStateException("GL flight recorder sequence exhausted");
        }
        int slotIndex = (int) ((sequence - 1L) % slotCount);
        int offset = Math.toIntExact(GlFlightFormat.slotOffset(slotIndex));

        mapped.putLong(offset + GlFlightFormat.SLOT_COMMIT_SEQUENCE, -sequence);
        mapped.putLong(offset + GlFlightFormat.SLOT_EPOCH_MILLIS, System.currentTimeMillis());
        mapped.putLong(offset + GlFlightFormat.SLOT_NANOS, System.nanoTime());
        mapped.putLong(offset + GlFlightFormat.SLOT_FRAME, frame);
        mapped.putLong(offset + GlFlightFormat.SLOT_THREAD_ID, Thread.currentThread().threadId());
        mapped.putLong(offset + GlFlightFormat.SLOT_ARGUMENT_0, argument0);
        mapped.putLong(offset + GlFlightFormat.SLOT_ARGUMENT_1, argument1);
        mapped.putLong(offset + GlFlightFormat.SLOT_ARGUMENT_2, argument2);
        mapped.putLong(offset + GlFlightFormat.SLOT_ARGUMENT_3, argument3);
        mapped.putInt(offset + GlFlightFormat.SLOT_KIND, kind.code());
        mapped.putInt(offset + GlFlightFormat.SLOT_PHASE, phase.code());
        mapped.putInt(offset + GlFlightFormat.SLOT_STAGE, stage.code());
        VarHandle.releaseFence();
        mapped.putLong(offset + GlFlightFormat.SLOT_COMMIT_SEQUENCE, sequence);
    }

    @Override
    public void close() throws IOException {
        if (!open.compareAndSet(true, false)) {
            return;
        }

        while (activeWriters.get() != 0) {
            Thread.onSpinWait();
        }
        Exception failure = null;
        try {
            mapped.putInt(GlFlightFormat.HEADER_CLEAN_CLOSE, 1);
            VarHandle.releaseFence();
            mapped.force();
            channel.force(true);
        } catch (IOException | RuntimeException e) {
            failure = e;
        } finally {
            try {
                channel.close();
            } catch (IOException | RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        rethrowCloseFailure(failure);
    }

    private void initializeHeader() {
        mapped.putLong(GlFlightFormat.HEADER_MAGIC, GlFlightFormat.MAGIC);
        mapped.putInt(GlFlightFormat.HEADER_VERSION, GlFlightFormat.VERSION);
        mapped.putInt(GlFlightFormat.HEADER_SIZE_OFFSET, GlFlightFormat.HEADER_SIZE);
        mapped.putInt(GlFlightFormat.HEADER_SLOT_SIZE, GlFlightFormat.SLOT_SIZE);
        mapped.putInt(GlFlightFormat.HEADER_SLOT_COUNT, slotCount);
        mapped.putInt(GlFlightFormat.HEADER_MODE, mode.code());
        mapped.putInt(GlFlightFormat.HEADER_CLEAN_CLOSE, 0);
        mapped.putLong(GlFlightFormat.HEADER_PROCESS_ID, ProcessHandle.current().pid());
        mapped.putLong(GlFlightFormat.HEADER_START_EPOCH_MILLIS, System.currentTimeMillis());
        mapped.putLong(GlFlightFormat.HEADER_START_NANOS, System.nanoTime());
    }

    private static void rethrowCloseFailure(Exception failure) throws IOException {
        if (failure instanceof IOException ioException) {
            throw ioException;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
    }
}
