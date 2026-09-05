package com.dhj.actinium.mixin.mod.hbm;

import com.dhj.actinium.compat.hbm.HbmRenderStateCompat;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Establishes the world lightmap before every HBM tile-entity renderer entry. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcherLightmap {
    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"))
    private void actinium$setWorldLightmap(
        TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci
    ) {
        if (HbmRenderStateCompat.isHbmTile(tileEntity)) {
            GLStateManager.beginForeignDraw();
        }
        HbmRenderStateCompat.setWorldLightmap(tileEntity);
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("RETURN"))
    private void actinium$finishForeignDraw(
        TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci
    ) {
        if (HbmRenderStateCompat.isHbmTile(tileEntity)) {
            GLStateManager.endForeignDraw();
        }
    }
}
