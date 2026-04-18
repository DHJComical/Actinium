package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.shader.uniform.ActiniumCommonUniforms;
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
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt2v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

final class ActiniumPostShaderInterface {
    private final @Nullable GlUniformInt colortex0Sampler;
    private final @Nullable GlUniformInt colortex1Sampler;
    private final @Nullable GlUniformInt colortex2Sampler;
    private final @Nullable GlUniformInt colortex3Sampler;
    private final @Nullable GlUniformInt gaux1Sampler;
    private final @Nullable GlUniformInt gaux2Sampler;
    private final @Nullable GlUniformInt gaux3Sampler;
    private final @Nullable GlUniformInt gaux4Sampler;
    private final @Nullable GlUniformInt depthtex0Sampler;
    private final @Nullable GlUniformInt depthtex1Sampler;
    private final @Nullable GlUniformInt shadowtex0Sampler;
    private final @Nullable GlUniformInt shadowtex1Sampler;
    private final @Nullable GlUniformInt shadowcolor0Sampler;
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
    private final @Nullable GlUniformFloat nearPlane;
    private final @Nullable GlUniformFloat farPlane;
    private final @Nullable GlUniformFloat blindness;
    private final @Nullable GlUniformFloat nightVision;
    private final @Nullable GlUniformFloat fovYInverse;

    private final @Nullable GlUniformInt frameCounter;
    private final @Nullable GlUniformInt worldTime;
    private final @Nullable GlUniformInt moonPhase;
    private final @Nullable GlUniformInt isEyeInWater;
    private final @Nullable GlUniformInt2v eyeBrightnessSmooth;

    private final @Nullable GlUniformFloat3v cameraPosition;
    private final @Nullable GlUniformFloat3v sunPosition;
    private final @Nullable GlUniformFloat3v moonPosition;
    private final @Nullable GlUniformFloat3v shadowLightPosition;
    private final @Nullable GlUniformFloat3v fogColor;
    private final @Nullable GlUniformFloat3v skyColor;

    private final @Nullable GlUniformMatrix4f gbufferModelView;
    private final @Nullable GlUniformMatrix4f gbufferModelViewInverse;
    private final @Nullable GlUniformMatrix4f gbufferProjection;
    private final @Nullable GlUniformMatrix4f gbufferProjectionInverse;
    private final @Nullable GlUniformMatrix4f shadowModelView;
    private final @Nullable GlUniformMatrix4f shadowModelViewInverse;
    private final @Nullable GlUniformMatrix4f shadowProjection;
    private final @Nullable GlUniformMatrix4f shadowProjectionInverse;

