package com.dhj.actinium.mixin.features.iris;

import com.gtnewhorizons.angelica.compat.mojang.Camera;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.rendering.RenderingState;
import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.compat.dh.DHCompat;
import net.coderbot.iris.debug.IrisGlDebug;
import net.coderbot.iris.gl.framebuffer.MinecraftFramebufferHelper;
import net.coderbot.iris.gl.program.Program;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.shaderpack.CloudSetting;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererIrisMixin implements IResourceManagerReloadListener {
    @Shadow
    public Minecraft mc;

    @Shadow
    private void renderCloudsCheck(RenderGlobal renderGlobalIn, float partialTicks, int pass, double x, double y, double z) {
    }

    @Shadow
    protected abstract void renderRainSnow(float partialTicks);

    @Shadow
    private void addRainParticles() {
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;getInstance()Lnet/minecraft/client/renderer/culling/ClippingHelper;", shift = At.Shift.AFTER)
    )
    private void actinium$beginIrisWorld(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        DHCompat.checkFrame();
        Iris.tryLoadShaderpackWhenPossible();
        if (!IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            return;
        }

        CapturedRenderingState.INSTANCE.setTickDelta(partialTicks);
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(System.nanoTime());

        Program.unbind();

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimensionName());
        BlockRenderingSettings.INSTANCE.reloadRendererIfRequired();
        pipeline.beginLevelRendering();
        IrisGlDebug.markStage("mixin:begin-world:done");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;updateRenderInfo(Lnet/minecraft/entity/Entity;Z)V", shift = At.Shift.AFTER)
    )
    private void actinium$captureCameraState(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        EntityLivingBase viewEntity = (EntityLivingBase) this.mc.getRenderViewEntity();
        if (viewEntity == null) {
            return;
        }

        Camera.INSTANCE.update(viewEntity, partialTicks);
        RenderingState.INSTANCE.setCameraPosition(
            Camera.INSTANCE.getEntityPos().x,
            Camera.INSTANCE.getEntityPos().y,
            Camera.INSTANCE.getEntityPos().z
        );
    }

    @Inject(
        method = "updateCameraAndRender(FJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterRenderWorld(float partialTicks, long nanoTime, CallbackInfo ci) {
        IrisGlDebug.markStage("entity-renderer:after-render-world");
    }

    @Inject(
        method = "updateCameraAndRender(FJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntityOutlineFramebuffer()V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterEntityOutlineFramebuffer(float partialTicks, long nanoTime, CallbackInfo ci) {
        IrisGlDebug.markStage("entity-renderer:after-entity-outline-fbo");
    }

    @Inject(
        method = "updateCameraAndRender(FJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/ShaderGroup;render(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterShaderGroup(float partialTicks, long nanoTime, CallbackInfo ci) {
        IrisGlDebug.markStage("entity-renderer:after-shader-group");
    }

    @Inject(
        method = "updateCameraAndRender(FJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterGameOverlay(float partialTicks, long nanoTime, CallbackInfo ci) {
        IrisGlDebug.markStage("entity-renderer:after-game-overlay");
    }

    @Inject(
        method = "updateCameraAndRender(FJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/GuiScreen;IIF)V", shift = At.Shift.AFTER, remap = false)
    )
    private void actinium$checkAfterDrawScreen(float partialTicks, long nanoTime, CallbackInfo ci) {
        IrisGlDebug.markStage("entity-renderer:after-draw-screen");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V")
    )
    private void actinium$renderIrisShadows(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!Iris.enabled || !IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            return;
        }

        IrisGlDebug.markStage("mixin:shadows:entry");
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.renderShadows((EntityRenderer) (Object) this, Camera.INSTANCE);
            IrisGlDebug.markStage("mixin:shadows:done");
        }
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterSky(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-sky");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;FIDDD)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterClouds(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-clouds");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterSetupTerrain(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-setup-terrain");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;updateChunks(J)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterUpdateChunks(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-update-chunks");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", shift = At.Shift.AFTER, ordinal = 0)
    )
    private void actinium$checkAfterSolidTerrain(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-terrain-solid");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", shift = At.Shift.AFTER, ordinal = 1)
    )
    private void actinium$checkAfterCutoutMippedTerrain(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-terrain-cutout-mipped");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", shift = At.Shift.AFTER, ordinal = 2)
    )
    private void actinium$checkAfterCutoutTerrain(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-terrain-cutout");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V", shift = At.Shift.AFTER, ordinal = 0)
    )
    private void actinium$checkAfterEntitiesPass0(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.check("render-world-pass:" + pass + ":after-entities-0");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/RayTraceResult;IF)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterSelectionBox(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-selection-box");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterBlockDamage(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-block-damage");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderLitParticles(Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterLitParticles(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-lit-particles");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterParticles(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-particles");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderRainSnow(F)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterWeather(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-weather");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", shift = At.Shift.AFTER, ordinal = 3)
    )
    private void actinium$checkAfterTranslucentTerrain(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-terrain-translucent");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V", shift = At.Shift.AFTER, ordinal = 1)
    )
    private void actinium$checkAfterEntitiesPass1(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.check("render-world-pass:" + pass + ":after-entities-1");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V", shift = At.Shift.AFTER, remap = false)
    )
    private void actinium$checkAfterRenderLast(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.markStage("render-world-pass:" + pass + ":after-render-last");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V", shift = At.Shift.AFTER)
    )
    private void actinium$checkAfterHand(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IrisGlDebug.check("render-world-pass:" + pass + ":after-hand");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V", remap = false)
    )
    private void actinium$finalizeIrisWorld(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!Iris.enabled || !IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            return;
        }

        MinecraftFramebufferHelper.restoreMinecraftFramebufferBuffers();
        IrisGlDebug.markStage("mixin:finalize:entry");
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline == null) {
            return;
        }

        IrisGlDebug.markStage("mixin:finalize:before-hand-translucent");
        HandRenderer.INSTANCE.renderTranslucent(partialTicks, Camera.INSTANCE, this.mc.renderGlobal, pipeline);
        IrisGlDebug.markStage("mixin:finalize:after-hand-translucent");
        this.mc.profiler.endStartSection("iris_final");
        pipeline.finalizeLevelRendering();
        IrisGlDebug.markStage("mixin:finalize:after-pipeline");
        Program.unbind();
        GLStateManager.glDepthMask(true);
        IrisGlDebug.markStage("mixin:finalize:done");
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V")
    )
    private void actinium$beginSky(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
        }
    }

    @Inject(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V", shift = At.Shift.AFTER)
    )
    private void actinium$endSky(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (!Iris.enabled) {
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.NONE);
        }
    }

    @Redirect(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I")
    )
    private int actinium$alwaysRenderSky(GameSettings settings) {
        return Math.max(settings.renderDistanceChunks, 4);
    }

    @ModifyConstant(method = "renderWorldPass(IFJ)V", constant = @Constant(doubleValue = 128.0D), expect = 2)
    private double actinium$alwaysRenderClouds(double cloudHeightCheck) {
        return IrisApi.getInstance().isShaderPackInUse() ? Double.NEGATIVE_INFINITY : cloudHeightCheck;
    }

    @Redirect(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;FIDDD)V")
    )
    private void actinium$renderClouds(EntityRenderer renderer, RenderGlobal renderGlobal, float partialTicks, int pass, double x, double y, double z) {
        if (!Iris.enabled) {
            this.renderCloudsCheck(renderGlobal, partialTicks, pass, x, y, z);
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline == null) {
            this.renderCloudsCheck(renderGlobal, partialTicks, pass, x, y, z);
            return;
        }

        pipeline.setPhase(WorldRenderingPhase.CLOUDS);
        if (pipeline.getCloudSetting() != CloudSetting.OFF) {
            this.renderCloudsCheck(renderGlobal, partialTicks, pass, x, y, z);
        }
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Redirect(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderRainSnow(F)V")
    )
    private void actinium$renderWeather(EntityRenderer renderer, float partialTicks) {
        if (!Iris.enabled) {
            this.renderRainSnow(partialTicks);
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline == null) {
            this.renderRainSnow(partialTicks);
            return;
        }

        pipeline.setPhase(WorldRenderingPhase.RAIN_SNOW);
        if (pipeline.shouldWriteRainAndSnowToDepthBuffer()) {
            GLStateManager.glDepthMask(true);
        }
        if (pipeline.shouldRenderWeather()) {
            this.renderRainSnow(partialTicks);
        }
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Redirect(
        method = "updateRenderer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;addRainParticles()V")
    )
    private void actinium$renderRainParticles(EntityRenderer renderer) {
        if (!Iris.enabled) {
            this.addRainParticles();
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline == null || pipeline.shouldRenderWeatherParticles()) {
            this.addRainParticles();
        }
    }

    @Redirect(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;F)V")
    )
    private void actinium$renderBlockDamage(RenderGlobal renderGlobal, Tessellator tessellator, net.minecraft.client.renderer.BufferBuilder bufferBuilder, net.minecraft.entity.Entity entity, float partialTicks) {
        if (!Iris.enabled) {
            renderGlobal.drawBlockDamageTexture(tessellator, bufferBuilder, entity, partialTicks);
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.DESTROY);
        }
        renderGlobal.drawBlockDamageTexture(tessellator, bufferBuilder, entity, partialTicks);
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.NONE);
        }
    }

    @Redirect(
        method = "renderHand(FI)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemInFirstPerson(F)V")
    )
    private void actinium$disableVanillaShaderHand(ItemRenderer itemRenderer, float partialTicks) {
        boolean shaderPackInUse = IrisApi.getInstance().isShaderPackInUse();
        if (!shaderPackInUse) {
            itemRenderer.renderItemInFirstPerson(partialTicks);
        }
    }
}
