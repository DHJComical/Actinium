package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.EntityIdHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderManager.class)
public class RenderManagerIrisMixin {
    @Redirect(
        method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V")
    )
    private void actinium$renderEntityWithIrisId(
        Render<Entity> render,
        Entity entity,
        double x,
        double y,
        double z,
        float yaw,
        float partialTicks
    ) {
        int previousEntity = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        try {
            render.doRender(entity, x, y, z, yaw, partialTicks);
        } finally {
            CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntity);
        }
    }
}
