package com.dhj.actinium.render.vertex;

import sun.misc.Unsafe;

/**
 * Writer for SHORT and USHORT typed elements: every component is stored as a 16-bit
 * value.
 */
final class ShortVertexWriter implements VertexWriter {
    /** Single pre-built instance handed out by the writer factory. */
    static final VertexWriter INSTANCE = new ShortVertexWriter();

    private static final Unsafe UNSAFE = DirectBufferAddress.UNSAFE;

    private ShortVertexWriter() {
    }

    @Override
    public void writePosition(long target, double x, double y, double z,
                               double xOffset, double yOffset, double zOffset) {
        UNSAFE.putShort(target, (short) (x + xOffset));
        UNSAFE.putShort(target + 2, (short) (y + yOffset));
        UNSAFE.putShort(target + 4, (short) (z + zOffset));
    }

    @Override
    public void writeColor(long target, int red, int green, int blue, int alpha) {
        UNSAFE.putShort(target, (short) red);
        UNSAFE.putShort(target + 2, (short) green);
        UNSAFE.putShort(target + 4, (short) blue);
        UNSAFE.putShort(target + 6, (short) alpha);
    }

    @Override
    public void writeTexCoord(long target, double u, double v) {
        UNSAFE.putShort(target, (short) v);
        UNSAFE.putShort(target + 2, (short) u);
    }

    @Override
    public void writeLightmap(long target, int skyLight, int blockLight) {
        UNSAFE.putShort(target, (short) blockLight);
        UNSAFE.putShort(target + 2, (short) skyLight);
    }

    @Override
    public void writeNormal(long target, float x, float y, float z) {
        UNSAFE.putShort(target, (short) ((int) (x * 32767) & 65535));
        UNSAFE.putShort(target + 2, (short) ((int) (y * 32767) & 65535));
        UNSAFE.putShort(target + 4, (short) ((int) (z * 32767) & 65535));
    }
}
