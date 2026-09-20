package com.dhj.actinium.mixin.mod.littletiles;

import com.creativemd.littletiles.client.render.world.TileEntityRenderManager;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.dhj.actinium.compat.littletiles.LittleTilesCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Translates LittleTiles' asynchronous cache-build completion into a celeritas section rebuild.
 * Vanilla gets this refresh for free because LittleTiles flips {@code RenderChunk.setNeedsUpdate}
 * once the cache is uploaded; the celeritas pipeline never polls that flag.
 */
@Mixin(value = TileEntityRenderManager.class, remap = false)
public abstract class MixinTileEntityRenderManager {
    @Shadow
    private TileEntityLittleTiles te;

    @Inject(method = "finishBuildingCache", at = @At("RETURN"))
    private void actinium$scheduleSectionRebuildOnCacheBuilt(int index, int renderState, boolean force,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            LittleTilesCompat.onCacheBuildFinished(this.te);
        }
    }
}
