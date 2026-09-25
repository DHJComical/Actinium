package com.dhj.actinium.mixin.mod.voxelmap;

import com.dhj.actinium.compat.voxelmap.VoxelMapCompat;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMConfig;
import com.mamiyaotaru.voxelmap.Map;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scopes VoxelMap's minimap alpha preparation to the minimap rectangle while StellarCore's HUD
 * cache is active.
 *
 * <p>VoxelMap clears the whole color buffer (masked to alpha-only) before drawing its circular
 * mask. Outside the HUD-cache window that is fine: the framebuffer is the plain screen and the
 * cleared alpha only shapes the minimap. Inside the cache window the framebuffer is the cached HUD
 * texture, so the clear wipes the alpha of every HUD element already drawn (making the cached blit
 * transparent) - unless the clear is skipped, in which case the map rectangle keeps the cache's
 * opaque alpha and the map quad (blended against {@code DstAlpha}) is drawn over the whole
 * rectangle, showing map content around the circular mask.</p>
 *
 * <p>This mixin scopes the clear to the same rectangle VoxelMap later uses for the map draw (see
 * {@code Map.renderMap}): the HUD elements outside the rectangle keep their alpha, the map area is
 * prepared normally, and the map draw blends against the freshly cleared alpha inside the
 * rectangle only.</p>
 */
@Mixin(value = Map.class, remap = false)
public abstract class MixinVoxelMapRenderMap {
    /** {@code GL_SCISSOR_TEST} - capability used to scope the alpha clear. */
    private static final int GL_SCISSOR_TEST = 3089;

    @Shadow
    private int scWidth;

    @Shadow
    private int scHeight;

    @Inject(method = "renderMap", at = @At("HEAD"))
    private void voxelmap$scopeClearToMapArea(int x, int y, int scScale, CallbackInfo ci) {
        if (!GLSMConfig.hudCacheOverride) {
            return;
        }
        // Mirror the scissor rectangle VoxelMap computes for the map draw (Map.renderMap), so the
        // alpha-only clear that prepares the circular mask only touches the minimap area.
        double guiScale = (double) Minecraft.getMinecraft().displayWidth / (double) this.scWidth;
        GLStateManager.glEnable(GL_SCISSOR_TEST);
        GLStateManager.glScissor(
            (int) (guiScale * (x - 32)),
            (int) (guiScale * ((this.scHeight - y) - 32)),
            (int) (guiScale * 64.0),
            (int) (guiScale * 63.0));
        VoxelMapCompat.mapScissorActive = true;
    }

    @Inject(method = "renderMap", at = @At("RETURN"))
    private void voxelmap$clearMapScissorFlag(int x, int y, int scScale, CallbackInfo ci) {
        VoxelMapCompat.mapScissorActive = false;
    }
}
