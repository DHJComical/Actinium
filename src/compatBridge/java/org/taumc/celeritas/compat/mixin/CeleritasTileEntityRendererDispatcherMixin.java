package org.taumc.celeritas.compat.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Restores the legacy Celeritas six-argument tile entity render ABI for addons.
 */
@Mixin(value = TileEntityRendererDispatcher.class, priority = 1500)
public abstract class CeleritasTileEntityRendererDispatcherMixin {
    /**
     * Celeritas addons inject into this overload. The current renderer stores the camera
     * coordinates in the dispatcher and accepts the shorter render entry point.
     */
    public void render(TileEntity tileEntity, double x, double y, double z,
                       float partialTicks, int destroyStage) {
        ((TileEntityRendererDispatcher) (Object) this).render(tileEntity, partialTicks, destroyStage);
    }
}
