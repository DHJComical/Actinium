package com.dhj.actinium.mixin.features.iris;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.ItemIdManager;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCape.class)
public class LayerCapeIrisMixin {
    @Unique
    private static final NamespacedId ACTINIUM$PLAYER_CAPE = new NamespacedId("minecraft", "player_cape");

    @Inject(
        method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPlayer;renderCape(F)V", shift = At.Shift.BEFORE)
    )
    private void actinium$setCapeItemId(
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        CallbackInfo ci
    ) {
        Object2IntFunction<NamespacedId> itemIds = BlockRenderingSettings.INSTANCE.getItemIds();
        if (itemIds != null) {
            CapturedRenderingState.INSTANCE.setCurrentRenderedItem(Math.max(0, itemIds.getInt(ACTINIUM$PLAYER_CAPE)));
        }
    }

    @Inject(
        method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPlayer;renderCape(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$resetCapeItemId(
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        CallbackInfo ci
    ) {
        ItemIdManager.resetItemId();
    }
}
