package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.uniforms.ItemIdManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderEntityItem.class)
public class RenderEntityItemIrisMixin {
    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("HEAD"))
    private void actinium$setDroppedItemId(
        EntityItem entity,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        ItemIdManager.setItemId(entity.getItem());
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("RETURN"))
    private void actinium$resetDroppedItemId(
        EntityItem entity,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        ItemIdManager.resetItemId();
    }
}
