package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class RenderGlobalIrisMixin {
    @Inject(method = "renderSky(FI)V", at = @At("HEAD"))
    private void actinium$beginSky(float partialTicks, int pass, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.SKY);
        }
    }

    @Inject(method = "renderSky(FI)V", at = @At("RETURN"))
    private void actinium$endSky(float partialTicks, int pass, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.NONE);
        }
    }

    @Inject(method = "drawSelectionBox", at = @At("HEAD"))
    private void actinium$beginOutline(EntityPlayer player, RayTraceResult movingObjectPositionIn, int execute, float partialTicks, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        if (IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            GbufferPrograms.beginOutline();
        }
    }

    @Inject(method = "drawSelectionBox", at = @At("RETURN"))
    private void actinium$endOutline(EntityPlayer player, RayTraceResult movingObjectPositionIn, int execute, float partialTicks, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        if (IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            GbufferPrograms.endOutline();
        }
    }

}
