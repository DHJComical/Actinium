package com.dhj.actinium.vertices.views;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public final class ActiniumQuadView {
    private long writePointer;
    private int stride;

    public void setup(long ptr, int stride) {
        this.writePointer = ptr;
        this.stride = stride;
    }

    public float x(int index) {
        return LWJGL.memGetFloat(this.writePointer - this.stride * (3L - index));
    }

    public float y(int index) {
        return LWJGL.memGetFloat(this.writePointer + 4L - this.stride * (3L - index));
    }

    public float z(int index) {
        return LWJGL.memGetFloat(this.writePointer + 8L - this.stride * (3L - index));
    }

    public float u(int index) {
        return LWJGL.memGetFloat(this.writePointer + 16L - this.stride * (3L - index));
    }

    public float v(int index) {
        return LWJGL.memGetFloat(this.writePointer + 20L - this.stride * (3L - index));
    }
}
