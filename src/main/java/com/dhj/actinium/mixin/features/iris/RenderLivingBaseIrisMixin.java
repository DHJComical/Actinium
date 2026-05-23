package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.ItemIdManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLivingBase.class)
public abstract class RenderLivingBaseIrisMixin<T extends EntityLivingBase> {
    @Shadow
    protected abstract int getColorMultiplier(T entitylivingbaseIn, float lightBrightness, float partialTickTime);

    @Inject(
        method = {
            "setBrightness(Lnet/minecraft/entity/EntityLivingBase;FZ)Z",
            "func_177092_a(Lnet/minecraft/entity/EntityLivingBase;FZ)Z",
            "a(Lvp;FZ)Z"
        },
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void actinium$replaceBrightnessTexEnv(T entity, float partialTicks, boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        if (!IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            return;
        }

        int color = this.getColorMultiplier(entity, entity.getBrightness(), partialTicks);
        boolean hasColorMultiplier = (color >> 24 & 255) > 0;
        boolean hurtOrDying = entity.hurtTime > 0 || entity.deathTime > 0;

        if (!hasColorMultiplier && !hurtOrDying) {
            cir.setReturnValue(false);
            return;
        }

        if (!hasColorMultiplier && !combineTextures) {
            cir.setReturnValue(false);
            return;
        }

        if (hurtOrDying) {
            CapturedRenderingState.INSTANCE.setCurrentEntityColor(1.0F, 0.0F, 0.0F, 0.3F);
        } else {
            float a = (color >> 24 & 255) / 255.0F;
            float r = (color >> 16 & 255) / 255.0F;
            float g = (color >> 8 & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            CapturedRenderingState.INSTANCE.setCurrentEntityColor(r, g, b, a);
        }

        cir.setReturnValue(true);
    }

    @Inject(
        method = {
            "setBrightness(Lnet/minecraft/entity/EntityLivingBase;FZ)Z",
            "func_177092_a(Lnet/minecraft/entity/EntityLivingBase;FZ)Z",
            "a(Lvp;FZ)Z"
        },
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;flip()Ljava/nio/Buffer;"),
        require = 0
    )
    private void actinium$captureEntityColor(T entity, float partialTicks, boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        int color = this.getColorMultiplier(entity, entity.getBrightness(), partialTicks);
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        CapturedRenderingState.INSTANCE.setCurrentEntityColor(r, g, b, a);
    }

    @Inject(
        method = {
            "unsetBrightness()V",
            "func_177091_f()V",
            "c()V"
        },
        at = @At("RETURN"),
        require = 0
    )
    private void actinium$resetEntityColor(CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
    }

    @Inject(
        method = {
            "renderLayers(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",
            "func_177093_a(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",
            "a(Lvp;FFFFFFF)V"
        },
        at = @At("RETURN"),
        require = 0
    )
    private void actinium$resetItemIdAfterLayers(
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scaleIn,
        CallbackInfo ci
    ) {
        ItemIdManager.resetItemId();
    }
}
