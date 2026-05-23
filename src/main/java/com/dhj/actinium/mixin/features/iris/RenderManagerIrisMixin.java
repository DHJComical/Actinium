package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
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
        boolean shaderPackInUse = IrisApiV0Impl.INSTANCE.isShaderPackInUse();
        if (!shaderPackInUse) {
            render.doRender(entity, x, y, z, yaw, partialTicks);
            return;
        }

        int previousEntity = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        WorldRenderingPhase previousPhase = GbufferPrograms.getCurrentPhase();
        boolean beganEntityPhase = previousPhase == WorldRenderingPhase.NONE;
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        try {
            if (beganEntityPhase) {
                GbufferPrograms.beginEntities();
            }
            render.doRender(entity, x, y, z, yaw, partialTicks);
        } finally {
            if (beganEntityPhase) {
                GbufferPrograms.endEntities();
            }
            CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntity);
        }
    }

    @Redirect(
        method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRenderShadowAndFire(Lnet/minecraft/entity/Entity;DDDFF)V")
    )
    private void actinium$renderEntityShadow(
        Render<Entity> render,
        Entity entity,
        double x,
        double y,
        double z,
        float yaw,
        float partialTicks
    ) {
        render.doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
    }

    @Redirect(
        method = "renderMultipass(Lnet/minecraft/entity/Entity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;renderMultipass(Lnet/minecraft/entity/Entity;DDDFF)V")
    )
    private void actinium$renderEntityMultipassWithIrisId(
        Render<Entity> render,
        Entity entity,
        double x,
        double y,
        double z,
        float yaw,
        float partialTicks
    ) {
        boolean shaderPackInUse = IrisApiV0Impl.INSTANCE.isShaderPackInUse();
        if (!shaderPackInUse) {
            render.renderMultipass(entity, x, y, z, yaw, partialTicks);
            return;
        }

        int previousEntity = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        WorldRenderingPhase previousPhase = GbufferPrograms.getCurrentPhase();
        boolean beganEntityPhase = previousPhase == WorldRenderingPhase.NONE;
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        try {
            if (beganEntityPhase) {
                GbufferPrograms.beginEntities();
            }
            render.renderMultipass(entity, x, y, z, yaw, partialTicks);
        } finally {
            if (beganEntityPhase) {
                GbufferPrograms.endEntities();
            }
            CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntity);
        }
    }
}
