package com.dhj.actinium.vertices;

public final class ActiniumExtendedDataHelper {
    public static final short BLOCK_RENDER_TYPE = 0;
    public static final short FLUID_RENDER_TYPE = 1;

    private ActiniumExtendedDataHelper() {
    }

    public static int packMidBlock(float x, float y, float z) {
        return ((int) (x * 64.0f) & 0xFF)
                | (((int) (y * 64.0f) & 0xFF) << 8)
                | (((int) (z * 64.0f) & 0xFF) << 16);
    }

    public static int computeMidBlock(float x, float y, float z, int localPosX, int localPosY, int localPosZ) {
        return packMidBlock(
                localPosX + 0.5f - x,
                localPosY + 0.5f - y,
                localPosZ + 0.5f - z
        );
    }
}
