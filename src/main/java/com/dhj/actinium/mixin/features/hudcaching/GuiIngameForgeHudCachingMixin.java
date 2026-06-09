package com.dhj.actinium.mixin.features.hudcaching;

import com.dhj.actinium.hudcaching.ActiniumHudCaching;
import com.gtnewhorizons.angelica.mixins.interfaces.GuiIngameForgeAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class GuiIngameForgeHudCachingMixin {
    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void actinium$resetCaptures(float partialTicks, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderVignetteCaptured = false;
            ActiniumHudCaching.renderHelmetCaptured = false;
            ActiniumHudCaching.renderPortalCapturedTicks = -1.0F;
            ActiniumHudCaching.renderCrosshairsCaptured = false;
        }
    }

    @Inject(method = "renderCrosshairs", at = @At("HEAD"), cancellable = true, remap = false)
    private void actinium$captureRenderCrosshairs(float partialTicks, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderCrosshairsCaptured = true;
            ActiniumHudCaching.fixGLStateBeforeRenderingCache();
            ci.cancel();
        }
    }

    @Inject(method = "renderHelmet", at = @At("HEAD"), cancellable = true, remap = false)
    private void actinium$captureRenderHelmet(ScaledResolution res, float partialTicks, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderHelmetCaptured = true;
            ci.cancel();
        }
    }

    @Inject(method = "renderPortal", at = @At("HEAD"), cancellable = true, remap = false)
    private void actinium$captureRenderPortal(ScaledResolution res, float partialTicks, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderPortalCapturedTicks = partialTicks;
            ci.cancel();
        }
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), remap = false)
    private void actinium$bindBossHealthTexture(CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ((GuiIngameForgeAccessor) this).callBind(Gui.ICONS);
        }
    }
}
