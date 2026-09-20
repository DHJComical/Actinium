package com.dhj.actinium.mixin.vintage.core.terrain;

import net.minecraft.client.multiplayer.WorldClient;
import dhj.embeddedt.embeddium.impl.render.chunk.map.ChunkTracker;
import dhj.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldClient.class)
public class MixinWorldClient implements ChunkTrackerHolder {
    private final ChunkTracker celeritas$tracker = new ChunkTracker();

    @Override
    public ChunkTracker sodium$getTracker() {
        return celeritas$tracker;
    }
}
