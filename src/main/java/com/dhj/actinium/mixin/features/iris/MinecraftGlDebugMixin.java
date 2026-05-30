package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.profiling.TimerQueryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftGlDebugMixin {
    @Unique
    private static final boolean ACTINIUM_CHECK_FRAME_GL_ERRORS =
        Boolean.getBoolean("actinium.frameGlErrorCheck") || Boolean.getBoolean("actinium.postRenderGlErrorCheck");

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
    @Unique
    private long actinium$gameLoopStart;
    @Unique
    private long actinium$runTickStart;
    @Unique
    private long actinium$gameRendererStart;
    @Unique
    private long actinium$frameOutputStart;
    @Unique
    private long actinium$streamIndicatorStart;
    @Unique
    private long actinium$updateDisplayStart;
    @Unique
    private long actinium$scheduledTasksStart;
    @Unique
    private long actinium$preRenderGlErrorStart;
    @Unique
    private long actinium$soundListenerStart;
    @Unique
    private long actinium$renderSetupStart;
    @Unique
    private long actinium$fmlRenderTickStartStart;
    @Unique
    private long actinium$toastStart;
    @Unique
    private long actinium$fmlRenderTickEndStart;
    @Unique
    private long actinium$debugInfoStart;
    @Unique
    private long actinium$postRenderGlErrorStart;
    @Unique
    private long actinium$pauseStateStart;
    @Unique
    private long actinium$frameTimerStart;

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void actinium$startGameLoopTiming(CallbackInfo ci) {
        this.actinium$gameLoopStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(method = "runGameLoop", at = @At("RETURN"))
    private void actinium$finishGameLoopTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("total", this.actinium$gameLoopStart);
        IrisGlDebug.incrementGameLoopFrameCount();
    }

    @Inject(method = "runTick()V", at = @At("HEAD"))
    private void actinium$startRunTickTiming(CallbackInfo ci) {
        this.actinium$runTickStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(method = "runTick()V", at = @At("RETURN"))
    private void actinium$finishRunTickTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("run-tick", this.actinium$runTickStart);
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z")
    )
    private void actinium$startScheduledTasksTiming(CallbackInfo ci) {
        this.actinium$scheduledTasksStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 0)
    )
    private void actinium$finishScheduledTasksAndStartPreRenderGlErrorTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("scheduled-tasks", this.actinium$scheduledTasksStart);
        this.actinium$preRenderGlErrorStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/SoundHandler;setListener(Lnet/minecraft/entity/Entity;F)V")
    )
    private void actinium$finishPreRenderGlErrorAndStartSoundTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("pre-render-gl-error", this.actinium$preRenderGlErrorStart);
        this.actinium$soundListenerStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", ordinal = 0)
    )
    private void actinium$finishSoundAndStartRenderSetupTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("sound-listener", this.actinium$soundListenerStart);
        this.actinium$renderSetupStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickStart(F)V", remap = false)
    )
    private void actinium$finishRenderSetupAndStartFmlRenderTickStartTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("render-setup", this.actinium$renderSetupStart);
        this.actinium$fmlRenderTickStartStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(method = "checkGLError(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void actinium$markGlErrorCheck(String message, CallbackInfo ci) {
        IrisGlDebug.markStage("minecraft:check-gl-error:" + message);
        if (!ACTINIUM_CHECK_FRAME_GL_ERRORS && ("Pre render".equals(message) || "Post render".equals(message))) {
            ci.cancel();
        }
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V")
    )
    private void actinium$startFrameRenderTimer(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("fml-render-tick-start", this.actinium$fmlRenderTickStartStart);
        this.actinium$gameRendererStart = IrisGlDebug.beginGameLoopStageTiming();
        if (!IrisGlDebug.shouldCaptureGpuPerfTiming()) {
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
        IrisGlDebug.recordGameLoopStageTiming("game-renderer", this.actinium$gameRendererStart);

        IrisGlDebug.logWhiteScreenProbe("after-game-renderer");
        IrisGlDebug.check("minecraft:after-game-renderer");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/toasts/GuiToast;drawToast(Lnet/minecraft/client/gui/ScaledResolution;)V")
    )
    private void actinium$startToastTiming(CallbackInfo ci) {
        this.actinium$toastStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/toasts/GuiToast;drawToast(Lnet/minecraft/client/gui/ScaledResolution;)V", shift = At.Shift.AFTER)
    )
    private void actinium$finishToastTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("toast", this.actinium$toastStart);
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickEnd(F)V", remap = false)
    )
    private void actinium$startFmlRenderTickEndTiming(CallbackInfo ci) {
        this.actinium$fmlRenderTickEndStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickEnd(F)V", shift = At.Shift.AFTER, remap = false)
    )
    private void actinium$finishFmlRenderTickEndTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("fml-render-tick-end", this.actinium$fmlRenderTickEndStart);
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayDebugInfo(J)V")
    )
    private void actinium$startDebugInfoTiming(CallbackInfo ci) {
        this.actinium$debugInfoStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayDebugInfo(J)V", shift = At.Shift.AFTER)
    )
    private void actinium$finishDebugInfoTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("debug-info", this.actinium$debugInfoStart);
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;bindFramebuffer(Z)V", shift = At.Shift.AFTER)
    )
    private void actinium$probeAfterMainFramebufferBind(CallbackInfo ci) {
        IrisGlDebug.logWhiteScreenProbe("after-main-fbo-bind");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V")
    )
    private void actinium$startFrameOutputTimer(CallbackInfo ci) {
        this.actinium$frameOutputStart = IrisGlDebug.beginGameLoopStageTiming();
        if (!IrisGlDebug.shouldCaptureGpuPerfTiming()) {
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
        IrisGlDebug.recordGameLoopStageTiming("framebuffer-output", this.actinium$frameOutputStart);

        IrisGlDebug.logWhiteScreenProbe("after-framebuffer-render");
        IrisGlDebug.markStage("minecraft:after-framebuffer-render");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderStreamIndicator(F)V")
    )
    private void actinium$startStreamIndicatorTiming(CallbackInfo ci) {
        this.actinium$streamIndicatorStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderStreamIndicator(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterStreamIndicator(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("stream-indicator", this.actinium$streamIndicatorStart);
        IrisGlDebug.markStage("minecraft:after-stream-indicator");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V")
    )
    private void actinium$markBeforeUpdateDisplay(CallbackInfo ci) {
        this.actinium$updateDisplayStart = IrisGlDebug.beginGameLoopStageTiming();
        IrisGlDebug.markStage("minecraft:before-update-display");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterUpdateDisplay(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("update-display", this.actinium$updateDisplayStart);
        IrisGlDebug.markStage("minecraft:after-update-display");
        IrisGlDebug.logWhiteScreenProbe("after-update-display");
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", shift = At.Shift.AFTER)
    )
    private void actinium$startPostRenderGlErrorTiming(CallbackInfo ci) {
        this.actinium$postRenderGlErrorStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;fpsCounter:I", ordinal = 0)
    )
    private void actinium$finishPostRenderGlErrorAndStartPauseStateTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("post-render-gl-error", this.actinium$postRenderGlErrorStart);
        this.actinium$pauseStateStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;frameTimer:Lnet/minecraft/util/FrameTimer;", ordinal = 0)
    )
    private void actinium$finishPauseStateAndStartFrameTimerTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("pause-state", this.actinium$pauseStateStart);
        this.actinium$frameTimerStart = IrisGlDebug.beginGameLoopStageTiming();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;startNanoTime:J", opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.AFTER)
    )
    private void actinium$finishFrameTimerTiming(CallbackInfo ci) {
        IrisGlDebug.recordGameLoopStageTiming("frame-timer", this.actinium$frameTimerStart);
    }
}
