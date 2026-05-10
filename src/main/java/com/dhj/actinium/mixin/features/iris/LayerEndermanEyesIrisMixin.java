package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.layer.GbufferPrograms;
import net.minecraft.client.renderer.entity.layers.LayerEndermanEyes;
import net.minecraft.entity.monster.EntityEnderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerEndermanEyes.class)
public class LayerEndermanEyesIrisMixin {
    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/monster/EntityEnderman;FFFFFFF)V", at = @At("HEAD"))
    private void actinium$beginEntityEyes(
        EntityEnderman entity,
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

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/monster/EntityEnderman;FFFFFFF)V", at = @At("RETURN"))
    private void actinium$endEntityEyes(
        EntityEnderman entity,
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
