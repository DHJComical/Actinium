package com.gtnewhorizon.gtnhlib.blockpos;

public final class BlockPos {
    public int x;
    public int y;
    public int z;

    public BlockPos() {}

    public BlockPos(int x, int y, int z) {
        set(x, y, z);
    }

    public BlockPos set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public BlockPos set(BlockPos other) {
        return set(other.x, other.y, other.z);
    }

    public BlockPos zero() {
        return set(0, 0, 0);
    }
}
