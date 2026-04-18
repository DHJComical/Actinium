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
        ActiniumRenderPipeline.INSTANCE.captureWorldState();
        ActiniumRenderPipeline.INSTANCE.endWorld();
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void actinium$runShadowPipeline(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (ActiniumRenderPipeline.INSTANCE.hasShadowProgram()) {
            ActiniumRenderPipeline.INSTANCE.renderShadowPass(partialTicks);
        }
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void actinium$runPostPipeline(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (ActiniumRenderPipeline.INSTANCE.hasPostProgram()) {
            ActiniumRenderPipeline.INSTANCE.renderPostPipeline(partialTicks);
        }
    }

    @Inject(method = "renderRainSnow", at = @At("HEAD"))
    private void actinium$beginWeather(float partialTicks, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginWeather();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(method = "renderRainSnow", at = @At("RETURN"))
    private void actinium$endWeather(float partialTicks, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endWeather();
    }
}
