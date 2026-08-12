package com.gtnewhorizons.angelica.compat.mojang;

import net.minecraft.util.EnumFacing;

import java.util.BitSet;
import java.util.Set;

public class ChunkOcclusionData {
    private static final int DIRECTION_COUNT = EnumFacing.values().length;
    private final BitSet visibility;

    public ChunkOcclusionData() {
        this.visibility = new BitSet(DIRECTION_COUNT * DIRECTION_COUNT);
    }

    public void addOpenEdgeFaces(Set<EnumFacing> faces) {
        for (EnumFacing dirFrom : faces) {
            for (EnumFacing dirTo : faces) {
                this.setVisibleThrough(dirFrom, dirTo, true);
            }
        }

        for (EnumFacing direction : faces) {
            this.visibility.set(direction.ordinal() * DIRECTION_COUNT + direction.ordinal());
        }
    }
    public void setVisibleThrough(EnumFacing from, EnumFacing to, boolean visible) {
        this.visibility.set(from.ordinal() + to.ordinal() * DIRECTION_COUNT, visible);
        this.visibility.set(to.ordinal() + from.ordinal() * DIRECTION_COUNT, visible);
    }

    public boolean isVisibleThrough(EnumFacing from, EnumFacing to) {
        return this.visibility.get(from.ordinal() + to.ordinal() * DIRECTION_COUNT);
    }

    public void fill(boolean visible) {
        this.visibility.set(0, this.visibility.size(), visible);
    }
}
