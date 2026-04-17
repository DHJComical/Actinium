package com.dhj.actinium.celeritas.vertices;

public class BlockRenderContext {
    public int localPosX;
    public int localPosY;
    public int localPosZ;
    public short blockId = -1;
    public short renderType = -1;
    public byte lightValue;

    public void set(int localX, int localY, int localZ, short blockId, short renderType, byte lightValue) {
        this.localPosX = localX;
        this.localPosY = localY;
        this.localPosZ = localZ;
        this.blockId = blockId;
        this.renderType = renderType;
        this.lightValue = lightValue;
    }

    public void reset() {
        this.blockId = -1;
        this.renderType = -1;
        this.lightValue = 0;
    }
}
