package com.dhj.actinium.mixin.features.iris;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.EntityIdHelper;
import net.coderbot.iris.debug.IrisGlDebug;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

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
        WorldRenderingPhase previousPhase = GbufferPrograms.getCurrentPhase();
        boolean beganEntityPhase = previousPhase == WorldRenderingPhase.NONE;
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        try {
            if (beganEntityPhase) {
                GbufferPrograms.beginEntities();
            }
            IrisGlDebug.logEntityPhase(
                    entity.getClass().getName(),
                    previousPhase.name(),
                    beganEntityPhase,
                    actinium$matrixSummary(RenderingState.INSTANCE.getModelViewMatrix()),
                    actinium$matrixSummary(RenderingState.INSTANCE.getProjectionMatrix()));
            if (previousPhase == WorldRenderingPhase.NONE && beganEntityPhase) {
                IrisGlDebug.logShadowEntityDraw(entity.getClass().getName(), x, y, z, yaw, partialTicks);
            }
            render.doRender(entity, x, y, z, yaw, partialTicks);
        } finally {
            if (beganEntityPhase) {
                GbufferPrograms.endEntities();
            }
            CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntity);
        }
    }

    private static String actinium$matrixSummary(Matrix4f matrix) {
        return String.format(
                Locale.ROOT,
                "[%.4f,%.4f,%.4f,%.4f|%.4f,%.4f,%.4f,%.4f]",
                matrix.m00(),
                matrix.m01(),
                matrix.m02(),
                matrix.m03(),
                matrix.m10(),
                matrix.m11(),
                matrix.m12(),
                matrix.m13());
    }
}
