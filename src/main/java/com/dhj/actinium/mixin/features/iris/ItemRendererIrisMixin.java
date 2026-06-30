package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.debug.ShaderRegressionDebug;
import com.gtnewhorizons.angelica.compat.mojang.InteractionHand;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.uniforms.ItemIdManager;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererIrisMixin {
    @Inject(method = "renderItemInFirstPerson(F)V", at = @At("HEAD"), cancellable = true)
    private void actinium$skipWrongHandPass(float partialTicks, CallbackInfo ci) {
        if (!IrisApi.getInstance().isShaderPackInUse()) {
            return;
        }

        boolean mainHandTranslucent = HandRenderer.INSTANCE.isHandTranslucent(InteractionHand.MAIN_HAND);
        boolean cancel = HandRenderer.INSTANCE.isRenderingSolid() == mainHandTranslucent;
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
        at = @At("HEAD")
    )
    private void actinium$setFirstPersonItemId(
        net.minecraft.client.entity.AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        EnumHand hand,
        float swingProgress,
        ItemStack stack,
        float equipProgress,
        CallbackInfo ci
    ) {
        ItemIdManager.setItemId(stack);
        ShaderRegressionDebug.logItemState("first-person-head", stack);
    }

    @Inject(
        method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
        at = @At("RETURN")
    )
    private void actinium$resetFirstPersonItemId(
        net.minecraft.client.entity.AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        EnumHand hand,
        float swingProgress,
        ItemStack stack,
        float equipProgress,
        CallbackInfo ci
    ) {
        ShaderRegressionDebug.logItemState("first-person-return", stack);
        ItemIdManager.resetItemId();
    }

    @Inject(
        method = "renderItemSide(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V",
        at = @At("HEAD")
    )
    private void actinium$setHeldItemId(
        EntityLivingBase entity,
        ItemStack stack,
        ItemCameraTransforms.TransformType transform,
        boolean leftHanded,
        CallbackInfo ci
    ) {
        ItemIdManager.setItemId(stack);
        ShaderRegressionDebug.logItemState("held-item-head", stack);
    }

    @Inject(
        method = "renderItemSide(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V",
        at = @At("RETURN")
    )
    private void actinium$resetHeldItemId(
        EntityLivingBase entity,
        ItemStack stack,
        ItemCameraTransforms.TransformType transform,
        boolean leftHanded,
        CallbackInfo ci
    ) {
        ShaderRegressionDebug.logItemState("held-item-return", stack);
        ItemIdManager.resetItemId();
    }
}
