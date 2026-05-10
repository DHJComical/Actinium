package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.layer.GbufferPrograms;
import net.minecraft.client.renderer.entity.layers.LayerSpiderEyes;
import net.minecraft.entity.monster.EntitySpider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerSpiderEyes.class)
public class LayerSpiderEyesIrisMixin {
    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/monster/EntitySpider;FFFFFFF)V", at = @At("HEAD"))
    private void actinium$beginEntityEyes(
        EntitySpider entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        CallbackInfo ci
    ) {
        GbufferPrograms.setupSpecialRenderCondition(SpecialCondition.ENTITY_EYES);
    }

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/monster/EntitySpider;FFFFFFF)V", at = @At("RETURN"))
    private void actinium$endEntityEyes(
        EntitySpider entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        CallbackInfo ci
    ) {
        GbufferPrograms.teardownSpecialRenderCondition();
    }
}
