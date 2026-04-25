package org.taumc.celeritas.mixin.features.render;

import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public class GameSettingsActiniumCloudMixin {
    @Shadow
    public int clouds;

    @Shadow
    public int renderDistanceChunks;

    @Inject(method = "shouldRenderClouds", at = @At("HEAD"), cancellable = true)
    private void actinium$overrideCloudMode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ActiniumRenderPipeline.INSTANCE.getCloudRenderModeOverride(this.clouds, this.renderDistanceChunks));
    }
}
