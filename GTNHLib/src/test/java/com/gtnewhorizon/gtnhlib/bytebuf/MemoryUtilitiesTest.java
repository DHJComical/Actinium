package com.gtnewhorizon.gtnhlib.bytebuf;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.junit.jupiter.api.Test;

class MemoryUtilitiesTest {

    @Test
    void allocCallocReallocAndFreePreserveNativeMemorySemantics() {
        long allocated = MemoryUtilities.nmemAllocChecked(16);
        assertNotEquals(NULL, allocated);

        try {
            MemoryUtilities.memPutLong(allocated, 0x1020_3040_5060_7080L);
            MemoryUtilities.memPutLong(allocated + Long.BYTES, 0x1122_3344_5566_7788L);

            allocated = MemoryUtilities.nmemReallocChecked(allocated, 32);

            assertEquals(0x1020_3040_5060_7080L, MemoryUtilities.memGetLong(allocated));
            assertEquals(0x1122_3344_5566_7788L, MemoryUtilities.memGetLong(allocated + Long.BYTES));
        } finally {
            MemoryUtilities.nmemFree(allocated);
        }

        long zeroed = MemoryUtilities.nmemCallocChecked(8, Integer.BYTES);
        try {
            for (int index = 0; index < 8; index++) {
                assertEquals(0, MemoryUtilities.memGetInt(zeroed + (long) index * Integer.BYTES));
            }
        } finally {
            MemoryUtilities.nmemFree(zeroed);
        }

        long minimumAllocation = MemoryUtilities.nmemAllocChecked(0);
        try {
            assertNotEquals(NULL, minimumAllocation);
        } finally {
            MemoryUtilities.nmemFree(minimumAllocation);
        }
    }

    @Test
    void allocatorAdapterRetainsTheLocalContract() {
        MemoryUtilities.MemoryAllocator allocator = MemoryUtilities.getAllocator();
        assertSame(allocator, MemoryUtilities.getAllocator(true));
        assertThrows(ArithmeticException.class, () -> allocator.calloc(Long.MAX_VALUE, 2));

        long address = allocator.calloc(4, Integer.BYTES);
        try {
            assertNotEquals(NULL, address);
            assertEquals(0, MemoryUtilities.memGetLong(address));
            MemoryUtilities.memPutInt(address, 42);

            address = allocator.realloc(address, 32);

            assertEquals(42, MemoryUtilities.memGetInt(address));
        } finally {
            allocator.free(address);
        }
    }

    @Test
    void primitiveAccessorsRoundTripAtRawAddresses() {
        long address = MemoryUtilities.nmemAllocChecked(64);
        try {
            MemoryUtilities.memPutByte(address, (byte) 0x5A);
            MemoryUtilities.memPutShort(address + 2, (short) 0x6B7C);
            MemoryUtilities.memPutInt(address + 4, 0x1234_5678);
            MemoryUtilities.memPutLong(address + 8, 0x1020_3040_5060_7080L);
            MemoryUtilities.memPutFloat(address + 16, 12.5F);
            MemoryUtilities.memPutDouble(address + 24, -17.25D);
            MemoryUtilities.memPutCLong(address + 40, 0x1234_5678L);
            MemoryUtilities.memPutAddress(address + 48, address);

            assertTrue(MemoryUtilities.memGetBoolean(address));
            assertEquals((byte) 0x5A, MemoryUtilities.memGetByte(address));
            assertEquals((short) 0x6B7C, MemoryUtilities.memGetShort(address + 2));
            assertEquals(0x1234_5678, MemoryUtilities.memGetInt(address + 4));
            assertEquals(0x1020_3040_5060_7080L, MemoryUtilities.memGetLong(address + 8));
            assertEquals(12.5F, MemoryUtilities.memGetFloat(address + 16));
            assertEquals(-17.25D, MemoryUtilities.memGetDouble(address + 24));
            assertEquals(0x1234_5678L, MemoryUtilities.memGetCLong(address + 40));
            assertEquals(address, MemoryUtilities.memGetAddress(address + 48));

            MemoryUtilities.memPutByte(address, (byte) 0);
            assertFalse(MemoryUtilities.memGetBoolean(address));
        } finally {
            MemoryUtilities.nmemFree(address);
        }
    }

