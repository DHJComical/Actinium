package com.gtnewhorizon.gtnhlib.bytebuf;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

/**
 * String decoding utilities.
 *
 * <p>
 * On Java 9 different implementations are used that work better with compat strings (JEP 254).
 * </p>
 */
final class MultiReleaseTextDecoding {

    private MultiReleaseTextDecoding() {}

    public static int classVersion() {
        return Runtime.version().feature() >= 17 ? 17 : 8;
    }

    /** @see MemoryUtilities#memUTF8(ByteBuffer, int, int) */
    static String decodeUTF8(long source, int length) {
        return MemoryUtil.memUTF8(source, length);
    }
}
