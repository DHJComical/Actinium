package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.shader.uniform.ActiniumCommonUniforms;
import com.dhj.actinium.shader.uniform.ActiniumCapturedRenderingState;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt2v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class ActiniumPostShaderInterface {
    private final @Nullable GlUniformInt colortex0Sampler;
    private final @Nullable GlUniformInt colortex1Sampler;
    private final @Nullable GlUniformInt colortex2Sampler;
    private final @Nullable GlUniformInt colortex3Sampler;
    private final @Nullable GlUniformInt colortex4Sampler;
    private final @Nullable GlUniformInt colortex5Sampler;
    private final @Nullable GlUniformInt colortex6Sampler;
    private final @Nullable GlUniformInt colortex7Sampler;
    private final @Nullable GlUniformInt gcolorSampler;
    private final @Nullable GlUniformInt gdepthSampler;
    private final @Nullable GlUniformInt gdepthtexSampler;
    private final @Nullable GlUniformInt gnormalSampler;
    private final @Nullable GlUniformInt compositeSampler;
    private final @Nullable GlUniformInt gaux1Sampler;
    private final @Nullable GlUniformInt gaux2Sampler;
    private final @Nullable GlUniformInt gaux3Sampler;
    private final @Nullable GlUniformInt gaux4Sampler;
    private final @Nullable GlUniformInt depthtex0Sampler;
    private final @Nullable GlUniformInt depthtex1Sampler;
    private final @Nullable GlUniformInt depthtex2Sampler;
    private final @Nullable GlUniformInt shadowSampler;
    private final @Nullable GlUniformInt shadowtex0Sampler;
    private final @Nullable GlUniformInt shadowtex1Sampler;
    private final @Nullable GlUniformInt watershadowSampler;
    private final @Nullable GlUniformInt shadowcolorSampler;
    private final @Nullable GlUniformInt shadowcolor0Sampler;
    private final @Nullable GlUniformInt shadowcolor1Sampler;
    private final @Nullable GlUniformInt noisetexSampler;

    private final @Nullable GlUniformFloat viewWidth;
    private final @Nullable GlUniformFloat viewHeight;
    private final @Nullable GlUniformFloat pixelSizeX;
    private final @Nullable GlUniformFloat pixelSizeY;
    private final @Nullable GlUniformFloat aspectRatioInverse;
    private final @Nullable GlUniformFloat frameTime;
    private final @Nullable GlUniformFloat frameTimeCounter;
    private final @Nullable GlUniformFloat rainStrength;
    private final @Nullable GlUniformFloat dayNightMix;
    private final @Nullable GlUniformFloat dayMoment;
    private final @Nullable GlUniformFloat dayMixer;
    private final @Nullable GlUniformFloat nightMixer;
    private final @Nullable GlUniformFloat volumetricDayMixer;
    private final @Nullable GlUniformFloat nearPlane;
    private final @Nullable GlUniformFloat farPlane;
    private final @Nullable GlUniformFloat blindness;
    private final @Nullable GlUniformFloat nightVision;
    private final @Nullable GlUniformFloat fovYInverse;
    private final @Nullable GlUniformFloat centerDepthSmooth;
    private final @Nullable GlUniformFloat ditherShift;
    private final @Nullable GlUniformFloat softLod;

    private final @Nullable GlUniformInt frameCounter;
    private final @Nullable GlUniformInt frameMod;
    private final @Nullable GlUniformInt worldTime;
    private final @Nullable GlUniformInt moonPhase;
    private final @Nullable GlUniformInt isEyeInWater;
    private final @Nullable GlUniformInt entityId;
    private final @Nullable GlUniformInt2v eyeBrightnessSmooth;

    private final @Nullable GlUniformFloat3v cameraPosition;
    private final @Nullable GlUniformFloat3v sunPosition;
    private final @Nullable GlUniformFloat3v moonPosition;
    private final @Nullable GlUniformFloat3v shadowLightPosition;
    private final @Nullable GlUniformFloat3v fogColor;
    private final @Nullable GlUniformFloat3v skyColor;
    private final @Nullable GlUniformFloat3v previousCameraPosition;
    private final @Nullable GlUniformFloat4v entityColor;

    private final @Nullable GlUniformMatrix4f gbufferModelView;
    private final @Nullable GlUniformMatrix4f gbufferModelViewInverse;
    private final @Nullable GlUniformMatrix4f gbufferProjection;
    private final @Nullable GlUniformMatrix4f gbufferProjectionInverse;
    private final @Nullable GlUniformMatrix4f gbufferPreviousModelView;
    private final @Nullable GlUniformMatrix4f gbufferPreviousProjection;
    private final @Nullable GlUniformMatrix4f shadowModelView;
    private final @Nullable GlUniformMatrix4f shadowModelViewInverse;
    private final @Nullable GlUniformMatrix4f shadowProjection;
    private final @Nullable GlUniformMatrix4f shadowProjectionInverse;
    private final Vector3f scratchSunPosition = new Vector3f();
    private final Vector3f scratchMoonPosition = new Vector3f();
    private final Vector3f scratchShadowLightPosition = new Vector3f();
    private boolean entityStateInitialized;
    private int lastEntityId = Integer.MIN_VALUE;
    private float lastEntityRed = Float.NaN;
    private float lastEntityGreen = Float.NaN;
    private float lastEntityBlue = Float.NaN;
    private float lastEntityAlpha = Float.NaN;

    ActiniumPostShaderInterface(ShaderBindingContext context) {
        this.colortex0Sampler = context.bindUniformIfPresent("colortex0", GlUniformInt::new);
        this.colortex1Sampler = context.bindUniformIfPresent("colortex1", GlUniformInt::new);
        this.colortex2Sampler = context.bindUniformIfPresent("colortex2", GlUniformInt::new);
        this.colortex3Sampler = context.bindUniformIfPresent("colortex3", GlUniformInt::new);
        this.colortex4Sampler = context.bindUniformIfPresent("colortex4", GlUniformInt::new);
        this.colortex5Sampler = context.bindUniformIfPresent("colortex5", GlUniformInt::new);
        this.colortex6Sampler = context.bindUniformIfPresent("colortex6", GlUniformInt::new);
        this.colortex7Sampler = context.bindUniformIfPresent("colortex7", GlUniformInt::new);
        this.gcolorSampler = context.bindUniformIfPresent("gcolor", GlUniformInt::new);
        this.gdepthSampler = context.bindUniformIfPresent("gdepth", GlUniformInt::new);
        this.gdepthtexSampler = context.bindUniformIfPresent("gdepthtex", GlUniformInt::new);
        this.gnormalSampler = context.bindUniformIfPresent("gnormal", GlUniformInt::new);
        this.compositeSampler = context.bindUniformIfPresent("composite", GlUniformInt::new);
        this.gaux1Sampler = context.bindUniformIfPresent("gaux1", GlUniformInt::new);
        this.gaux2Sampler = context.bindUniformIfPresent("gaux2", GlUniformInt::new);
        this.gaux3Sampler = context.bindUniformIfPresent("gaux3", GlUniformInt::new);
        this.gaux4Sampler = context.bindUniformIfPresent("gaux4", GlUniformInt::new);
        this.depthtex0Sampler = context.bindUniformIfPresent("depthtex0", GlUniformInt::new);
        this.depthtex1Sampler = context.bindUniformIfPresent("depthtex1", GlUniformInt::new);
        this.depthtex2Sampler = context.bindUniformIfPresent("depthtex2", GlUniformInt::new);
        this.shadowSampler = context.bindUniformIfPresent("shadow", GlUniformInt::new);
        this.shadowtex0Sampler = context.bindUniformIfPresent("shadowtex0", GlUniformInt::new);
        this.shadowtex1Sampler = context.bindUniformIfPresent("shadowtex1", GlUniformInt::new);
        this.watershadowSampler = context.bindUniformIfPresent("watershadow", GlUniformInt::new);
        this.shadowcolorSampler = context.bindUniformIfPresent("shadowcolor", GlUniformInt::new);
        this.shadowcolor0Sampler = context.bindUniformIfPresent("shadowcolor0", GlUniformInt::new);
        this.shadowcolor1Sampler = context.bindUniformIfPresent("shadowcolor1", GlUniformInt::new);
        this.noisetexSampler = context.bindUniformIfPresent("noisetex", GlUniformInt::new);

        this.viewWidth = context.bindUniformIfPresent("viewWidth", GlUniformFloat::new);
        this.viewHeight = context.bindUniformIfPresent("viewHeight", GlUniformFloat::new);
        this.pixelSizeX = context.bindUniformIfPresent("pixelSizeX", GlUniformFloat::new);
        this.pixelSizeY = context.bindUniformIfPresent("pixelSizeY", GlUniformFloat::new);
        this.aspectRatioInverse = context.bindUniformIfPresent("aspectRatioInverse", GlUniformFloat::new);
        this.frameTime = context.bindUniformIfPresent("frameTime", GlUniformFloat::new);
        this.frameTimeCounter = context.bindUniformIfPresent("frameTimeCounter", GlUniformFloat::new);
        this.rainStrength = context.bindUniformIfPresent("rainStrength", GlUniformFloat::new);
        this.dayNightMix = context.bindUniformIfPresent("dayNightMix", GlUniformFloat::new);
        this.dayMoment = context.bindUniformIfPresent("dayMoment", GlUniformFloat::new);
        this.dayMixer = context.bindUniformIfPresent("dayMixer", GlUniformFloat::new);
        this.nightMixer = context.bindUniformIfPresent("nightMixer", GlUniformFloat::new);
        this.volumetricDayMixer = context.bindUniformIfPresent("volumetricDayMixer", GlUniformFloat::new);
        this.nearPlane = context.bindUniformIfPresent("near", GlUniformFloat::new);
        this.farPlane = context.bindUniformIfPresent("far", GlUniformFloat::new);
        this.blindness = context.bindUniformIfPresent("blindness", GlUniformFloat::new);
        this.nightVision = context.bindUniformIfPresent("nightVision", GlUniformFloat::new);
        this.fovYInverse = context.bindUniformIfPresent("fovYInverse", GlUniformFloat::new);
        this.centerDepthSmooth = context.bindUniformIfPresent("centerDepthSmooth", GlUniformFloat::new);
        this.ditherShift = context.bindUniformIfPresent("ditherShift", GlUniformFloat::new);
        this.softLod = context.bindUniformIfPresent("softLod", GlUniformFloat::new);

        this.frameCounter = context.bindUniformIfPresent("frameCounter", GlUniformInt::new);
        this.frameMod = context.bindUniformIfPresent("frameMod", GlUniformInt::new);
        this.worldTime = context.bindUniformIfPresent("worldTime", GlUniformInt::new);
        this.moonPhase = context.bindUniformIfPresent("moonPhase", GlUniformInt::new);
        this.isEyeInWater = context.bindUniformIfPresent("isEyeInWater", GlUniformInt::new);
        this.entityId = context.bindUniformIfPresent("entityId", GlUniformInt::new);
        this.eyeBrightnessSmooth = context.bindUniformIfPresent("eyeBrightnessSmooth", GlUniformInt2v::new);

        this.cameraPosition = context.bindUniformIfPresent("cameraPosition", GlUniformFloat3v::new);
        this.sunPosition = context.bindUniformIfPresent("sunPosition", GlUniformFloat3v::new);
        this.moonPosition = context.bindUniformIfPresent("moonPosition", GlUniformFloat3v::new);
        this.shadowLightPosition = context.bindUniformIfPresent("shadowLightPosition", GlUniformFloat3v::new);
        this.fogColor = context.bindUniformIfPresent("fogColor", GlUniformFloat3v::new);
        this.skyColor = context.bindUniformIfPresent("skyColor", GlUniformFloat3v::new);
        this.previousCameraPosition = context.bindUniformIfPresent("previousCameraPosition", GlUniformFloat3v::new);
        this.entityColor = context.bindUniformIfPresent("entityColor", GlUniformFloat4v::new);

        this.gbufferModelView = context.bindUniformIfPresent("gbufferModelView", GlUniformMatrix4f::new);
        this.gbufferModelViewInverse = context.bindUniformIfPresent("gbufferModelViewInverse", GlUniformMatrix4f::new);
        this.gbufferProjection = context.bindUniformIfPresent("gbufferProjection", GlUniformMatrix4f::new);
        this.gbufferProjectionInverse = context.bindUniformIfPresent("gbufferProjectionInverse", GlUniformMatrix4f::new);
        this.gbufferPreviousModelView = context.bindUniformIfPresent("gbufferPreviousModelView", GlUniformMatrix4f::new);
        this.gbufferPreviousProjection = context.bindUniformIfPresent("gbufferPreviousProjection", GlUniformMatrix4f::new);
        this.shadowModelView = context.bindUniformIfPresent("shadowModelView", GlUniformMatrix4f::new);
        this.shadowModelViewInverse = context.bindUniformIfPresent("shadowModelViewInverse", GlUniformMatrix4f::new);
        this.shadowProjection = context.bindUniformIfPresent("shadowProjection", GlUniformMatrix4f::new);
        this.shadowProjectionInverse = context.bindUniformIfPresent("shadowProjectionInverse", GlUniformMatrix4f::new);
    }

    public void setupState(ActiniumRenderPipeline pipeline, ActiniumPostTargets targets, float partialTicks, float frameDeltaSeconds, float frameTimeCounterSeconds, int frameIndex) {
        bindSampler(this.colortex0Sampler, 0);
        bindSampler(this.colortex1Sampler, 1);
        bindSampler(this.colortex2Sampler, 2);
        bindSampler(this.colortex3Sampler, 3);
        bindSampler(this.colortex4Sampler, ActiniumRenderPipeline.POST_GAUX1_UNIT);
        bindSampler(this.colortex5Sampler, ActiniumRenderPipeline.POST_GAUX2_UNIT);
        bindSampler(this.colortex6Sampler, ActiniumRenderPipeline.POST_GAUX3_UNIT);
        bindSampler(this.colortex7Sampler, ActiniumRenderPipeline.POST_GAUX4_UNIT);
        bindSampler(this.gcolorSampler, 0);
        bindSampler(this.gdepthSampler, 1);
        bindSampler(this.gdepthtexSampler, ActiniumRenderPipeline.POST_DEPTHTEX0_UNIT);
        bindSampler(this.gnormalSampler, 2);
        bindSampler(this.compositeSampler, 3);
        bindSampler(this.gaux1Sampler, ActiniumRenderPipeline.POST_GAUX1_UNIT);
        bindSampler(this.gaux2Sampler, ActiniumRenderPipeline.POST_GAUX2_UNIT);
        bindSampler(this.gaux3Sampler, ActiniumRenderPipeline.POST_GAUX3_UNIT);
        bindSampler(this.gaux4Sampler, ActiniumRenderPipeline.POST_GAUX4_UNIT);
        bindSampler(this.depthtex0Sampler, ActiniumRenderPipeline.POST_DEPTHTEX0_UNIT);
        bindSampler(this.depthtex1Sampler, ActiniumRenderPipeline.POST_DEPTHTEX1_UNIT);
        bindSampler(this.depthtex2Sampler, ActiniumRenderPipeline.POST_DEPTHTEX2_UNIT);
        bindSampler(this.shadowSampler, ActiniumRenderPipeline.POST_SHADOW_TEX0_UNIT);
        bindSampler(this.shadowtex0Sampler, ActiniumRenderPipeline.POST_SHADOW_TEX0_UNIT);
        bindSampler(this.shadowtex1Sampler, ActiniumRenderPipeline.POST_SHADOW_TEX1_UNIT);
        bindSampler(this.watershadowSampler, ActiniumRenderPipeline.POST_SHADOW_TEX0_UNIT);
        bindSampler(this.shadowcolorSampler, ActiniumRenderPipeline.POST_SHADOW_COLOR0_UNIT);
        bindSampler(this.shadowcolor0Sampler, ActiniumRenderPipeline.POST_SHADOW_COLOR0_UNIT);
        bindSampler(this.shadowcolor1Sampler, ActiniumRenderPipeline.POST_SHADOW_COLOR1_UNIT);
        bindSampler(this.noisetexSampler, ActiniumRenderPipeline.POST_NOISETEX_UNIT);

        int width = pipeline.getWidth();
        int height = pipeline.getHeight();

        setFloat(this.viewWidth, width);
        setFloat(this.viewHeight, height);
        setFloat(this.pixelSizeX, 1.0f / Math.max(1, width));
        setFloat(this.pixelSizeY, 1.0f / Math.max(1, height));
        setFloat(this.aspectRatioInverse, height / (float) Math.max(1, width));
        setFloat(this.frameTime, frameDeltaSeconds);
        setFloat(this.frameTimeCounter, frameTimeCounterSeconds);
        setFloat(this.nearPlane, 0.05f);
        setFloat(this.farPlane, Math.max(16.0f, Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0f));
        setFloat(this.fovYInverse, (float) (1.0d / Math.tan(Math.toRadians(Minecraft.getMinecraft().gameSettings.fovSetting * 0.5d))));
        setFloat(this.centerDepthSmooth, pipeline.getCenterDepthSmooth());
        setFloat(this.ditherShift, pipeline.getDitherShift());
        setFloat(this.softLod, pipeline.getSoftLod());

        if (this.frameCounter != null) {
            this.frameCounter.setInt(frameIndex);
        }

        if (this.frameMod != null) {
            this.frameMod.setInt(pipeline.getFrameMod());
        }

        this.updateEntityState();

        Minecraft minecraft = Minecraft.getMinecraft();
        Entity entity = minecraft.getRenderViewEntity();

        if (entity != null) {
            if (this.cameraPosition != null) {
                this.cameraPosition.set(
                        (float) pipeline.getWorldCameraPosition().x,
                        (float) pipeline.getWorldCameraPosition().y,
                        (float) pipeline.getWorldCameraPosition().z
                );
            }

            if (this.previousCameraPosition != null) {
                this.previousCameraPosition.set(
                        (float) pipeline.getServedPreviousWorldCameraPosition().x,
                        (float) pipeline.getServedPreviousWorldCameraPosition().y,
                        (float) pipeline.getServedPreviousWorldCameraPosition().z
                );
            }

            if (this.isEyeInWater != null) {
                this.isEyeInWater.setInt(entity.isInsideOfMaterial(Material.WATER) ? 1 : 0);
            }

            if (this.eyeBrightnessSmooth != null) {
                int brightness = entity.getBrightnessForRender();
                this.eyeBrightnessSmooth.set(brightness & 0xFFFF, brightness >>> 16);
            }

            setFloat(this.blindness, getPotionStrength(entity));
            setFloat(this.nightVision, getNightVisionStrength(entity));

            if (this.skyColor != null && minecraft.world != null) {
                Vec3d currentSkyColor = minecraft.world.getSkyColor(entity, partialTicks);
                this.skyColor.set((float) currentSkyColor.x, (float) currentSkyColor.y, (float) currentSkyColor.z);
            }
        }

        float[] fog = pipeline.getFogColor();
        if (this.fogColor != null) {
            this.fogColor.set(fog[0], fog[1], fog[2]);
        }

        if (minecraft.world != null) {
            int currentWorldTime = ActiniumCommonUniforms.getWorldTime(minecraft.world);
            float currentDayMoment = ActiniumCommonUniforms.getDayMoment(currentWorldTime);
            pipeline.fillShaderCoreCelestialUniforms(
                    pipeline.getGbufferModelViewMatrix(),
                    partialTicks,
                    this.scratchSunPosition,
                    this.scratchMoonPosition,
                    this.scratchShadowLightPosition
            );

            if (this.sunPosition != null) {
                this.sunPosition.set(this.scratchSunPosition.x, this.scratchSunPosition.y, this.scratchSunPosition.z);
            }

            if (this.moonPosition != null) {
                this.moonPosition.set(this.scratchMoonPosition.x, this.scratchMoonPosition.y, this.scratchMoonPosition.z);
            }

            if (this.shadowLightPosition != null) {
                this.shadowLightPosition.set(
                        this.scratchShadowLightPosition.x,
                        this.scratchShadowLightPosition.y,
                        this.scratchShadowLightPosition.z
                );
            }

            if (this.worldTime != null) {
                this.worldTime.setInt(currentWorldTime);
            }

            if (this.moonPhase != null) {
                this.moonPhase.setInt(ActiniumCommonUniforms.getMoonPhase(minecraft.world));
            }

            setFloat(this.rainStrength, minecraft.world.getRainStrength(partialTicks));
            setFloat(this.dayNightMix, ActiniumCommonUniforms.getDayNightMix(currentWorldTime));
            setFloat(this.dayMoment, currentDayMoment);
            setFloat(this.dayMixer, ActiniumCommonUniforms.getDayMixer(currentDayMoment));
            setFloat(this.nightMixer, ActiniumCommonUniforms.getNightMixer(currentDayMoment));
            setFloat(this.volumetricDayMixer, ActiniumCommonUniforms.getVolumetricDayMixer(currentDayMoment));
        }

        setMatrix(this.gbufferModelView, pipeline.getGbufferModelViewMatrix());
        setMatrix(this.gbufferModelViewInverse, pipeline.getGbufferModelViewInverseMatrix());
        setMatrix(this.gbufferProjection, pipeline.getGbufferProjectionMatrix());
        setMatrix(this.gbufferProjectionInverse, pipeline.getGbufferProjectionInverseMatrix());
        setMatrix(this.gbufferPreviousModelView, pipeline.getPreviousGbufferModelViewMatrix());
        setMatrix(this.gbufferPreviousProjection, pipeline.getPreviousGbufferProjectionMatrix());
        setMatrix(this.shadowModelView, pipeline.getShadowModelViewMatrix());
        setMatrix(this.shadowModelViewInverse, pipeline.getShadowModelViewInverseMatrix());
        setMatrix(this.shadowProjection, pipeline.getShadowProjectionMatrix());
        setMatrix(this.shadowProjectionInverse, pipeline.getShadowProjectionInverseMatrix());
    }

    private static float getPotionStrength(Entity entity) {
        if (!(entity instanceof EntityLivingBase living)) {
            return 0.0f;
        }

        PotionEffect effect = living.getActivePotionEffect(MobEffects.BLINDNESS);
        return effect != null ? 1.0f : 0.0f;
    }

    private static float getNightVisionStrength(Entity entity) {
        if (!(entity instanceof EntityLivingBase living)) {
            return 0.0f;
        }

        PotionEffect effect = living.getActivePotionEffect(MobEffects.NIGHT_VISION);
        if (effect == null) {
            return 0.0f;
        }

        if (effect.getDuration() > 200) {
            return 1.0f;
        }

        return 0.7f + MathHelper.sin(effect.getDuration() * ((float) Math.PI * 0.2f)) * 0.3f;
    }

    private static void bindSampler(@Nullable GlUniformInt uniform, int unit) {
        if (uniform != null) {
            uniform.setInt(unit);
        }
    }

    private void updateEntityState() {
        int entityId = ActiniumCapturedRenderingState.getCurrentRenderedEntity();
        Vector4f capturedEntityColor = ActiniumCapturedRenderingState.getCurrentEntityColor();
        float red = capturedEntityColor.x;
        float green = capturedEntityColor.y;
        float blue = capturedEntityColor.z;
        float alpha = capturedEntityColor.w;

        if (this.entityStateInitialized
                && this.lastEntityId == entityId
                && this.lastEntityRed == red
                && this.lastEntityGreen == green
                && this.lastEntityBlue == blue
                && this.lastEntityAlpha == alpha) {
            return;
        }

        this.entityStateInitialized = true;
        this.lastEntityId = entityId;
        this.lastEntityRed = red;
        this.lastEntityGreen = green;
        this.lastEntityBlue = blue;
        this.lastEntityAlpha = alpha;

        if (this.entityId != null) {
            this.entityId.setInt(entityId);
        }

        if (this.entityColor != null) {
            this.entityColor.set(red, green, blue, alpha);
        }
    }

    private static void setFloat(@Nullable GlUniformFloat uniform, float value) {
        if (uniform != null) {
            uniform.setFloat(value);
        }
    }

    private static void setMatrix(@Nullable GlUniformMatrix4f uniform, Matrix4fc matrix) {
        if (uniform != null) {
            uniform.set(matrix);
        }
    }
}
