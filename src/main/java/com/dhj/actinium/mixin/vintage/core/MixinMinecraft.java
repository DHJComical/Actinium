package com.dhj.actinium.mixin.vintage.core;

import com.dhj.actinium.debug.flight.GlFlightRecording;
import com.dhj.actinium.gui.ActiniumWindowModeController;
import com.dhj.actinium.render.BufferBuilderStreamingDrawer;
import com.dhj.actinium.render.EndPortalCompositeRenderer;
import com.dhj.actinium.runtime.ActiniumRuntime;
import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;
import com.mitchej123.lwjgl.LWJGLServiceProvider;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Unique
    private final RenderAheadManager celeritas$renderAheadManager = new RenderAheadManager();

    @Inject(method = "runTick", at = @At("HEAD"))
    private void preRender(CallbackInfo ci) {
        ActiniumWindowModeController.synchronize((Minecraft) (Object) this);
    }

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void beginRenderFrame(CallbackInfo ci) {
        GlFlightRecording.beginFrame();
        EndPortalCompositeRenderer.beginFrame();
        final int limit = supportsCpuRenderAhead() ? ActiniumRuntime.options().advanced.cpuRenderAheadLimit : 0;
        if (limit > 0) {
            celeritas$renderAheadManager.startFrame(limit);
        }
    }

    @Inject(
            method = "runGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;updateDisplay()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void endStreamingFrame(CallbackInfo ci) {
        if (supportsCpuRenderAhead() && ActiniumRuntime.options().advanced.cpuRenderAheadLimit > 0) {
            celeritas$renderAheadManager.endFrame();
        }
        TessellatorStreamingDrawer.endFrame();
        BufferBuilderStreamingDrawer.endFrame();
        GlFlightRecording.beginSwap();
    }

    @Inject(
            method = "runGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;updateDisplay()V",
                    shift = At.Shift.AFTER
            )
    )
    private void actinium$finishDisplaySwap(CallbackInfo ci) {
        GlFlightRecording.endSwap();
    }

    @Inject(method = "runGameLoop", at = @At("RETURN"))
    private void actinium$finishFlightRecorderFrame(CallbackInfo ci) {
        GlFlightRecording.endFrame();
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("RETURN"))
    private void actinium$closeFlightRecorder(CallbackInfo ci) {
        GlFlightRecording.closeNormally();
    }

    @Unique
    private static boolean supportsCpuRenderAhead() {
        return LWJGLServiceProvider.LWJGL.isOpenGLVersionSupported(3, 2);
    }
}

