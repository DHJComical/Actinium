package com.dhj.actinium.mixin.features.render;

import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererActiniumWeatherParticlesMixin {
    @Inject(method = "addRainParticles", at = @At("HEAD"), cancellable = true)
    private void actinium$disableWeatherParticlesWhenPackRequestsIt(CallbackInfo ci) {
        if (!ActiniumRenderPipeline.INSTANCE.shouldRenderWeatherParticles()) {
            ci.cancel();
        }
    }
}