    @Test
    void setAndCopyOperateOnTheRequestedRanges() {
        long source = MemoryUtilities.nmemAllocChecked(33);
        long destination = MemoryUtilities.nmemAllocChecked(33);
        try {
            MemoryUtilities.memSet(source, 0xAB, 33);
            MemoryUtilities.memSet(destination, 0, 33);
            MemoryUtilities.memCopy(source, destination, 33);

            for (int index = 0; index < 33; index++) {
                assertEquals((byte) 0xAB, MemoryUtilities.memGetByte(destination + index));
            }
        } finally {
            MemoryUtilities.nmemFree(destination);
            MemoryUtilities.nmemFree(source);
        }
    }

    @Test
    void bufferWrappingAddressingDuplicationAndSlicingShareMemory() {
        ByteBuffer buffer = MemoryUtilities.memAlloc(32).order(ByteOrder.BIG_ENDIAN);
        try {
            long baseAddress = MemoryUtilities.memAddress0(buffer);
            assertNotEquals(NULL, baseAddress);

            ByteBuffer wrapped = MemoryUtilities.memByteBuffer(baseAddress, buffer.capacity());
            assertEquals(baseAddress, MemoryUtilities.memAddress0(wrapped));
            wrapped.putInt(0, 0x1234_5678);
            assertEquals(0x1234_5678, MemoryUtilities.memGetInt(baseAddress));

            buffer.position(4).limit(24);
            ByteBuffer duplicate = MemoryUtilities.memDuplicate(buffer);
            assertEquals(4, duplicate.position());
            assertEquals(24, duplicate.limit());
            assertEquals(ByteOrder.BIG_ENDIAN, duplicate.order());
            assertEquals(baseAddress, MemoryUtilities.memAddress0(duplicate));

            ByteBuffer slice = MemoryUtilities.memSlice(buffer, 3, 8);
            assertEquals(0, slice.position());
            assertEquals(8, slice.capacity());
            assertEquals(baseAddress + 7, MemoryUtilities.memAddress0(slice));
            slice.put(0, (byte) 0x6D);
            assertEquals((byte) 0x6D, MemoryUtilities.memGetByte(baseAddress + 7));

            IntBuffer integers = MemoryUtilities.memIntBuffer(baseAddress, 8);
            assertEquals(baseAddress, MemoryUtilities.memAddress0(integers));
            integers.put(2, 0x0A0B_0C0D);
            assertEquals(0x0A0B_0C0D, MemoryUtilities.memGetInt(baseAddress + 2L * Integer.BYTES));
        } finally {
            MemoryUtilities.memFree(buffer);
        }
    }

    @Test
    void asciiUtf8AndUtf16RoundTripWithoutIntermediateState() {
        ByteBuffer ascii = MemoryUtilities.memASCII("Actinium", true);
        ByteBuffer utf8 = MemoryUtilities.memUTF8("A\u4E2D\uD83D\uDE80", true);
        ByteBuffer utf16 = MemoryUtilities.memUTF16("B\u6587\uD83D\uDE80", true);
        try {
            assertEquals("Actinium", MemoryUtilities.memASCII(MemoryUtilities.memAddress(ascii)));
            assertEquals("A\u4E2D\uD83D\uDE80", MemoryUtilities.memUTF8(MemoryUtilities.memAddress(utf8)));
            assertEquals("B\u6587\uD83D\uDE80", MemoryUtilities.memUTF16(MemoryUtilities.memAddress(utf16)));
        } finally {
            MemoryUtilities.memFree(utf16);
            MemoryUtilities.memFree(utf8);
            MemoryUtilities.memFree(ascii);
        }
    }

    @Test
    void nullSafeAndRangeCheckedApisFailFast() {
        assertNull(MemoryUtilities.memByteBufferSafe(NULL, 8));
        assertEquals(NULL, MemoryUtilities.memAddressSafe((ByteBuffer) null));
        MemoryUtilities.memFree((ByteBuffer) null);

        ByteBuffer buffer = MemoryUtilities.memAlloc(8);
        ByteBuffer tooSmall = MemoryUtilities.memAlloc(2);
        try {
            buffer.position(2).limit(6);
            assertThrows(IllegalArgumentException.class, () -> MemoryUtilities.memSlice(buffer, -1, 1));
            assertThrows(IllegalArgumentException.class, () -> MemoryUtilities.memSlice(buffer, 5, 1));
            assertThrows(IllegalArgumentException.class, () -> MemoryUtilities.memSlice(buffer, 0, 7));
            assertThrows(
                    BufferOverflowException.class,
                    () -> MemoryUtilities.memASCII("overflow", false, tooSmall));
        } finally {
            MemoryUtilities.memFree(tooSmall);
            MemoryUtilities.memFree(buffer);
        }
    }
}
