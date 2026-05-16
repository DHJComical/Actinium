/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Simple index checks.
 *
 * <p>
 * On Java 9 these checks are replaced with the corresponding {@link java.util.Objects} methods, which perform better.
 * </p>
 */
public final class CheckIntrinsics {

    private CheckIntrinsics() {}

    public static int classVersion() {
        return Runtime.version().feature() >= 17 ? 17 : 8;
    }

    public static int checkIndex(int index, int length) {
        if (Runtime.version().feature() >= 9) {
            return Objects.checkIndex(index, length);
        }
        if (index < 0 || length <= index) {
            throw new IndexOutOfBoundsException();
        }
        return index;
    }

    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (Runtime.version().feature() >= 9) {
            return Objects.checkFromToIndex(fromIndex, toIndex, length);
        }
        if (fromIndex < 0 || toIndex < fromIndex || length < toIndex) {
            throw new IndexOutOfBoundsException();
        }
        return fromIndex;
    }

    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (Runtime.version().feature() >= 9) {
            return Objects.checkFromIndexSize(fromIndex, size, length);
        }
        if ((length | fromIndex | size) < 0 || length - fromIndex < size) {
            throw new IndexOutOfBoundsException();
        }
        return fromIndex;
    }

    public static ByteBuffer NewDirectByteBuffer(long address, int capacity) {
        if (Runtime.version().feature() >= 17) {
            return org.lwjgl.system.jni.JNINativeInterface.NewDirectByteBuffer(address, capacity);
        }
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ByteBuffer> dbb = (Class<? extends ByteBuffer>) Class.forName("java.nio.DirectByteBuffer");
            final var newDbb = dbb.getDeclaredConstructor(long.class, int.class);
            newDbb.setAccessible(true);
            return newDbb.newInstance(address, capacity);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemoryUtilities.MemoryAllocator getLwjgl3ifyAllocator() {
        if (Runtime.version().feature() < 17) {
            return null;
        }
        return new Lwjgl3ifyAllocator();
    }

    private static final class Lwjgl3ifyAllocator implements MemoryUtilities.MemoryAllocator {
        @Override
        public long malloc(long size) {
            return org.lwjgl.system.MemoryUtil.nmemAlloc(size);
        }

        @Override
        public long calloc(long num, long size) {
            return org.lwjgl.system.MemoryUtil.nmemCalloc(num, size);
        }

        @Override
        public long realloc(long ptr, long size) {
            return org.lwjgl.system.MemoryUtil.nmemRealloc(ptr, size);
        }

        @Override
        public void free(long ptr) {
            org.lwjgl.system.MemoryUtil.nmemFree(ptr);
        }
    }

}
