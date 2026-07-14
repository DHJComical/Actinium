/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import org.lwjgl.system.MemoryUtil;

final class MultiReleaseMemCopy {

    private MultiReleaseMemCopy() {}

    public static int classVersion() {
        return Runtime.version().feature() >= 17 ? 17 : 8;
    }

    static void copy(long src, long dst, long bytes) {
        MemoryUtil.memCopy(src, dst, bytes);
    }

}
