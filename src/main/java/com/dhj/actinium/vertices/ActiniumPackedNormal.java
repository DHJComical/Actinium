package com.dhj.actinium.vertices;

import org.joml.Math;

public final class ActiniumPackedNormal {
    private ActiniumPackedNormal() {
    }

    public static int pack(float x, float y, float z, float w) {
        x = Math.clamp(-1.0f, 1.0f, x);
        y = Math.clamp(-1.0f, 1.0f, y);
        z = Math.clamp(-1.0f, 1.0f, z);
        w = Math.clamp(-1.0f, 1.0f, w);

        return ((int) (x * 127.0f) & 0xFF)
                | (((int) (y * 127.0f) & 0xFF) << 8)
                | (((int) (z * 127.0f) & 0xFF) << 16)
                | (((int) (w * 127.0f) & 0xFF) << 24);
    }
}
