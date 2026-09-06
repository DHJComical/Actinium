package com.dhj.actinium.render.vertex;

import com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteVertexWriterTest {
    private static final int RED = 0x11;
    private static final int GREEN = 0x22;
    private static final int BLUE = 0x33;
    private static final int ALPHA = 0x44;

    private static ByteBuffer writeColor(boolean littleEndian) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        ByteVertexWriter.putColorBytes(MemoryUtilities.memAddress0(buffer),
            RED, GREEN, BLUE, ALPHA, littleEndian);
        return buffer;
    }

    @Test
    void littleEndianStoresRgbaByteOrder() {
        ByteBuffer buffer = writeColor(true);

        assertEquals((byte) RED, buffer.get(0));
        assertEquals((byte) GREEN, buffer.get(1));
        assertEquals((byte) BLUE, buffer.get(2));
        assertEquals((byte) ALPHA, buffer.get(3));
    }

    @Test
    void bigEndianStoresArgbByteOrder() {
        ByteBuffer buffer = writeColor(false);

        assertEquals((byte) ALPHA, buffer.get(0));
        assertEquals((byte) BLUE, buffer.get(1));
        assertEquals((byte) GREEN, buffer.get(2));
        assertEquals((byte) RED, buffer.get(3));
    }

    /**
     * The two platform layouts exist so that a native-order packed int read yields the
     * same value on either host; verify that equivalence with explicit-order reads.
     */
    @Test
    void packedIntIsIdenticalUnderEitherOrdering() {
        int expected = ALPHA << 24 | BLUE << 16 | GREEN << 8 | RED;

        assertEquals(expected, writeColor(true).order(ByteOrder.LITTLE_ENDIAN).getInt(0));
        assertEquals(expected, writeColor(false).order(ByteOrder.BIG_ENDIAN).getInt(0));
    }
}
