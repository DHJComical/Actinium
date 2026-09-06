package com.dhj.actinium.render.vertex;

import sun.misc.Unsafe;

/**
 * Writer for FLOAT typed elements: every component is stored as a 32-bit float.
 */
final class FloatVertexWriter implements VertexWriter {
    /** Single pre-built instance handed out by the writer factory. */
    static final VertexWriter INSTANCE = new FloatVertexWriter();

    private static final Unsafe UNSAFE = DirectBufferAddress.UNSAFE;

    private FloatVertexWriter() {
    }

    @Override
    public void writePosition(long target, double x, double y, double z,
                               double xOffset, double yOffset, double zOffset) {
        UNSAFE.putFloat(target, (float) (x + xOffset));
        UNSAFE.putFloat(target + 4, (float) (y + yOffset));
        UNSAFE.putFloat(target + 8, (float) (z + zOffset));
    }

    @Override
    public void writeColor(long target, int red, int green, int blue, int alpha) {
        UNSAFE.putFloat(target, red / 255.0F);
        UNSAFE.putFloat(target + 4, green / 255.0F);
        UNSAFE.putFloat(target + 8, blue / 255.0F);
        UNSAFE.putFloat(target + 12, alpha / 255.0F);
    }

    @Override
    public void writeTexCoord(long target, double u, double v) {
        UNSAFE.putFloat(target, (float) u);
        UNSAFE.putFloat(target + 4, (float) v);
    }

    @Override
    public void writeLightmap(long target, int skyLight, int blockLight) {
        UNSAFE.putFloat(target, (float) skyLight);
        UNSAFE.putFloat(target + 4, (float) blockLight);
    }

    @Override
    public void writeNormal(long target, float x, float y, float z) {
        UNSAFE.putFloat(target, x);
        UNSAFE.putFloat(target + 4, y);
        UNSAFE.putFloat(target + 8, z);
    }
}
