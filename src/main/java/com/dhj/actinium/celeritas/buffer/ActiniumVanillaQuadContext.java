package com.dhj.actinium.celeritas.buffer;

public final class ActiniumVanillaQuadContext {
    private final int localPosX;
    private final int localPosY;
    private final int localPosZ;
    private final int blockStateId;
    private final short renderType;
    private final byte lightValue;

    public ActiniumVanillaQuadContext(int localPosX, int localPosY, int localPosZ, int blockStateId, short renderType, byte lightValue) {
        this.localPosX = localPosX;
        this.localPosY = localPosY;
        this.localPosZ = localPosZ;
        this.blockStateId = blockStateId;
        this.renderType = renderType;
        this.lightValue = lightValue;
    }

    public int localPosX() {
        return this.localPosX;
    }

    public int localPosY() {
        return this.localPosY;
    }

    public int localPosZ() {
        return this.localPosZ;
    }

    public int blockStateId() {
        return this.blockStateId;
    }

    public short renderType() {
        return this.renderType;
    }

    public byte lightValue() {
        return this.lightValue;
    }
}
