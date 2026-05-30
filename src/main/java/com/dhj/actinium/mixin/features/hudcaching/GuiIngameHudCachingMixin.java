package com.dhj.actinium.mixin.features.hudcaching;

import com.dhj.actinium.hudcaching.ActiniumHudCaching;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class GuiIngameHudCachingMixin {
    @Inject(method = "renderGameOverlay(F)V", at = @At("HEAD"))
    private void actinium$resetCaptures(float partialTicks, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderVignetteCaptured = false;
            ActiniumHudCaching.renderHelmetCaptured = false;
            ActiniumHudCaching.renderPortalCapturedTicks = -1.0F;
        }
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void actinium$captureRenderVignette(float lightLevel, ScaledResolution scaledRes, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderVignetteCaptured = true;
            ci.cancel();
        }
    }

    @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
    private void actinium$captureRenderPumpkinOverlay(ScaledResolution scaledRes, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderHelmetCaptured = true;
            ci.cancel();
        }
    }

    @Inject(method = "renderPortal", at = @At("HEAD"), cancellable = true)
    private void actinium$captureRenderPortal(float timeInPortal, ScaledResolution scaledRes, CallbackInfo ci) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            ActiniumHudCaching.renderPortalCapturedTicks = timeInPortal;
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "renderScoreboard",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I")
    )
    private int actinium$fixScoreboardTextAlpha(FontRenderer fontRenderer, String text, int x, int y, int color, Operation<Integer> original) {
        if (ActiniumHudCaching.renderingCacheOverride) {
            color |= 0xFF000000;
        }
        return original.call(fontRenderer, text, x, y, color);
    }
}
