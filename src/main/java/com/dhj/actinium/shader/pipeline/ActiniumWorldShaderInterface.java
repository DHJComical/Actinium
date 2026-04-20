package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.uniform.ActiniumCommonUniforms;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
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
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

final class ActiniumWorldShaderInterface {
    private final @Nullable GlUniformInt texSampler;
    private final @Nullable GlUniformInt lightmapSampler;
    private final @Nullable GlUniformInt gaux4Sampler;
    private final @Nullable GlUniformInt shadowtex0Sampler;
    private final @Nullable GlUniformInt shadowtex1Sampler;
    private final @Nullable GlUniformInt shadowcolor0Sampler;

    private final @Nullable GlUniformFloat viewWidth;
    private final @Nullable GlUniformFloat viewHeight;
    private final @Nullable GlUniformFloat pixelSizeX;
    private final @Nullable GlUniformFloat pixelSizeY;
    private final @Nullable GlUniformFloat far;
    private final @Nullable GlUniformFloat rainStrength;
    private final @Nullable GlUniformFloat dayNightMix;
    private final @Nullable GlUniformFloat dayMoment;
    private final @Nullable GlUniformFloat dayMixer;
    private final @Nullable GlUniformFloat nightMixer;
    private final @Nullable GlUniformFloat frameTimeCounter;
    private final @Nullable GlUniformFloat nightVision;
    private final @Nullable GlUniformFloat blindness;
    private final @Nullable GlUniformFloat darknessFactor;
    private final @Nullable GlUniformFloat darknessLightFactor;
    private final @Nullable GlUniformFloat ditherShift;

    private final @Nullable GlUniformInt frameCounter;
    private final @Nullable GlUniformInt frameMod;
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
    private final @Nullable GlUniformMatrix4f gbufferProjectionInverse;
    private final @Nullable GlUniformMatrix4f shadowModelView;
    private final @Nullable GlUniformMatrix4f shadowProjection;

    private final Matrix4f scratchModelViewInverse = new Matrix4f();
    private final Vector3f scratchSunPosition = new Vector3f();
    private final Vector3f scratchMoonPosition = new Vector3f();

    ActiniumWorldShaderInterface(ShaderBindingContext context) {
        this.texSampler = context.bindUniformIfPresent("tex", GlUniformInt::new);
        this.lightmapSampler = context.bindUniformIfPresent("lightmap", GlUniformInt::new);
        this.gaux4Sampler = context.bindUniformIfPresent("gaux4", GlUniformInt::new);
        this.shadowtex0Sampler = context.bindUniformIfPresent("shadowtex0", GlUniformInt::new);
        this.shadowtex1Sampler = context.bindUniformIfPresent("shadowtex1", GlUniformInt::new);
        this.shadowcolor0Sampler = context.bindUniformIfPresent("shadowcolor0", GlUniformInt::new);

        this.viewWidth = context.bindUniformIfPresent("viewWidth", GlUniformFloat::new);
        this.viewHeight = context.bindUniformIfPresent("viewHeight", GlUniformFloat::new);
        this.pixelSizeX = context.bindUniformIfPresent("pixelSizeX", GlUniformFloat::new);
        this.pixelSizeY = context.bindUniformIfPresent("pixelSizeY", GlUniformFloat::new);
        this.far = context.bindUniformIfPresent("far", GlUniformFloat::new);
        this.rainStrength = context.bindUniformIfPresent("rainStrength", GlUniformFloat::new);
        this.dayNightMix = context.bindUniformIfPresent("dayNightMix", GlUniformFloat::new);
        this.dayMoment = context.bindUniformIfPresent("dayMoment", GlUniformFloat::new);
        this.dayMixer = context.bindUniformIfPresent("dayMixer", GlUniformFloat::new);
        this.nightMixer = context.bindUniformIfPresent("nightMixer", GlUniformFloat::new);
        this.frameTimeCounter = context.bindUniformIfPresent("frameTimeCounter", GlUniformFloat::new);
        this.nightVision = context.bindUniformIfPresent("nightVision", GlUniformFloat::new);
        this.blindness = context.bindUniformIfPresent("blindness", GlUniformFloat::new);
        this.darknessFactor = context.bindUniformIfPresent("darknessFactor", GlUniformFloat::new);
        this.darknessLightFactor = context.bindUniformIfPresent("darknessLightFactor", GlUniformFloat::new);
        this.ditherShift = context.bindUniformIfPresent("ditherShift", GlUniformFloat::new);

        this.frameCounter = context.bindUniformIfPresent("frameCounter", GlUniformInt::new);
        this.frameMod = context.bindUniformIfPresent("frameMod", GlUniformInt::new);
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
        this.gbufferProjectionInverse = context.bindUniformIfPresent("gbufferProjectionInverse", GlUniformMatrix4f::new);
        this.shadowModelView = context.bindUniformIfPresent("shadowModelView", GlUniformMatrix4f::new);
        this.shadowProjection = context.bindUniformIfPresent("shadowProjection", GlUniformMatrix4f::new);
    }

