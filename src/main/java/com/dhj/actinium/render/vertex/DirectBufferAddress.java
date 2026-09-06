package com.dhj.actinium.render.vertex;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Resolves the native memory address of direct {@link ByteBuffer} instances.
 *
 * <p>Motivation: the vertex write hot path performs absolute stores straight into the
 * staging buffer of {@code BufferBuilder} to avoid the per-call index and bounds work of
 * {@code ByteBuffer.putXxx}. The {@code java.nio.Buffer#address} field carries that address
 * but has no public accessor; the FFM {@code MemorySegment} view of an existing buffer is
 * unavailable under {@code --release 21}. Reading the field offset through
 * {@code sun.misc.Unsafe#objectFieldOffset} is therefore the only entry point. The single
 * reflective lookup of {@code sun.misc.Unsafe#theUnsafe} is part of that same grant: there
 * is no non-reflective way to obtain the {@code Unsafe} instance from application code.
 */
public final class DirectBufferAddress {
    /**
     * Shared {@code Unsafe} instance for the vertex package. All raw stores of the
     * vertex writers route through this holder so {@code sun.misc.Unsafe} is referenced
     * from exactly one place.
     */
    static final Unsafe UNSAFE;
    private static final long ADDRESS_FIELD_OFFSET;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            Field address = Buffer.class.getDeclaredField("address");
            ADDRESS_FIELD_OFFSET = UNSAFE.objectFieldOffset(address);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError("java.nio.Buffer#address is not reachable: " + e);
        }
    }

    private DirectBufferAddress() {
    }

    /**
     * Returns the native address backing the given direct buffer, or fails fast when the
     * buffer is not direct or its address has not been assigned yet.
     *
     * @param buffer direct buffer whose native address is requested
     * @return native base address of the buffer contents
     */
    public static long of(ByteBuffer buffer) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("Vertex staging buffer must be direct");
        }
        long address = UNSAFE.getLong(buffer, ADDRESS_FIELD_OFFSET);
        if (address == 0) {
            throw new IllegalStateException("Direct vertex staging buffer has no native address");
        }
        return address;
    }
}
