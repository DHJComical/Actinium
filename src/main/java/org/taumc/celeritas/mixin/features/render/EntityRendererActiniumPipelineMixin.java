package org.taumc.celeritas.mixin.features.render;

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
        ActiniumRenderPipeline.INSTANCE.beginWorld();
    }

    @Inject(method = "renderWorldPass", at = @At("RETURN"))
    private void actinium$endWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (ActiniumRenderPipeline.INSTANCE.hasPostProgram()) {
            ActiniumRenderPipeline.INSTANCE.beginPost();
            ActiniumRenderPipeline.INSTANCE.endPost();
        }

        ActiniumRenderPipeline.INSTANCE.endWorld();
    }

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void actinium$beginSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginSky();
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void actinium$endSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.endSky();
    }

    @Inject(method = "renderCloudsCheck", at = @At("HEAD"))
    private void actinium$beginClouds(float partialTicks, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginClouds();
    }

    @Inject(method = "renderCloudsCheck", at = @At("RETURN"))
    private void actinium$endClouds(float partialTicks, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.endClouds();
    }

    @Inject(method = "renderRainSnow", at = @At("HEAD"))
    private void actinium$beginWeather(float partialTicks, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginWeather();
    }

    @Inject(method = "renderRainSnow", at = @At("RETURN"))
    private void actinium$endWeather(float partialTicks, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.endWeather();
    }
}
