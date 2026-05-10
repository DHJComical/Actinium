package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.uniforms.ItemIdManager;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerArmorBase.class)
public class LayerArmorBaseIrisMixin {
    @Inject(
        method = "renderArmorLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFFLnet/minecraft/inventory/EntityEquipmentSlot;)V",
        at = @At("HEAD")
    )
    private void actinium$setArmorItemId(
        EntityLivingBase entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        EntityEquipmentSlot slot,
        CallbackInfo ci
    ) {
        ItemIdManager.setItemId(entity.getItemStackFromSlot(slot));
    }

    @Inject(
        method = "renderArmorLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFFLnet/minecraft/inventory/EntityEquipmentSlot;)V",
        at = @At("RETURN")
    )
    private void actinium$resetArmorItemId(
        EntityLivingBase entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        EntityEquipmentSlot slot,
        CallbackInfo ci
    ) {
        ItemIdManager.resetItemId();
    }

    @Inject(
        method = "renderEnchantedGlint(Lnet/minecraft/client/renderer/entity/RenderLivingBase;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/model/ModelBase;FFFFFFF)V",
        at = @At("HEAD")
    )
    private static void actinium$beginArmorGlint(
        RenderLivingBase<?> renderer,
        EntityLivingBase entity,
        ModelBase model,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        CallbackInfo ci
    ) {
        GbufferPrograms.setupSpecialRenderCondition(SpecialCondition.GLINT);
    }

    @Inject(
        method = "renderEnchantedGlint(Lnet/minecraft/client/renderer/entity/RenderLivingBase;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/model/ModelBase;FFFFFFF)V",
        at = @At("RETURN")
    )
    private static void actinium$endArmorGlint(
        RenderLivingBase<?> renderer,
        EntityLivingBase entity,
        ModelBase model,
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
