package com.gtnewhorizon.gtnhlib.bytebuf;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class MemoryStack implements AutoCloseable {
    private MemoryStack() {
    }

    public static MemoryStack stackPush() {
        return new MemoryStack();
    }

    public IntBuffer mallocInt(int size) {
        return BufferUtils.createIntBuffer(size);
    }

    public FloatBuffer mallocFloat(int size) {
        return BufferUtils.createFloatBuffer(size);
    }

    @Override
    public void close() {
    }
}
