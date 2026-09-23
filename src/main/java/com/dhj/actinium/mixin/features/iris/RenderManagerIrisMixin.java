package com.dhj.actinium.mixin.features.iris;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.debug.ShaderRegressionDebug;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.EntityIdHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Publishes the entity that is being rendered to the shader pipeline: the Iris
 * entity id consumed by {@code entityId} uniforms and the entities gbuffer phase
 * for the duration of {@link RenderManager#renderEntity} and
 * {@link RenderManager#renderMultipass}.
 *
 * <p>Both hooks wrap their whole target method instead of redirecting the
 * {@code Render#doRender} / {@code Render#renderMultipass} call sites. ASM
 * coremods rewrite this class before Mixin sees it — LagGoggles (via TickCentral)
 * moves the body of {@code renderEntity} into a {@code laggoggles_trueRender}
 * method and leaves a forwarding stub in its place — so a {@code @Redirect} on a
 * call site of the original body has nothing left to inject into, and the failed
 * injection check aborts the whole class transform. Wrapping the entry point
 * works for every body layout. See docs/compat/laggoggles.md.
 */
@Mixin(RenderManager.class)
public class RenderManagerIrisMixin {

    @WrapMethod(method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V")
    private void actinium$renderEntityWithIrisId(
        Entity entity,
        double x,
        double y,
        double z,
        float yaw,
        float partialTicks,
        boolean renderOutlines,
        Operation<Void> original
    ) {
        if (!IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            original.call(entity, x, y, z, yaw, partialTicks, renderOutlines);
            return;
        }

        Render<Entity> render = ((RenderManager) (Object) this).getEntityRenderObject(entity);
        int previousEntity = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        WorldRenderingPhase previousPhase = GbufferPrograms.getCurrentPhase();
        GbufferPrograms.EntityPhase entityPhase = null;
        boolean beganEntityPhase = false;
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        try {
            entityPhase = GbufferPrograms.enterEntityPhase();
            beganEntityPhase = entityPhase.changedPhase();
            ShaderRegressionDebug.logEntityPhase("renderEntity:before", entity, render, previousPhase.name(), beganEntityPhase);
            original.call(entity, x, y, z, yaw, partialTicks, renderOutlines);
        } finally {
            try {
                if (entityPhase != null) {
                    entityPhase.close();
                }
            } finally {
                ShaderRegressionDebug.logEntityPhase("renderEntity:after", entity, render, previousPhase.name(), beganEntityPhase);
                CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntity);
            }
        }
    }

    @WrapMethod(method = "renderMultipass(Lnet/minecraft/entity/Entity;F)V")
    private void actinium$renderEntityMultipassWithIrisId(
        Entity entity,
        float partialTicks,
        Operation<Void> original
    ) {
        if (!IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            original.call(entity, partialTicks);
            return;
        }

        Render<Entity> render = ((RenderManager) (Object) this).getEntityRenderObject(entity);
        int previousEntity = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        WorldRenderingPhase previousPhase = GbufferPrograms.getCurrentPhase();
        GbufferPrograms.EntityPhase entityPhase = null;
        boolean beganEntityPhase = false;
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        try {
            entityPhase = GbufferPrograms.enterEntityPhase();
            beganEntityPhase = entityPhase.changedPhase();
            ShaderRegressionDebug.logEntityPhase("renderMultipass:before", entity, render, previousPhase.name(), beganEntityPhase);
            original.call(entity, partialTicks);
        } finally {
            try {
                if (entityPhase != null) {
                    entityPhase.close();
                }
            } finally {
                ShaderRegressionDebug.logEntityPhase("renderMultipass:after", entity, render, previousPhase.name(), beganEntityPhase);
                CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntity);
            }
        }
    }
}
