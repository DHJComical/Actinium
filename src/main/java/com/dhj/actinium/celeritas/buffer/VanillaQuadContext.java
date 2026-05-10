package com.dhj.actinium.celeritas.buffer;

public final class VanillaQuadContext {
    private final int localPosX;
    private final int localPosY;
    private final int localPosZ;
    private final int blockStateId;
    private final short renderType;
    private final byte lightValue;

    public VanillaQuadContext(int localPosX, int localPosY, int localPosZ, int blockStateId, short renderType, byte lightValue) {
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

    public VanillaQuadContext withBlockStateId(int blockStateId) {
        if (this.blockStateId == blockStateId) {
            return this;
        }

        return new VanillaQuadContext(
                this.localPosX,
                this.localPosY,
                this.localPosZ,
                blockStateId,
                this.renderType,
                this.lightValue
        );
    }

    public short renderType() {
        return this.renderType;
    }

    public byte lightValue() {
        return this.lightValue;
    }
}
