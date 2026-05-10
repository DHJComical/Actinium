package com.mitchej123.lwjgl;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class MemoryStack implements AutoCloseable {
    private MemoryStack() {
    }

    public static MemoryStack stackPush() {
        return new MemoryStack();
    }

    public FloatBuffer mallocFloat(int size) {
        return BufferUtils.createFloatBuffer(size);
    }

    public FloatBuffer callocFloat(int size) {
        return BufferUtils.createFloatBuffer(size);
    }

    public IntBuffer mallocInt(int size) {
        return BufferUtils.createIntBuffer(size);
    }

    public IntBuffer callocInt(int size) {
        return BufferUtils.createIntBuffer(size);
    }

    @Override
    public void close() {
    }
}
