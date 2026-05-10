package com.dhj.actinium.mixin.features.iris;

import com.gtnewhorizons.angelica.client.rendering.TextureTracker;
import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.texture.pbr.PBRTextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureUtil.class)
public class TextureUtilIrisMixin {
    @Inject(method = "bindTexture(I)V", at = @At("RETURN"))
    private static void actinium$notifyIrisTextureBind(int textureId, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        TextureTracker.INSTANCE.onBindTexture();
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.onBindTexture(textureId);
        }
    }

    @Inject(method = "deleteTexture(I)V", at = @At("HEAD"))
    private static void actinium$notifyIrisTextureDelete(int textureId, CallbackInfo ci) {
        if (Iris.enabled) {
            PBRTextureManager.INSTANCE.onDeleteTexture(textureId);
        }
    }
}
