package com.gtnewhorizons.angelica.glsm.recording.commands;

import com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PixelDataSnapshotTest {

    @Test
    void nullBuffersCopyToNull() {
        assertNull(PixelDataSnapshot.copy((ByteBuffer) null));
        assertNull(PixelDataSnapshot.copy((IntBuffer) null));
        assertNull(PixelDataSnapshot.copy((FloatBuffer) null));
        assertNull(PixelDataSnapshot.copy((DoubleBuffer) null));
    }

    @Test
    void byteBufferCopyCapturesRemainingBytesAndPreservesPosition() {
        final ByteBuffer src = ByteBuffer.allocate(5);
        src.put(new byte[]{10, 20, 30, 40, 50});
        src.position(1); // snapshot covers [20, 30, 40, 50]

        final ByteBuffer copy = PixelDataSnapshot.copy(src);
        try {
            assertEquals(1, src.position(), "source position must be restored");
            assertEquals(4, copy.remaining());
            assertEquals(0, copy.position());
            for (int i = 0; i < 4; i++) {
                assertEquals(src.get(1 + i), copy.get(i), "byte[" + i + "]");
            }
        } finally {
            MemoryUtilities.memFree(copy);
        }
    }

    @Test
    void intBufferCopyCapturesRemainingInts() {
        final IntBuffer src = IntBuffer.wrap(new int[]{7, 8, 9});
        src.position(1);

        final ByteBuffer copy = PixelDataSnapshot.copy(src);
        try {
            assertEquals(1, src.position(), "source position must be restored");
            final IntBuffer view = copy.asIntBuffer();
            assertEquals(2, view.remaining());
            assertEquals(8, view.get(0));
            assertEquals(9, view.get(1));
        } finally {
            MemoryUtilities.memFree(copy);
        }
    }

    @Test
    void floatBufferCopyCapturesRemainingFloats() {
        final FloatBuffer src = FloatBuffer.wrap(new float[]{0.5f, 1.5f, 2.5f});

        final ByteBuffer copy = PixelDataSnapshot.copy(src);
        try {
            assertEquals(0, src.position(), "source position must be restored");
            final FloatBuffer view = copy.asFloatBuffer();
            assertEquals(3, view.remaining());
            assertEquals(0.5f, view.get(0));
            assertEquals(1.5f, view.get(1));
            assertEquals(2.5f, view.get(2));
        } finally {
            MemoryUtilities.memFree(copy);
        }
    }

    @Test
    void doubleBufferCopyCapturesRemainingDoubles() {
        final DoubleBuffer src = DoubleBuffer.wrap(new double[]{Math.PI, Math.E});

        final ByteBuffer copy = PixelDataSnapshot.copy(src);
        try {
            assertEquals(0, src.position(), "source position must be restored");
            final DoubleBuffer view = copy.asDoubleBuffer();
            assertEquals(2, view.remaining());
            assertEquals(Math.PI, view.get(0));
            assertEquals(Math.E, view.get(1));
        } finally {
            MemoryUtilities.memFree(copy);
        }
    }

    @Test
    void snapshotIsDecoupledFromSourceMutation() {
        final ByteBuffer src = ByteBuffer.allocate(2);
        src.put(new byte[]{1, 2});
        src.flip();

        final ByteBuffer copy = PixelDataSnapshot.copy(src);
        try {
            src.put(0, (byte) 99);
            assertEquals(1, copy.get(0), "snapshot must not see later source writes");
        } finally {
            MemoryUtilities.memFree(copy);
        }
    }
}
