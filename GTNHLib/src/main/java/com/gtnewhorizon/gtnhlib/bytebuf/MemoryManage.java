/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.MemoryAllocator;

import org.lwjgl.system.MemoryUtil;

/** Provides {@link MemoryAllocator} implementations for {@link MemoryUtilities} to use. */
final class MemoryManage {

    private MemoryManage() {}

    static MemoryAllocator getInstance() {
        return new LwjglAllocator();
    }

    /** Adapts LWJGL's maintained native allocator to the local public allocator contract. */
    private static final class LwjglAllocator implements MemoryAllocator {

        @Override
        public long malloc(long size) {
            return MemoryUtil.nmemAlloc(size);
        }

        @Override
        public long calloc(long num, long size) {
            return MemoryUtil.nmemCalloc(1L, Math.multiplyExact(num, size));
        }

        @Override
        public long realloc(long ptr, long size) {
            return MemoryUtil.nmemRealloc(ptr, size);
        }

        @Override
        public void free(long ptr) {
            MemoryUtil.nmemFree(ptr);
        }
    }

}
