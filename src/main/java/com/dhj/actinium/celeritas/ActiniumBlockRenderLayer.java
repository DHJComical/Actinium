package com.dhj.actinium.celeritas;

public enum ActiniumBlockRenderLayer {
    SOLID,
    CUTOUT,
    CUTOUT_MIPPED,
    TRANSLUCENT;

    public int toVanillaPass() {
        return this == TRANSLUCENT ? 1 : 0;
    }

    public net.minecraft.util.BlockRenderLayer toVanillaLayer() {
        return switch (this) {
            case SOLID -> net.minecraft.util.BlockRenderLayer.SOLID;
            case CUTOUT -> net.minecraft.util.BlockRenderLayer.CUTOUT;
            case CUTOUT_MIPPED -> net.minecraft.util.BlockRenderLayer.CUTOUT_MIPPED;
            case TRANSLUCENT -> net.minecraft.util.BlockRenderLayer.TRANSLUCENT;
        };
    }

    public static ActiniumBlockRenderLayer fromVanillaPass(int pass) {
        return pass == 0 ? CUTOUT_MIPPED : TRANSLUCENT;
    }

    public static ActiniumBlockRenderLayer fromVanillaLayer(net.minecraft.util.BlockRenderLayer layer) {
        return switch (layer) {
            case SOLID -> SOLID;
            case CUTOUT -> CUTOUT;
            case CUTOUT_MIPPED -> CUTOUT_MIPPED;
            case TRANSLUCENT -> TRANSLUCENT;
        };
    }
}
