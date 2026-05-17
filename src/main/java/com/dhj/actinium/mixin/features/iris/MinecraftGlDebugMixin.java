package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftGlDebugMixin {
    @Redirect(method = "checkGLError(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;glGetError()I"))
    private int actinium$logGlStateForErrors(String message) {
        int error = GlStateManager.glGetError();
        IrisGlDebug.logMinecraftGlError(message, error);
        return error;
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterGameRenderer(CallbackInfo ci) {
        IrisGlDebug.check("minecraft:after-game-renderer");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterFramebufferRender(CallbackInfo ci) {
        IrisGlDebug.check("minecraft:after-framebuffer-render");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderStreamIndicator(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterStreamIndicator(CallbackInfo ci) {
        IrisGlDebug.check("minecraft:after-stream-indicator");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V")
    )
    private void actinium$markBeforeUpdateDisplay(CallbackInfo ci) {
        IrisGlDebug.markStage("minecraft:before-update-display");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterUpdateDisplay(CallbackInfo ci) {
        IrisGlDebug.check("minecraft:after-update-display");
    }
}
