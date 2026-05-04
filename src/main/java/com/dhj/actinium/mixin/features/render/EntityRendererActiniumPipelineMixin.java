package com.dhj.actinium.mixin.features.render;

import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererActiniumPipelineMixin {
    @Inject(method = "renderWorldPass", at = @At("HEAD"))
    private void actinium$beginWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginWorld(partialTicks);
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void actinium$runPreparePipeline(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.renderPreparePipeline(partialTicks);
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
                    ordinal = 3,
                    shift = At.Shift.BEFORE
            )
    )
    private void actinium$runDeferredPipeline(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.renderDeferredPipeline(partialTicks);
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            )
    )
    private void actinium$finalizeWorldPassBeforeHand(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.finalizeWorldBeforeHand();
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void actinium$prepareFirstPersonState(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.prepareFirstPersonRenderState();
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V",
                    shift = At.Shift.AFTER
            )
    )
    private void actinium$finalizeWorldPassAfterHand(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.finalizeWorldAfterHand(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At("TAIL"))
    private void actinium$finalizeWorldPassAtTail(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.finalizeWorldAfterHand(partialTicks);
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void actinium$beginParticlePass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!ActiniumRenderPipeline.INSTANCE.shouldUseParticleProgram()) {
            return;
        }

        ActiniumRenderPipeline.INSTANCE.beginParticles();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void actinium$endParticlePass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!ActiniumRenderPipeline.INSTANCE.shouldUseParticleProgram()) {
            return;
        }

        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endParticles();
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void actinium$runShadowPipeline(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (ActiniumRenderPipeline.INSTANCE.hasShadowProgram()) {
            ActiniumRenderPipeline.INSTANCE.renderShadowPass(partialTicks);
        }
    }

    @Inject(method = "renderRainSnow", at = @At("HEAD"))
    private void actinium$beginWeather(float partialTicks, CallbackInfo ci) {
        if (!ActiniumRenderPipeline.INSTANCE.shouldUseWeatherProgram()) {
            return;
        }

        ActiniumRenderPipeline.INSTANCE.beginWeather();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(method = "renderRainSnow", at = @At("RETURN"))
    private void actinium$endWeather(float partialTicks, CallbackInfo ci) {
        if (!ActiniumRenderPipeline.INSTANCE.shouldUseWeatherProgram()) {
            return;
        }

        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endWeather();
    }
}
