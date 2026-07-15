package com.dhj.actinium.mixin.vintage.core;

import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import com.mitchej123.lwjgl.LWJGLServiceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dhj.actinium.gui.ActiniumWindowModeController;
import com.dhj.actinium.runtime.ActiniumRuntime;
import com.dhj.actinium.render.BufferBuilderStreamingDrawer;
import com.dhj.actinium.render.EndPortalCompositeRenderer;

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
    }

    @Unique
    private static boolean supportsCpuRenderAhead() {
        return LWJGLServiceProvider.LWJGL.isOpenGLVersionSupported(3, 2);
    }
}

