package com.dhj.actinium.mixin.features.hudcaching;

import com.dhj.actinium.hudcaching.ActiniumHudCaching;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class EntityRendererHudCachingMixin {
    @Redirect(
        method = "updateCameraAndRender(FJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V")
    )
    private void actinium$renderCachedHud(GuiIngame guiIngame, float partialTicks) {
        ActiniumHudCaching.renderCachedHud((EntityRenderer) (Object) this, guiIngame, partialTicks);
    }
}
