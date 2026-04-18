package org.taumc.celeritas.mixin.features.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class RenderGlobalActiniumPipelineMixin {
    @Inject(method = "renderSky(FI)V", at = @At("HEAD"))
    private void actinium$beginSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginSky();
        ActiniumRenderPipeline.INSTANCE.captureSkyStageState();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableFog()V", ordinal = 0))
    private void actinium$endBaseSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endSky();
    }

    @Inject(method = "renderSky(FI)V", at = @At("RETURN"))
    private void actinium$endSky(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endSky();
    }

    @Inject(method = "renderClouds(FIDDD)V", at = @At("HEAD"))
    private void actinium$beginClouds(float partialTicks, int pass, double x, double y, double z, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginClouds();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(method = "renderClouds(FIDDD)V", at = @At("RETURN"))
    private void actinium$endClouds(float partialTicks, int pass, double x, double y, double z, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endClouds();
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0))
    private void actinium$beginSkyTextured(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.beginSkyTextured();
        ActiniumRenderPipeline.INSTANCE.bindWorldStageProgram(partialTicks);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableTexture2D()V", ordinal = 2))
    private void actinium$endSkyTextured(float partialTicks, int pass, CallbackInfo ci) {
        ActiniumRenderPipeline.INSTANCE.unbindWorldStageProgram();
        ActiniumRenderPipeline.INSTANCE.endSky();
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getCelestialAngle(F)F"),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getRainStrength(F)F"))
    )
    private void actinium$applySunPathRotation(float partialTicks, int pass, CallbackInfo ci) {
        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return;
        }

        float sunPathRotation = ActiniumShaderPackManager.getActiveShaderProperties().getSunPathRotation();
        if (sunPathRotation != 0.0F) {
            GlStateManager.rotate(sunPathRotation, 0.0F, 0.0F, 1.0F);
        }
    }

    @Inject(
            method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 0),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;calcSunriseSunsetColors(FF)[F"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0)
            )
    )
    private void actinium$clearSunriseFan(float partialTicks, int pass, CallbackInfo ci) {
        if (this.actinium$shouldSuppressVanillaSkyGeometry()) {
            Tessellator.getInstance().getBuffer().reset();
        }
    }

    @WrapWithCondition(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V", ordinal = 2))
    private boolean actinium$renderSky2Vbo(VertexBuffer vertexBuffer, int mode) {
        return !this.actinium$shouldSuppressVanillaSkyGeometry();
    }

    @WrapWithCondition(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 2))
    private boolean actinium$renderSky2CallListBelowHorizon(int displayList) {
        return !this.actinium$shouldSuppressVanillaSkyGeometry();
    }

    @WrapWithCondition(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 3))
    private boolean actinium$renderSky2CallList(int displayList) {
        return !this.actinium$shouldSuppressVanillaSkyGeometry();
    }

    @Unique
    private boolean actinium$shouldSuppressVanillaSkyGeometry() {
        return ActiniumShaderPackManager.areShadersEnabled()
                && ActiniumRenderPipeline.INSTANCE.hasSkyProgram()
                && ActiniumRenderPipeline.INSTANCE.hasPostProgram();
    }
}
