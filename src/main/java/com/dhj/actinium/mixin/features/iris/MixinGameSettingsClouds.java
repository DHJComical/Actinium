package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.pipeline.SkyRenderDistance;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public abstract class MixinGameSettingsClouds {
    @Shadow
    public int clouds;

    @Shadow
    public int renderDistanceChunks;

    @Inject(method = "shouldRenderClouds", at = @At("HEAD"), cancellable = true)
    private void actinium$keepCloudsAtLowRenderDistance(CallbackInfoReturnable<Integer> cir) {
        if (Iris.enabled
            && IrisApiV0Impl.INSTANCE.isShaderPackInUse()
            && this.renderDistanceChunks < SkyRenderDistance.MINIMUM_RENDER_DISTANCE_CHUNKS) {
            cir.setReturnValue(this.clouds);
        }
    }
}
