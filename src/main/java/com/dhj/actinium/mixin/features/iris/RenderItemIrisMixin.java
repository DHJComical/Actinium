package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.render.FastLitItemDisplayListCache;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.layer.GbufferPrograms;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public class RenderItemIrisMixin {
    @Inject(method = "renderEffect(Lnet/minecraft/client/renderer/block/model/IBakedModel;)V", at = @At("HEAD"))
    private void actinium$beginItemGlint(IBakedModel model, CallbackInfo ci) {
        GbufferPrograms.setupSpecialRenderCondition(SpecialCondition.GLINT);
    }

    @Inject(method = "renderEffect(Lnet/minecraft/client/renderer/block/model/IBakedModel;)V", at = @At("RETURN"))
    private void actinium$endItemGlint(IBakedModel model, CallbackInfo ci) {
        GbufferPrograms.teardownSpecialRenderCondition();
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void actinium$clearFastLitItemDisplayLists(IResourceManager resourceManager, CallbackInfo ci) {
        FastLitItemDisplayListCache.clear();
    }
}