    ActiniumPostShaderInterface(ShaderBindingContext context) {
        this.colortex0Sampler = context.bindUniformIfPresent("colortex0", GlUniformInt::new);
        this.colortex1Sampler = context.bindUniformIfPresent("colortex1", GlUniformInt::new);
        this.colortex2Sampler = context.bindUniformIfPresent("colortex2", GlUniformInt::new);
        this.colortex3Sampler = context.bindUniformIfPresent("colortex3", GlUniformInt::new);
        this.gaux1Sampler = context.bindUniformIfPresent("gaux1", GlUniformInt::new);
        this.gaux2Sampler = context.bindUniformIfPresent("gaux2", GlUniformInt::new);
        this.gaux3Sampler = context.bindUniformIfPresent("gaux3", GlUniformInt::new);
        this.gaux4Sampler = context.bindUniformIfPresent("gaux4", GlUniformInt::new);
        this.depthtex0Sampler = context.bindUniformIfPresent("depthtex0", GlUniformInt::new);
        this.depthtex1Sampler = context.bindUniformIfPresent("depthtex1", GlUniformInt::new);
        this.shadowtex0Sampler = context.bindUniformIfPresent("shadowtex0", GlUniformInt::new);
        this.shadowtex1Sampler = context.bindUniformIfPresent("shadowtex1", GlUniformInt::new);
        this.shadowcolor0Sampler = context.bindUniformIfPresent("shadowcolor0", GlUniformInt::new);
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
        this.nearPlane = context.bindUniformIfPresent("near", GlUniformFloat::new);
        this.farPlane = context.bindUniformIfPresent("far", GlUniformFloat::new);
        this.blindness = context.bindUniformIfPresent("blindness", GlUniformFloat::new);
        this.nightVision = context.bindUniformIfPresent("nightVision", GlUniformFloat::new);
        this.fovYInverse = context.bindUniformIfPresent("fovYInverse", GlUniformFloat::new);

        this.frameCounter = context.bindUniformIfPresent("frameCounter", GlUniformInt::new);
        this.worldTime = context.bindUniformIfPresent("worldTime", GlUniformInt::new);
        this.moonPhase = context.bindUniformIfPresent("moonPhase", GlUniformInt::new);
        this.isEyeInWater = context.bindUniformIfPresent("isEyeInWater", GlUniformInt::new);
        this.eyeBrightnessSmooth = context.bindUniformIfPresent("eyeBrightnessSmooth", GlUniformInt2v::new);

        this.cameraPosition = context.bindUniformIfPresent("cameraPosition", GlUniformFloat3v::new);
        this.sunPosition = context.bindUniformIfPresent("sunPosition", GlUniformFloat3v::new);
        this.moonPosition = context.bindUniformIfPresent("moonPosition", GlUniformFloat3v::new);
        this.shadowLightPosition = context.bindUniformIfPresent("shadowLightPosition", GlUniformFloat3v::new);
        this.fogColor = context.bindUniformIfPresent("fogColor", GlUniformFloat3v::new);
        this.skyColor = context.bindUniformIfPresent("skyColor", GlUniformFloat3v::new);

        this.gbufferModelView = context.bindUniformIfPresent("gbufferModelView", GlUniformMatrix4f::new);
        this.gbufferModelViewInverse = context.bindUniformIfPresent("gbufferModelViewInverse", GlUniformMatrix4f::new);
        this.gbufferProjection = context.bindUniformIfPresent("gbufferProjection", GlUniformMatrix4f::new);
        this.gbufferProjectionInverse = context.bindUniformIfPresent("gbufferProjectionInverse", GlUniformMatrix4f::new);
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
        bindSampler(this.gaux1Sampler, 4);
        bindSampler(this.gaux2Sampler, 5);
        bindSampler(this.gaux3Sampler, 6);
        bindSampler(this.gaux4Sampler, 7);
        bindSampler(this.depthtex0Sampler, 8);
        bindSampler(this.depthtex1Sampler, 9);
        bindSampler(this.noisetexSampler, 10);
        bindSampler(this.shadowtex0Sampler, 11);
        bindSampler(this.shadowtex1Sampler, 12);
        bindSampler(this.shadowcolor0Sampler, 13);

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

        if (this.frameCounter != null) {
            this.frameCounter.setInt(frameIndex);
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Entity entity = minecraft.getRenderViewEntity();

        if (entity != null) {
            if (this.cameraPosition != null) {
                this.cameraPosition.set((float) entity.posX, (float) entity.posY, (float) entity.posZ);
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
            float celestialAngle = minecraft.world.getCelestialAngle(partialTicks) * ((float) Math.PI * 2.0f);
            float sunX = -MathHelper.sin(celestialAngle);
            float sunY = MathHelper.cos(celestialAngle);
            float sunZ = 0.0f;

            Vector3f sun = pipeline.transformDirection(sunX, sunY, sunZ);
            Vector3f moon = pipeline.transformDirection(-sunX, -sunY, -sunZ);

            if (this.sunPosition != null) {
                this.sunPosition.set(sun.x, sun.y, sun.z);
            }

            if (this.moonPosition != null) {
                this.moonPosition.set(moon.x, moon.y, moon.z);
            }

            if (this.shadowLightPosition != null) {
                Vector3f light = minecraft.world.isDaytime() ? sun : moon;
                this.shadowLightPosition.set(light.x, light.y, light.z);
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
        }

        setMatrix(this.gbufferModelView, pipeline.getGbufferModelViewMatrix());
        setMatrix(this.gbufferModelViewInverse, pipeline.getGbufferModelViewInverseMatrix());
        setMatrix(this.gbufferProjection, pipeline.getGbufferProjectionMatrix());
        setMatrix(this.gbufferProjectionInverse, pipeline.getGbufferProjectionInverseMatrix());
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
