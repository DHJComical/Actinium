package net.coderbot.iris.celeritas.vertices;

import net.coderbot.iris.vertices.IrisQuadView;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

final class LwjglQuadView implements IrisQuadView {
    private long writePointer;
    private int stride;

    void setup(long ptr, int stride) {
        this.writePointer = ptr;
        this.stride = stride;
    }

    @Override
    public float x(int index) {
        return LWJGL.memGetFloat(this.writePointer - this.stride * (3L - index));
    }

    @Override
    public float y(int index) {
        return LWJGL.memGetFloat(this.writePointer + 4L - this.stride * (3L - index));
    }

    @Override
    public float z(int index) {
        return LWJGL.memGetFloat(this.writePointer + 8L - this.stride * (3L - index));
    }

    @Override
    public float u(int index) {
        return LWJGL.memGetFloat(this.writePointer + 16L - this.stride * (3L - index));
    }

    @Override
    public float v(int index) {
        return LWJGL.memGetFloat(this.writePointer + 20L - this.stride * (3L - index));
    }
}