    public void setupState(ActiniumRenderPipeline pipeline, Matrix4fc modelViewMatrix, Matrix4fc projectionInverseMatrix) {
        Matrix4fc referenceModelViewMatrix = modelViewMatrix;
        Matrix4fc referenceProjectionInverseMatrix = projectionInverseMatrix;

        if (this.texSampler != null) {
            this.texSampler.setInt(0);
        }

        if (this.lightmapSampler != null) {
            this.lightmapSampler.setInt(1);
        }

        if (this.gaux4Sampler != null) {
            this.gaux4Sampler.setInt(ActiniumRenderPipeline.WORLD_GAUX4_UNIT);
        }

        if (this.shadowtex0Sampler != null) {
            this.shadowtex0Sampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_TEX0_UNIT);
        }

        if (this.shadowtex1Sampler != null) {
            this.shadowtex1Sampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_TEX1_UNIT);
        }

        if (this.shadowcolor0Sampler != null) {
            this.shadowcolor0Sampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_COLOR0_UNIT);
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();
        int width = framebuffer != null ? Math.max(1, framebuffer.framebufferWidth) : Math.max(1, minecraft.displayWidth);
        int height = framebuffer != null ? Math.max(1, framebuffer.framebufferHeight) : Math.max(1, minecraft.displayHeight);

        setFloat(this.viewWidth, width);
        setFloat(this.viewHeight, height);
        setFloat(this.pixelSizeX, 1.0f / width);
        setFloat(this.pixelSizeY, 1.0f / height);
        setFloat(this.far, Math.max(16.0f, minecraft.gameSettings.renderDistanceChunks * 16.0f));
        setFloat(this.frameTimeCounter, pipeline.getFrameTimeCounterSeconds());
        setFloat(this.darknessFactor, 0.0f);
        setFloat(this.darknessLightFactor, 0.0f);
        setFloat(this.ditherShift, pipeline.getDitherShift());

        if (this.frameCounter != null) {
            this.frameCounter.setInt(pipeline.getFrameCounter());
        }

        if (this.frameMod != null) {
            this.frameMod.setInt(pipeline.getFrameMod());
        }


        Entity entity = minecraft.getRenderViewEntity();
        Vec3d currentWorldSkyColor = null;
        Vec3d currentWorldFogColor = null;
        boolean useManagedSkyState = pipeline.getCurrentStage() == ActiniumRenderStage.SKY
                || pipeline.getCurrentStage() == ActiniumRenderStage.SKY_TEXTURED
                || pipeline.getCurrentStage() == ActiniumRenderStage.CLOUDS;

        if (entity != null) {
            if (this.cameraPosition != null) {
                this.cameraPosition.set(
                        (float) pipeline.getWorldCameraPosition().x,
                        (float) pipeline.getWorldCameraPosition().y,
                        (float) pipeline.getWorldCameraPosition().z
                );
            }

            if (this.isEyeInWater != null) {
                this.isEyeInWater.setInt(entity.isInsideOfMaterial(Material.WATER) ? 1 : 0);
            }

            if (this.eyeBrightnessSmooth != null) {
                int brightness = entity.getBrightnessForRender();
                this.eyeBrightnessSmooth.set(brightness & 0xFFFF, brightness >>> 16);
            }

            setFloat(this.nightVision, getNightVisionStrength(entity));
            setFloat(this.blindness, getBlindnessStrength(entity));

            if (minecraft.world != null) {
                float partialTicks = minecraft.getRenderPartialTicks();
                currentWorldSkyColor = minecraft.world.getSkyColor(entity, partialTicks);
                currentWorldFogColor = minecraft.world.getFogColor(partialTicks);
            }

            if (this.skyColor != null) {
                if (useManagedSkyState) {
                    Vector3f managedSkyColor = pipeline.getManagedSkyColor();
                    this.skyColor.set(managedSkyColor.x, managedSkyColor.y, managedSkyColor.z);
                } else if (currentWorldSkyColor != null) {
                    this.skyColor.set((float) currentWorldSkyColor.x, (float) currentWorldSkyColor.y, (float) currentWorldSkyColor.z);
                }
            }
        }

        if (minecraft.world != null) {
            int currentWorldTime = ActiniumCommonUniforms.getWorldTime(minecraft.world);
            float currentDayMoment = ActiniumCommonUniforms.getDayMoment(currentWorldTime);
            float dayNight = ActiniumCommonUniforms.getDayNightMix(currentWorldTime);
            boolean useManagedCelestialState = useManagedSkyState && pipeline.hasManagedSkyCelestialState();

            if (useManagedCelestialState) {
                this.scratchSunPosition.set(pipeline.getManagedSkySunPosition());
                this.scratchMoonPosition.set(pipeline.getManagedSkyMoonPosition());
            } else {
                float celestialAngle = minecraft.world.getCelestialAngle(minecraft.getRenderPartialTicks());
                float sunPathRotation = ActiniumShaderPackManager.getActiveShaderProperties().getSunPathRotation();

                new Matrix4f(referenceModelViewMatrix)
                        .rotateY((float) Math.toRadians(-90.0f))
                        .rotateZ((float) Math.toRadians(sunPathRotation))
                        .rotateX(celestialAngle * ((float) Math.PI * 2.0f))
                        .transformDirection(0.0f, 100.0f, 0.0f, this.scratchSunPosition);
                new Matrix4f(referenceModelViewMatrix)
                        .rotateY((float) Math.toRadians(-90.0f))
                        .rotateZ((float) Math.toRadians(sunPathRotation))
                        .rotateX(celestialAngle * ((float) Math.PI * 2.0f))
                        .transformDirection(0.0f, -100.0f, 0.0f, this.scratchMoonPosition);
            }

            if (this.worldTime != null) {
                this.worldTime.setInt(currentWorldTime);
            }

            if (this.moonPhase != null) {
                this.moonPhase.setInt(ActiniumCommonUniforms.getMoonPhase(minecraft.world));
            }

            setFloat(this.rainStrength, minecraft.world.getRainStrength(0.0f));
            setFloat(this.dayNightMix, dayNight);
            setFloat(this.dayMoment, currentDayMoment);
            setFloat(this.dayMixer, ActiniumCommonUniforms.getDayMixer(currentDayMoment));
            setFloat(this.nightMixer, ActiniumCommonUniforms.getNightMixer(currentDayMoment));

            if (this.sunPosition != null) {
                this.sunPosition.set(this.scratchSunPosition.x, this.scratchSunPosition.y, this.scratchSunPosition.z);
            }

            if (this.moonPosition != null) {
                this.moonPosition.set(this.scratchMoonPosition.x, this.scratchMoonPosition.y, this.scratchMoonPosition.z);
            }

            if (this.shadowLightPosition != null) {
                Vector3f light = useManagedCelestialState
                        ? pipeline.getManagedSkyShadowLightPosition()
                        : dayNight >= 0.5f ? this.scratchSunPosition : this.scratchMoonPosition;
                this.shadowLightPosition.set(light.x, light.y, light.z);
            }
        }

        float[] currentFogColor = pipeline.getFogColor();
        float fogRed = currentFogColor[0];
        float fogGreen = currentFogColor[1];
        float fogBlue = currentFogColor[2];

        if ((pipeline.getCurrentStage() == ActiniumRenderStage.SKY || pipeline.getCurrentStage() == ActiniumRenderStage.SKY_TEXTURED)
                && currentWorldFogColor != null) {
            fogRed = (float) currentWorldFogColor.x;
            fogGreen = (float) currentWorldFogColor.y;
            fogBlue = (float) currentWorldFogColor.z;
        }

        if (this.fogColor != null) {
            this.fogColor.set(fogRed, fogGreen, fogBlue);
        }

        setMatrix(this.gbufferModelView, referenceModelViewMatrix);

        if (this.gbufferModelViewInverse != null) {
            this.scratchModelViewInverse.set(referenceModelViewMatrix).invert();
            this.gbufferModelViewInverse.set(this.scratchModelViewInverse);
        }

        setMatrix(this.gbufferProjectionInverse, referenceProjectionInverseMatrix);
        setMatrix(this.shadowModelView, pipeline.getShadowModelViewMatrix());
        setMatrix(this.shadowProjection, pipeline.getShadowProjectionMatrix());
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

    private static float getBlindnessStrength(Entity entity) {
        if (!(entity instanceof EntityLivingBase living)) {
            return 0.0f;
        }

        return living.getActivePotionEffect(MobEffects.BLINDNESS) != null ? 1.0f : 0.0f;
    }

    private static void setFloat(@Nullable GlUniformFloat uniform, float value) {
        if (uniform != null) {
            uniform.setFloat(value);
        }
    }

    private static void setMatrix(@Nullable GlUniformMatrix4f uniform, Matrix4fc value) {
        if (uniform != null) {
            uniform.set(value);
        }
    }

}
