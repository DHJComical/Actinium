package org.taumc.celeritas.mixin.features.render;

import com.dhj.actinium.shader.uniform.ActiniumCapturedRenderingState;
import com.dhj.actinium.shader.uniform.ActiniumEntityIdHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderManager.class)
public class RenderManagerActiniumEntityStateMixin {
    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void actinium$beginRenderEntity(Entity entityIn,
                                            double x,
                                            double y,
                                            double z,
                                            float yaw,
                                            float partialTicks,
                                            boolean debugBoundingBox,
                                            CallbackInfo ci) {
        ActiniumCapturedRenderingState.setCurrentEntity(ActiniumEntityIdHelper.getEntityId(entityIn));
        ActiniumCapturedRenderingState.setCurrentEntityColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void actinium$endRenderEntity(Entity entityIn,
                                          double x,
                                          double y,
                                          double z,
                                          float yaw,
                                          float partialTicks,
                                          boolean debugBoundingBox,
                                          CallbackInfo ci) {
        ActiniumCapturedRenderingState.resetEntityState();
    }

    @Inject(method = "renderMultipass", at = @At("HEAD"))
    private void actinium$beginRenderMultipass(Entity entityIn, float partialTicks, CallbackInfo ci) {
        ActiniumCapturedRenderingState.setCurrentEntity(ActiniumEntityIdHelper.getEntityId(entityIn));
        ActiniumCapturedRenderingState.setCurrentEntityColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Inject(method = "renderMultipass", at = @At("RETURN"))
    private void actinium$endRenderMultipass(Entity entityIn, float partialTicks, CallbackInfo ci) {
        ActiniumCapturedRenderingState.resetEntityState();
    }
}
