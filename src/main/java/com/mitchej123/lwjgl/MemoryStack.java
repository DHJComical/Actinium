package com.mitchej123.lwjgl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public abstract class MemoryStack implements AutoCloseable {

    public static MemoryStack stackPush() {
        return LWJGLServiceProvider.LWJGL.stackPush();
    }

    @Override
    public abstract void close();

    public abstract long getPointer();
    public abstract void setPointer(long pointer);
    public abstract long getAddress();
    public abstract int getSize();

    public abstract ByteBuffer malloc(int size);
    public abstract ShortBuffer mallocShort(int count);
    public abstract IntBuffer mallocInt(int count);
    public abstract LongBuffer mallocLong(int count);
    public abstract FloatBuffer mallocFloat(int count);

    public abstract ByteBuffer calloc(int size);
    public abstract ShortBuffer callocShort(int count);
    public abstract IntBuffer callocInt(int count);
    public abstract LongBuffer callocLong(int count);
    public abstract FloatBuffer callocFloat(int count);

    public IntBuffer ints(int value) {
        IntBuffer buf = mallocInt(1);
        buf.put(0, value);
        return buf;
    }

    public IntBuffer ints(int... values) {
        IntBuffer buf = mallocInt(values.length);
        buf.put(values).flip();
        return buf;
    }

    public FloatBuffer floats(float value) {
        FloatBuffer buf = mallocFloat(1);
        buf.put(0, value);
        return buf;
    }

    public FloatBuffer floats(float... values) {
        FloatBuffer buf = mallocFloat(values.length);
        buf.put(values).flip();
        return buf;
    }

    public abstract long nmalloc(int size);
    public abstract long nmalloc(int alignment, int size);
    public abstract long ncalloc(int alignment, int count, int size);
}
