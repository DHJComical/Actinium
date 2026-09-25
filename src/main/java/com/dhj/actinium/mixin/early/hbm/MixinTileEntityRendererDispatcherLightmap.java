package com.dhj.actinium.mixin.early.hbm;

import com.dhj.actinium.compat.hbm.HbmRenderStateCompat;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Establishes the world lightmap before every HBM tile-entity renderer entry.
 *
 * <p>Woven from an early config on purpose: {@code TileEntityRendererDispatcher} is a vanilla class
 * that large packs load well before the late/conditional mixin window. NTM's legacy coremod ASM
 * transformer pulls it in while resolving other classes' hierarchies, and Mixin cannot weave into
 * an already-loaded class, so a late config targeting it aborts the launch with
 * {@code MixinTargetAlreadyLoadedException} (issue #136). Early configs are registered before any
 * game class loads, so the injection is in place no matter who loads the dispatcher or when.
 *
 * <p>The hook is inert on vanilla-only installs: {@link HbmRenderStateCompat#isHbmInstalled()}
 * guards both injections, {@link HbmRenderStateCompat#setWorldLightmap} repeats the check for its
 * own callers, and {@link HbmRenderStateCompat#isHbmTile} never matches a vanilla tile entity.
 */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcherLightmap {
    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"))
    private void actinium$setWorldLightmap(
        TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci
    ) {
        if (!HbmRenderStateCompat.isHbmInstalled()) {
            return;
        }
        if (HbmRenderStateCompat.isHbmTile(tileEntity)) {
            GLStateManager.beginForeignDraw();
        }
        HbmRenderStateCompat.setWorldLightmap(tileEntity);
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("RETURN"))
    private void actinium$finishForeignDraw(
        TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci
    ) {
        if (!HbmRenderStateCompat.isHbmInstalled()) {
            return;
        }
        if (HbmRenderStateCompat.isHbmTile(tileEntity)) {
            GLStateManager.endForeignDraw();
        }
    }
}
