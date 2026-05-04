package com.dhj.actinium.mixin.features.render;

import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import com.dhj.actinium.shadows.ActiniumShadowRenderingState;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public abstract class RenderActiniumShadowMixin<T extends Entity> {
    @Inject(method = "doRenderShadowAndFire", at = @At("HEAD"), cancellable = true)
    private void actinium$skipEntityPostPassDuringShadowRender(T entityIn,
                                                               double x,
                                                               double y,
                                                               double z,
                                                               float yaw,
                                                               float partialTicks,
                                                               CallbackInfo ci) {
        if (ActiniumShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = "doRenderShadowAndFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/Render;renderShadow(Lnet/minecraft/entity/Entity;DDDFF)V"
            )
    )
    private boolean actinium$renderVanillaEntityShadow(Render<T> instance,
                                                       Entity entityIn,
                                                       double x,
                                                       double y,
                                                       double z,
                                                       float shadowAlpha,
                                                       float partialTicks) {
        return !ActiniumShaderPackManager.areShadersEnabled() || !ActiniumRenderPipeline.INSTANCE.hasShadowProgram();
    }
}
