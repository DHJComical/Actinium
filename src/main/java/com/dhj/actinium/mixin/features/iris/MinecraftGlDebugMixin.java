package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.embeddedt.embeddium.impl.gl.profiling.TimerQueryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftGlDebugMixin {
    @Unique
    private TimerQueryManager actinium$frameOutputTimer;
    @Unique
    private TimerQueryManager actinium$frameRenderTimer;
    @Unique
    private long actinium$frameOutputCpuStart;
    @Unique
    private long actinium$frameRenderCpuStart;
    @Unique
    private boolean actinium$frameOutputProfiling;
    @Unique
    private boolean actinium$frameRenderProfiling;

    @Redirect(method = "checkGLError(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;glGetError()I"))
    private int actinium$logGlStateForErrors(String message) {
        int error = GlStateManager.glGetError();
        IrisGlDebug.logMinecraftGlError(message, error);
        return error;
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V")
    )
    private void actinium$startFrameRenderTimer(CallbackInfo ci) {
        if (!IrisGlDebug.isEnabled()) {
            this.actinium$frameRenderProfiling = false;
            return;
        }

        if (this.actinium$frameRenderTimer == null) {
            this.actinium$frameRenderTimer = new TimerQueryManager();
        }

        this.actinium$frameRenderTimer.updateTime();
        this.actinium$frameRenderCpuStart = System.nanoTime();
        this.actinium$frameRenderProfiling = true;
        this.actinium$frameRenderTimer.startProfiling();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterGameRenderer(CallbackInfo ci) {
        if (this.actinium$frameRenderProfiling) {
            this.actinium$frameRenderTimer.finishProfiling();
            IrisGlDebug.logFrameRenderTiming(System.nanoTime() - this.actinium$frameRenderCpuStart, this.actinium$frameRenderTimer.getLastTime());
            this.actinium$frameRenderProfiling = false;
        }

        IrisGlDebug.check("minecraft:after-game-renderer");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V")
    )
    private void actinium$startFrameOutputTimer(CallbackInfo ci) {
        if (!IrisGlDebug.isEnabled()) {
            this.actinium$frameOutputProfiling = false;
            return;
        }

        if (this.actinium$frameOutputTimer == null) {
            this.actinium$frameOutputTimer = new TimerQueryManager();
        }

        this.actinium$frameOutputTimer.updateTime();
        this.actinium$frameOutputCpuStart = System.nanoTime();
        this.actinium$frameOutputProfiling = true;
        this.actinium$frameOutputTimer.startProfiling();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterFramebufferRender(CallbackInfo ci) {
        if (this.actinium$frameOutputProfiling) {
            this.actinium$frameOutputTimer.finishProfiling();
            IrisGlDebug.logFrameOutputTiming(System.nanoTime() - this.actinium$frameOutputCpuStart, this.actinium$frameOutputTimer.getLastTime());
            this.actinium$frameOutputProfiling = false;
        }

        IrisGlDebug.markStage("minecraft:after-framebuffer-render");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderStreamIndicator(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterStreamIndicator(CallbackInfo ci) {
        IrisGlDebug.markStage("minecraft:after-stream-indicator");
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
        IrisGlDebug.markStage("minecraft:after-update-display");
    }
}
