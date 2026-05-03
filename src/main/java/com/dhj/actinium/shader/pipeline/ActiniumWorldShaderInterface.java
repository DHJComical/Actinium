package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.uniform.ActiniumCapturedRenderingState;
import com.dhj.actinium.shader.uniform.ActiniumCommonUniforms;
import com.dhj.actinium.shader.uniform.ActiniumOptiFineUniforms;
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
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt2v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix3f;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class ActiniumWorldShaderInterface {
    private final @Nullable GlUniformInt texSampler;
    private final @Nullable GlUniformInt gtextureSampler;
    private final @Nullable GlUniformInt lightmapSampler;
    private final @Nullable GlUniformInt normalsSampler;
    private final @Nullable GlUniformInt specularSampler;
    private final @Nullable GlUniformInt gaux1Sampler;
    private final @Nullable GlUniformInt gaux2Sampler;
    private final @Nullable GlUniformInt gaux3Sampler;
    private final @Nullable GlUniformInt gaux4Sampler;
    private final @Nullable GlUniformInt colortex4Sampler;
    private final @Nullable GlUniformInt colortex5Sampler;
    private final @Nullable GlUniformInt colortex6Sampler;
    private final @Nullable GlUniformInt colortex7Sampler;
    private final @Nullable GlUniformInt colortex8Sampler;
    private final @Nullable GlUniformInt colortex9Sampler;
    private final @Nullable GlUniformInt colortex10Sampler;
    private final @Nullable GlUniformInt colortex11Sampler;
    private final @Nullable GlUniformInt colortex12Sampler;
    private final @Nullable GlUniformInt colortex13Sampler;
    private final @Nullable GlUniformInt colortex14Sampler;
    private final @Nullable GlUniformInt colortex15Sampler;
    private final @Nullable GlUniformInt shadowSampler;
    private final @Nullable GlUniformInt watershadowSampler;
    private final @Nullable GlUniformInt shadowtex0Sampler;
    private final @Nullable GlUniformInt shadowtex1Sampler;
    private final @Nullable GlUniformInt shadowcolorSampler;
    private final @Nullable GlUniformInt shadowcolor0Sampler;
    private final @Nullable GlUniformInt shadowcolor1Sampler;
    private final @Nullable GlUniformInt noisetexSampler;
    private final @Nullable GlUniformInt entityId;
    private final @Nullable GlUniformFloat4v entityColor;
    private final @Nullable GlUniformInt blockEntityId;
    private final @Nullable GlUniformInt heldItemId;
    private final @Nullable GlUniformInt heldBlockLightValue;
    private final @Nullable GlUniformInt heldItemId2;
    private final @Nullable GlUniformInt heldBlockLightValue2;

    private final @Nullable GlUniformFloat aspectRatio;
    private final @Nullable GlUniformFloat viewWidth;
    private final @Nullable GlUniformFloat viewHeight;
    private final @Nullable GlUniformFloat pixelSizeX;
    private final @Nullable GlUniformFloat pixelSizeY;
    private final @Nullable GlUniformFloat near;
    private final @Nullable GlUniformFloat far;
    private final @Nullable GlUniformFloat frameTime;
    private final @Nullable GlUniformFloat eyeAltitude;
    private final @Nullable GlUniformFloat screenBrightness;
    private final @Nullable GlUniformFloat rainStrength;
    private final @Nullable GlUniformFloat wetness;
    private final @Nullable GlUniformFloat dayNightMix;
    private final @Nullable GlUniformFloat dayMoment;
    private final @Nullable GlUniformFloat dayMixer;
    private final @Nullable GlUniformFloat nightMixer;
    private final @Nullable GlUniformFloat frameTimeCounter;
    private final @Nullable GlUniformFloat playerMood;
    private final @Nullable GlUniformFloat nightVision;
    private final @Nullable GlUniformFloat blindness;
    private final @Nullable GlUniformFloat darknessFactor;
    private final @Nullable GlUniformFloat darknessLightFactor;
    private final @Nullable GlUniformFloat fogStart;
    private final @Nullable GlUniformFloat fogEnd;
    private final @Nullable GlUniformFloat fogDensity;
    private final @Nullable GlUniformFloat centerDepthSmooth;
    private final @Nullable GlUniformFloat ditherShift;
    private final @Nullable GlUniformFloat sunAngle;
    private final @Nullable GlUniformFloat shadowAngle;

    private final @Nullable GlUniformInt frameCounter;
    private final @Nullable GlUniformInt frameMod;
    private final @Nullable GlUniformFloat frameMod8;
    private final @Nullable GlUniformInt worldTime;
    private final @Nullable GlUniformInt worldDay;
    private final @Nullable GlUniformInt moonPhase;
    private final @Nullable GlUniformInt isEyeInWater;
    private final @Nullable GlUniformInt2v eyeBrightnessSmooth;
    private final @Nullable GlUniformInt2v eyeBrightness;
    private final @Nullable GlUniformInt fogMode;
    private final @Nullable GlUniformInt fogShape;
    private final @Nullable GlUniformInt hideGUI;
    private final @Nullable GlUniformInt2v atlasSize;
    private final @Nullable GlUniformInt2v terrainTextureSize;
    private final @Nullable GlUniformInt terrainIconSize;
    private final @Nullable GlUniformInt4v blendFunc;
    private final @Nullable GlUniformInt instanceId;
    private final @Nullable GlUniformInt renderStage;
    private final @Nullable GlUniformInt bossBattle;

    private final @Nullable GlUniformFloat3v cameraPosition;
    private final @Nullable GlUniformInt3v cameraPositionInt;
    private final @Nullable GlUniformFloat3v cameraPositionFract;
    private final @Nullable GlUniformFloat3v previousCameraPosition;
    private final @Nullable GlUniformInt3v previousCameraPositionInt;
    private final @Nullable GlUniformFloat3v previousCameraPositionFract;
    private final @Nullable GlUniformFloat3v sunPosition;
    private final @Nullable GlUniformFloat3v moonPosition;
    private final @Nullable GlUniformFloat3v shadowLightPosition;
    private final @Nullable GlUniformFloat3v upPosition;
    private final @Nullable GlUniformFloat3v fogColor;
    private final @Nullable GlUniformFloat3v skyColor;
    private final @Nullable GlUniformFloat4v colorModulator;
    private final @Nullable GlUniformFloat4v spriteBounds;

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
    private final @Nullable GlUniformMatrix4f modelViewMatrix;
    private final @Nullable GlUniformMatrix4f modelViewMatrixInverse;
    private final @Nullable GlUniformMatrix4f projectionMatrix;
    private final @Nullable GlUniformMatrix4f projectionMatrixInverse;
    private final @Nullable GlUniformMatrix4f textureMatrix;
    private final @Nullable GlUniformMatrix3f normalMatrix;

    private final Matrix4f scratchModelViewInverse = new Matrix4f();
    private final Matrix3f scratchNormalMatrix = new Matrix3f();
    private final Matrix4f scratchSkyBasisMatrix = new Matrix4f();
    private final Vector3f scratchSunPosition = new Vector3f();
    private final Vector3f scratchMoonPosition = new Vector3f();
    private final Vector3f scratchShadowLightPosition = new Vector3f();
    private final Vector3f scratchUpPosition = new Vector3f();
    private final Vector3f scratchSkyUpBasis = new Vector3f();
    private final Vector4f scratchColorModulator = new Vector4f();
    private boolean entityStateInitialized;
    private int lastEntityId = Integer.MIN_VALUE;
    private float lastEntityRed = Float.NaN;
    private float lastEntityGreen = Float.NaN;
    private float lastEntityBlue = Float.NaN;
    private float lastEntityAlpha = Float.NaN;

    ActiniumWorldShaderInterface(ShaderBindingContext context) {
        this.texSampler = context.bindUniformIfPresent("tex", GlUniformInt::new);
        this.gtextureSampler = context.bindUniformIfPresent("gtexture", GlUniformInt::new);
        this.lightmapSampler = context.bindUniformIfPresent("lightmap", GlUniformInt::new);
        this.normalsSampler = context.bindUniformIfPresent("normals", GlUniformInt::new);
        this.specularSampler = context.bindUniformIfPresent("specular", GlUniformInt::new);
        this.gaux1Sampler = context.bindUniformIfPresent("gaux1", GlUniformInt::new);
        this.gaux2Sampler = context.bindUniformIfPresent("gaux2", GlUniformInt::new);
        this.gaux3Sampler = context.bindUniformIfPresent("gaux3", GlUniformInt::new);
        this.gaux4Sampler = context.bindUniformIfPresent("gaux4", GlUniformInt::new);
        this.colortex4Sampler = context.bindUniformIfPresent("colortex4", GlUniformInt::new);
        this.colortex5Sampler = context.bindUniformIfPresent("colortex5", GlUniformInt::new);
        this.colortex6Sampler = context.bindUniformIfPresent("colortex6", GlUniformInt::new);
        this.colortex7Sampler = context.bindUniformIfPresent("colortex7", GlUniformInt::new);
        this.colortex8Sampler = context.bindUniformIfPresent("colortex8", GlUniformInt::new);
        this.colortex9Sampler = context.bindUniformIfPresent("colortex9", GlUniformInt::new);
        this.colortex10Sampler = context.bindUniformIfPresent("colortex10", GlUniformInt::new);
        this.colortex11Sampler = context.bindUniformIfPresent("colortex11", GlUniformInt::new);
        this.colortex12Sampler = context.bindUniformIfPresent("colortex12", GlUniformInt::new);
        this.colortex13Sampler = context.bindUniformIfPresent("colortex13", GlUniformInt::new);
        this.colortex14Sampler = context.bindUniformIfPresent("colortex14", GlUniformInt::new);
        this.colortex15Sampler = context.bindUniformIfPresent("colortex15", GlUniformInt::new);
        this.shadowSampler = context.bindUniformIfPresent("shadow", GlUniformInt::new);
        this.watershadowSampler = context.bindUniformIfPresent("watershadow", GlUniformInt::new);
        this.shadowtex0Sampler = context.bindUniformIfPresent("shadowtex0", GlUniformInt::new);
        this.shadowtex1Sampler = context.bindUniformIfPresent("shadowtex1", GlUniformInt::new);
        this.shadowcolorSampler = context.bindUniformIfPresent("shadowcolor", GlUniformInt::new);
        this.shadowcolor0Sampler = context.bindUniformIfPresent("shadowcolor0", GlUniformInt::new);
        this.shadowcolor1Sampler = context.bindUniformIfPresent("shadowcolor1", GlUniformInt::new);
        this.noisetexSampler = context.bindUniformIfPresent("noisetex", GlUniformInt::new);
        this.entityId = context.bindUniformIfPresent("entityId", GlUniformInt::new);
        this.entityColor = context.bindUniformIfPresent("entityColor", GlUniformFloat4v::new);
        this.blockEntityId = context.bindUniformIfPresent("blockEntityId", GlUniformInt::new);
        this.heldItemId = context.bindUniformIfPresent("heldItemId", GlUniformInt::new);
        this.heldBlockLightValue = context.bindUniformIfPresent("heldBlockLightValue", GlUniformInt::new);
        this.heldItemId2 = context.bindUniformIfPresent("heldItemId2", GlUniformInt::new);
        this.heldBlockLightValue2 = context.bindUniformIfPresent("heldBlockLightValue2", GlUniformInt::new);

        this.aspectRatio = context.bindUniformIfPresent("aspectRatio", GlUniformFloat::new);
        this.viewWidth = context.bindUniformIfPresent("viewWidth", GlUniformFloat::new);
        this.viewHeight = context.bindUniformIfPresent("viewHeight", GlUniformFloat::new);
        this.pixelSizeX = context.bindUniformIfPresent("pixelSizeX", GlUniformFloat::new);
        this.pixelSizeY = context.bindUniformIfPresent("pixelSizeY", GlUniformFloat::new);
        this.near = context.bindUniformIfPresent("near", GlUniformFloat::new);
        this.far = context.bindUniformIfPresent("far", GlUniformFloat::new);
        this.frameTime = context.bindUniformIfPresent("frameTime", GlUniformFloat::new);
        this.eyeAltitude = context.bindUniformIfPresent("eyeAltitude", GlUniformFloat::new);
        this.screenBrightness = context.bindUniformIfPresent("screenBrightness", GlUniformFloat::new);
        this.rainStrength = context.bindUniformIfPresent("rainStrength", GlUniformFloat::new);
        this.wetness = context.bindUniformIfPresent("wetness", GlUniformFloat::new);
        this.dayNightMix = context.bindUniformIfPresent("dayNightMix", GlUniformFloat::new);
        this.dayMoment = context.bindUniformIfPresent("dayMoment", GlUniformFloat::new);
        this.dayMixer = context.bindUniformIfPresent("dayMixer", GlUniformFloat::new);
        this.nightMixer = context.bindUniformIfPresent("nightMixer", GlUniformFloat::new);
        this.frameTimeCounter = context.bindUniformIfPresent("frameTimeCounter", GlUniformFloat::new);
        this.playerMood = context.bindUniformIfPresent("playerMood", GlUniformFloat::new);
        this.nightVision = context.bindUniformIfPresent("nightVision", GlUniformFloat::new);
        this.blindness = context.bindUniformIfPresent("blindness", GlUniformFloat::new);
        this.darknessFactor = context.bindUniformIfPresent("darknessFactor", GlUniformFloat::new);
        this.darknessLightFactor = context.bindUniformIfPresent("darknessLightFactor", GlUniformFloat::new);
        this.fogStart = context.bindUniformIfPresent("fogStart", GlUniformFloat::new);
        this.fogEnd = context.bindUniformIfPresent("fogEnd", GlUniformFloat::new);
        this.fogDensity = context.bindUniformIfPresent("fogDensity", GlUniformFloat::new);
        this.centerDepthSmooth = context.bindUniformIfPresent("centerDepthSmooth", GlUniformFloat::new);
        this.ditherShift = context.bindUniformIfPresent("ditherShift", GlUniformFloat::new);
        this.sunAngle = context.bindUniformIfPresent("sunAngle", GlUniformFloat::new);
        this.shadowAngle = context.bindUniformIfPresent("shadowAngle", GlUniformFloat::new);

        this.frameCounter = context.bindUniformIfPresent("frameCounter", GlUniformInt::new);
        this.frameMod = context.bindUniformIfPresent("frameMod", GlUniformInt::new);
        this.frameMod8 = context.bindUniformIfPresent("framemod8", GlUniformFloat::new);
        this.worldTime = context.bindUniformIfPresent("worldTime", GlUniformInt::new);
        this.worldDay = context.bindUniformIfPresent("worldDay", GlUniformInt::new);
        this.moonPhase = context.bindUniformIfPresent("moonPhase", GlUniformInt::new);
        this.isEyeInWater = context.bindUniformIfPresent("isEyeInWater", GlUniformInt::new);
        this.eyeBrightnessSmooth = context.bindUniformIfPresent("eyeBrightnessSmooth", GlUniformInt2v::new);
        this.eyeBrightness = context.bindUniformIfPresent("eyeBrightness", GlUniformInt2v::new);
        this.fogMode = context.bindUniformIfPresent("fogMode", GlUniformInt::new);
        this.fogShape = context.bindUniformIfPresent("fogShape", GlUniformInt::new);
        this.hideGUI = context.bindUniformIfPresent("hideGUI", GlUniformInt::new);
        this.atlasSize = context.bindUniformIfPresent("atlasSize", GlUniformInt2v::new);
        this.terrainTextureSize = context.bindUniformIfPresent("terrainTextureSize", GlUniformInt2v::new);
        this.terrainIconSize = context.bindUniformIfPresent("terrainIconSize", GlUniformInt::new);
        this.blendFunc = context.bindUniformIfPresent("blendFunc", GlUniformInt4v::new);
        this.instanceId = context.bindUniformIfPresent("instanceId", GlUniformInt::new);
        this.renderStage = context.bindUniformIfPresent("renderStage", GlUniformInt::new);
        this.bossBattle = context.bindUniformIfPresent("bossBattle", GlUniformInt::new);

        this.cameraPosition = context.bindUniformIfPresent("cameraPosition", GlUniformFloat3v::new);
        this.cameraPositionInt = context.bindUniformIfPresent("cameraPositionInt", GlUniformInt3v::new);
        this.cameraPositionFract = context.bindUniformIfPresent("cameraPositionFract", GlUniformFloat3v::new);
        this.previousCameraPosition = context.bindUniformIfPresent("previousCameraPosition", GlUniformFloat3v::new);
        this.previousCameraPositionInt = context.bindUniformIfPresent("previousCameraPositionInt", GlUniformInt3v::new);
        this.previousCameraPositionFract = context.bindUniformIfPresent("previousCameraPositionFract", GlUniformFloat3v::new);
        this.sunPosition = context.bindUniformIfPresent("sunPosition", GlUniformFloat3v::new);
        this.moonPosition = context.bindUniformIfPresent("moonPosition", GlUniformFloat3v::new);
        this.shadowLightPosition = context.bindUniformIfPresent("shadowLightPosition", GlUniformFloat3v::new);
        this.upPosition = context.bindUniformIfPresent("upPosition", GlUniformFloat3v::new);
        this.fogColor = context.bindUniformIfPresent("fogColor", GlUniformFloat3v::new);
        this.skyColor = context.bindUniformIfPresent("skyColor", GlUniformFloat3v::new);
        this.colorModulator = context.bindUniformIfPresent("colorModulator", GlUniformFloat4v::new);
        this.spriteBounds = context.bindUniformIfPresent("spriteBounds", GlUniformFloat4v::new);

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
        this.modelViewMatrix = context.bindUniformIfPresent("modelViewMatrix", GlUniformMatrix4f::new);
        this.modelViewMatrixInverse = context.bindUniformIfPresent("modelViewMatrixInverse", GlUniformMatrix4f::new);
        this.projectionMatrix = context.bindUniformIfPresent("projectionMatrix", GlUniformMatrix4f::new);
        this.projectionMatrixInverse = context.bindUniformIfPresent("projectionMatrixInverse", GlUniformMatrix4f::new);
        this.textureMatrix = context.bindUniformIfPresent("textureMatrix", GlUniformMatrix4f::new);
        this.normalMatrix = context.bindUniformIfPresent("normalMatrix", GlUniformMatrix3f::new);
    }

    public void setupState(ActiniumRenderPipeline pipeline, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, Matrix4fc projectionInverseMatrix) {
        Matrix4fc referenceModelViewMatrix = modelViewMatrix;
        Matrix4fc referenceProjectionMatrix = projectionMatrix;
        Matrix4fc referenceProjectionInverseMatrix = projectionInverseMatrix;
        boolean useManagedSkyState = pipeline.getCurrentStage() == ActiniumRenderStage.SKY
                || pipeline.getCurrentStage() == ActiniumRenderStage.SKY_TEXTURED
                || pipeline.getCurrentStage() == ActiniumRenderStage.CLOUDS;

        if (useManagedSkyState) {
            referenceModelViewMatrix = pipeline.getSkyStageModelViewMatrix();
            referenceProjectionMatrix = pipeline.getSkyStageProjectionMatrix();
            referenceProjectionInverseMatrix = pipeline.getSkyStageProjectionInverseMatrix();
        }

        if (ActiniumShaderPackManager.isDebugEnabled()
                && pipeline.getCurrentStage() != null
                && (pipeline.getCurrentStage() == ActiniumRenderStage.SKY || pipeline.getCurrentStage() == ActiniumRenderStage.SKY_TEXTURED)
                && pipeline.getFrameCounter() % 60 == 0) {
            this.scratchSkyBasisMatrix.set(referenceModelViewMatrix);
            this.scratchSkyBasisMatrix.transformDirection(0.0f, 1.0f, 0.0f, this.scratchSkyUpBasis);
            ActiniumShaders.logger().info(
                    "[DEBUG] World sky basis stage={} program={} managedSkyState={} referenceUp=[{}, {}, {}] managedUp=[{}, {}, {}] upPositionPresent={}",
                    pipeline.getCurrentStage(),
                    pipeline.getActiveWorldProgramName(),
                    useManagedSkyState,
                    this.scratchSkyUpBasis.x,
                    this.scratchSkyUpBasis.y,
                    this.scratchSkyUpBasis.z,
                    pipeline.getManagedSkyUpPosition().x,
                    pipeline.getManagedSkyUpPosition().y,
                    pipeline.getManagedSkyUpPosition().z,
                    this.upPosition != null
            );
        }

        if (this.texSampler != null) {
            this.texSampler.setInt(0);
        }

        if (this.gtextureSampler != null) {
            this.gtextureSampler.setInt(0);
        }

        if (this.lightmapSampler != null) {
            this.lightmapSampler.setInt(1);
        }

        if (this.normalsSampler != null) {
            this.normalsSampler.setInt(2);
        }

        if (this.specularSampler != null) {
            this.specularSampler.setInt(3);
        }

        if (this.gaux1Sampler != null) {
            this.gaux1Sampler.setInt(ActiniumRenderPipeline.TERRAIN_GAUX1_UNIT);
        }

        if (this.gaux2Sampler != null) {
            this.gaux2Sampler.setInt(ActiniumRenderPipeline.TERRAIN_GAUX2_UNIT);
        }

        if (this.gaux3Sampler != null) {
            this.gaux3Sampler.setInt(ActiniumRenderPipeline.POST_GAUX3_UNIT);
        }

        if (this.gaux4Sampler != null) {
            this.gaux4Sampler.setInt(ActiniumRenderPipeline.WORLD_GAUX4_UNIT);
        }

        if (this.colortex4Sampler != null) {
            this.colortex4Sampler.setInt(ActiniumRenderPipeline.TERRAIN_GAUX1_UNIT);
        }

        if (this.colortex5Sampler != null) {
            this.colortex5Sampler.setInt(ActiniumRenderPipeline.TERRAIN_GAUX2_UNIT);
        }

        if (this.colortex6Sampler != null) {
            this.colortex6Sampler.setInt(ActiniumRenderPipeline.POST_GAUX3_UNIT);
        }

        if (this.colortex7Sampler != null) {
            this.colortex7Sampler.setInt(ActiniumRenderPipeline.WORLD_GAUX4_UNIT);
        }

        if (this.colortex8Sampler != null) {
            this.colortex8Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX8_UNIT);
        }

        if (this.colortex9Sampler != null) {
            this.colortex9Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX9_UNIT);
        }

        if (this.colortex10Sampler != null) {
            this.colortex10Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX10_UNIT);
        }

        if (this.colortex11Sampler != null) {
            this.colortex11Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX11_UNIT);
        }

        if (this.colortex12Sampler != null) {
            this.colortex12Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX12_UNIT);
        }

        if (this.colortex13Sampler != null) {
            this.colortex13Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX13_UNIT);
        }

        if (this.colortex14Sampler != null) {
            this.colortex14Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX14_UNIT);
        }

        if (this.colortex15Sampler != null) {
            this.colortex15Sampler.setInt(ActiniumRenderPipeline.POST_COLORTEX15_UNIT);
        }

        if (this.shadowSampler != null) {
            this.shadowSampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_TEX0_UNIT);
        }

        if (this.watershadowSampler != null) {
            this.watershadowSampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_TEX0_UNIT);
        }

        if (this.shadowtex0Sampler != null) {
            this.shadowtex0Sampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_TEX0_UNIT);
        }

        if (this.shadowtex1Sampler != null) {
            this.shadowtex1Sampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_TEX1_UNIT);
        }

        if (this.shadowcolorSampler != null) {
            this.shadowcolorSampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_COLOR0_UNIT);
        }

        if (this.shadowcolor0Sampler != null) {
            this.shadowcolor0Sampler.setInt(ActiniumRenderPipeline.TERRAIN_SHADOW_COLOR0_UNIT);
        }

        if (this.shadowcolor1Sampler != null) {
            this.shadowcolor1Sampler.setInt(ActiniumRenderPipeline.POST_SHADOW_COLOR1_UNIT);
        }

        if (this.noisetexSampler != null) {
            this.noisetexSampler.setInt(ActiniumRenderPipeline.TERRAIN_NOISETEX_UNIT);
        }

        this.updateEntityState();

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();
        int width = framebuffer != null ? Math.max(1, framebuffer.framebufferWidth) : Math.max(1, minecraft.displayWidth);
        int height = framebuffer != null ? Math.max(1, framebuffer.framebufferHeight) : Math.max(1, minecraft.displayHeight);

        setFloat(this.viewWidth, width);
        setFloat(this.viewHeight, height);
        setFloat(this.pixelSizeX, 1.0f / width);
        setFloat(this.pixelSizeY, 1.0f / height);
        setFloat(this.aspectRatio, width / (float) height);
        setFloat(this.near, 0.05f);
        setFloat(this.far, Math.max(16.0f, minecraft.gameSettings.renderDistanceChunks * 16.0f));
        setFloat(this.frameTime, pipeline.getCurrentFrameDeltaSeconds());
        setFloat(this.screenBrightness, ActiniumOptiFineUniforms.getScreenBrightness());
        setFloat(this.frameTimeCounter, pipeline.getFrameTimeCounterSeconds());
        setFloat(this.darknessFactor, 0.0f);
        setFloat(this.darknessLightFactor, 0.0f);
        setFloat(this.fogStart, ActiniumOptiFineUniforms.getFogStart());
        setFloat(this.fogEnd, ActiniumOptiFineUniforms.getFogEnd());
        setFloat(this.fogDensity, ActiniumOptiFineUniforms.getFogDensity());
        setFloat(this.centerDepthSmooth, pipeline.getCenterDepthSmooth());
        setFloat(this.ditherShift, pipeline.getDitherShift());
        setFloat(this.sunAngle, ActiniumOptiFineUniforms.getSunAngle(minecraft.world, minecraft.getRenderPartialTicks()));
        setFloat(this.shadowAngle, ActiniumOptiFineUniforms.getShadowAngle(minecraft.world, minecraft.getRenderPartialTicks()));

        if (this.frameCounter != null) {
            this.frameCounter.setInt(pipeline.getFrameCounter());
        }

        if (this.frameMod != null) {
            this.frameMod.setInt(pipeline.getFrameMod());
        }

        setFloat(this.frameMod8, pipeline.getFrameMod8());

        if (this.fogMode != null) {
            this.fogMode.setInt(ActiniumOptiFineUniforms.getFogMode());
        }

        if (this.fogShape != null) {
            this.fogShape.setInt(ActiniumOptiFineUniforms.getFogShape());
        }

        if (this.hideGUI != null) {
            this.hideGUI.setInt(ActiniumOptiFineUniforms.isHideGui());
        }

        if (this.atlasSize != null) {
            this.atlasSize.set(ActiniumOptiFineUniforms.getAtlasWidth(), ActiniumOptiFineUniforms.getAtlasHeight());
        }

        if (this.terrainTextureSize != null) {
            this.terrainTextureSize.set(ActiniumOptiFineUniforms.getAtlasWidth(), ActiniumOptiFineUniforms.getAtlasHeight());
        }

        if (this.terrainIconSize != null) {
            this.terrainIconSize.setInt(ActiniumOptiFineUniforms.getTerrainIconSize());
        }

        if (this.blendFunc != null) {
            this.blendFunc.set(
                    ActiniumOptiFineUniforms.getBlendSrcRgb(),
                    ActiniumOptiFineUniforms.getBlendDstRgb(),
                    ActiniumOptiFineUniforms.getBlendSrcAlpha(),
                    ActiniumOptiFineUniforms.getBlendDstAlpha()
            );
        }

        if (this.instanceId != null) {
            this.instanceId.setInt(0);
        }

        if (this.renderStage != null) {
            this.renderStage.setInt(pipeline.getCurrentStage().ordinal());
        }

        if (this.bossBattle != null) {
            this.bossBattle.setInt(0);
        }

        if (this.colorModulator != null) {
            this.scratchColorModulator.set(ActiniumOptiFineUniforms.getCurrentColorModulator());
            this.colorModulator.set(this.scratchColorModulator.x, this.scratchColorModulator.y, this.scratchColorModulator.z, this.scratchColorModulator.w);
        }

        if (this.spriteBounds != null) {
            this.spriteBounds.set(0.0f, 0.0f, 1.0f, 1.0f);
        }


        Entity entity = minecraft.getRenderViewEntity();
        Vec3d currentWorldSkyColor = null;

        if (entity != null) {
            if (this.cameraPosition != null) {
                this.cameraPosition.set(
                        (float) pipeline.getShaderCameraPosition().x,
                        (float) pipeline.getShaderCameraPosition().y,
                        (float) pipeline.getShaderCameraPosition().z
                );
            }

            if (this.cameraPositionInt != null) {
                this.cameraPositionInt.set(
                        pipeline.getCameraPositionIntX(),
                        pipeline.getCameraPositionIntY(),
                        pipeline.getCameraPositionIntZ()
                );
            }

            if (this.cameraPositionFract != null) {
                this.cameraPositionFract.set(
                        pipeline.getCameraPositionFractX(),
                        pipeline.getCameraPositionFractY(),
                        pipeline.getCameraPositionFractZ()
                );
            }

            if (this.previousCameraPosition != null) {
                this.previousCameraPosition.set(
                        (float) pipeline.getPreviousShaderCameraPosition().x,
                        (float) pipeline.getPreviousShaderCameraPosition().y,
                        (float) pipeline.getPreviousShaderCameraPosition().z
                );
            }

            if (this.previousCameraPositionInt != null) {
                this.previousCameraPositionInt.set(
                        (int) Math.floor(pipeline.getPreviousShaderCameraPositionUnshifted().x),
                        (int) Math.floor(pipeline.getPreviousShaderCameraPositionUnshifted().y),
                        (int) Math.floor(pipeline.getPreviousShaderCameraPositionUnshifted().z)
                );
            }

            if (this.previousCameraPositionFract != null) {
                this.previousCameraPositionFract.set(
                        (float) (pipeline.getPreviousShaderCameraPositionUnshifted().x - Math.floor(pipeline.getPreviousShaderCameraPositionUnshifted().x)),
                        (float) (pipeline.getPreviousShaderCameraPositionUnshifted().y - Math.floor(pipeline.getPreviousShaderCameraPositionUnshifted().y)),
                        (float) (pipeline.getPreviousShaderCameraPositionUnshifted().z - Math.floor(pipeline.getPreviousShaderCameraPositionUnshifted().z))
                );
            }

            if (this.isEyeInWater != null) {
                this.isEyeInWater.setInt(entity.isInsideOfMaterial(Material.WATER) ? 1 : 0);
            }

            if (this.eyeBrightnessSmooth != null) {
                int brightness = entity.getBrightnessForRender();
                this.eyeBrightnessSmooth.set(brightness & 0xFFFF, brightness >>> 16);
            }

            if (this.eyeBrightness != null) {
                int brightness = entity.getBrightnessForRender();
                this.eyeBrightness.set(brightness & 0xFFFF, brightness >>> 16);
            }

            setFloat(this.eyeAltitude, ActiniumOptiFineUniforms.getEyeAltitude(entity));
            setFloat(this.playerMood, ActiniumOptiFineUniforms.getPlayerMood(entity));
            setFloat(this.nightVision, getNightVisionStrength(entity));
            setFloat(this.blindness, getBlindnessStrength(entity));

            if (this.heldItemId != null) {
                this.heldItemId.setInt(ActiniumOptiFineUniforms.getHeldItemId(entity, false));
            }

            if (this.heldBlockLightValue != null) {
                this.heldBlockLightValue.setInt(ActiniumOptiFineUniforms.getHeldBlockLightValue(entity, false));
            }

            if (this.heldItemId2 != null) {
                this.heldItemId2.setInt(ActiniumOptiFineUniforms.getHeldItemId(entity, true));
            }

            if (this.heldBlockLightValue2 != null) {
                this.heldBlockLightValue2.setInt(ActiniumOptiFineUniforms.getHeldBlockLightValue(entity, true));
            }

            if (minecraft.world != null) {
                float partialTicks = minecraft.getRenderPartialTicks();
                currentWorldSkyColor = minecraft.world.getSkyColor(entity, partialTicks);
            }

            if (this.skyColor != null) {
                if (useManagedSkyState) {
                    Vector3f managedSkyColor = pipeline.getManagedSkyColor();
                    this.skyColor.set(managedSkyColor.x, managedSkyColor.y, managedSkyColor.z);
                } else if (pipeline.hasCapturedSkyColor()) {
                    Vector3f capturedSkyColor = pipeline.getCapturedSkyColor();
                    this.skyColor.set(capturedSkyColor.x, capturedSkyColor.y, capturedSkyColor.z);
                } else if (currentWorldSkyColor != null) {
                    this.skyColor.set((float) currentWorldSkyColor.x, (float) currentWorldSkyColor.y, (float) currentWorldSkyColor.z);
                }
            }
        }

        if (minecraft.world != null) {
            int currentWorldTime = ActiniumCommonUniforms.getWorldTime(minecraft.world);
            float currentDayMoment = ActiniumCommonUniforms.getDayMoment(currentWorldTime);
            float dayNight = ActiniumCommonUniforms.getDayNightMix(currentWorldTime);
            float partialTicks = minecraft.getRenderPartialTicks();
            boolean useManagedCelestialState = useManagedSkyState && pipeline.hasManagedSkyCelestialState();

            if (useManagedCelestialState) {
                this.scratchSunPosition.set(pipeline.getManagedSkySunPosition());
                this.scratchMoonPosition.set(pipeline.getManagedSkyMoonPosition());
                this.scratchShadowLightPosition.set(pipeline.getManagedSkyShadowLightPosition());
            } else {
                pipeline.fillShaderCoreCelestialUniforms(
                        referenceModelViewMatrix,
                        partialTicks,
                        this.scratchSunPosition,
                        this.scratchMoonPosition,
                        this.scratchShadowLightPosition
                );
            }

            if (this.worldTime != null) {
                this.worldTime.setInt(currentWorldTime);
            }

            if (this.worldDay != null) {
                this.worldDay.setInt(ActiniumOptiFineUniforms.getWorldDay(minecraft.world));
            }

            if (this.moonPhase != null) {
                this.moonPhase.setInt(ActiniumCommonUniforms.getMoonPhase(minecraft.world));
            }

            setFloat(this.rainStrength, minecraft.world.getRainStrength(0.0f));
            setFloat(this.wetness, ActiniumOptiFineUniforms.getWetness(minecraft.world));
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
                        : this.scratchShadowLightPosition;
                this.shadowLightPosition.set(light.x, light.y, light.z);
            }

            if (useManagedSkyState) {
                Vector3f managedUpPosition = pipeline.getManagedSkyUpPosition();
                this.scratchUpPosition.set(managedUpPosition.x, managedUpPosition.y, managedUpPosition.z);
            } else {
                pipeline.fillShaderCoreUpPosition(this.scratchUpPosition);
            }

            if (this.upPosition != null) {
                this.upPosition.set(this.scratchUpPosition.x, this.scratchUpPosition.y, this.scratchUpPosition.z);
            }
        }

        if (this.blockEntityId != null) {
            this.blockEntityId.setInt(ActiniumOptiFineUniforms.getBlockEntityId());
        }

        float[] currentFogColor = pipeline.getFogColor();
        float fogRed = currentFogColor[0];
        float fogGreen = currentFogColor[1];
        float fogBlue = currentFogColor[2];

        if (this.fogColor != null) {
            this.fogColor.set(fogRed, fogGreen, fogBlue);
        }

        if (ActiniumShaderPackManager.isDebugEnabled()
                && pipeline.getCurrentStage() != null
                && (pipeline.getCurrentStage() == ActiniumRenderStage.SKY || pipeline.getCurrentStage() == ActiniumRenderStage.SKY_TEXTURED)
                && pipeline.getFrameCounter() % 60 == 0) {
            Vector3f currentSky = pipeline.getManagedSkyColor();
            ActiniumShaders.logger().info(
                    "[DEBUG] World sky uniforms stage={} program={} sky=[{}, {}, {}] fog=[{}, {}, {}] managedUp=[{}, {}, {}] computedUp=[{}, {}, {}] upUniformPresent={} sun=[{}, {}, {}] moon=[{}, {}, {}]",
                    pipeline.getCurrentStage(),
                    pipeline.getActiveWorldProgramName(),
                    currentSky.x,
                    currentSky.y,
                    currentSky.z,
                    fogRed,
                    fogGreen,
                    fogBlue,
                    pipeline.getManagedSkyUpPosition().x,
                    pipeline.getManagedSkyUpPosition().y,
                    pipeline.getManagedSkyUpPosition().z,
                    this.scratchUpPosition.x,
                    this.scratchUpPosition.y,
                    this.scratchUpPosition.z,
                    this.upPosition != null,
                    this.scratchSunPosition.x,
                    this.scratchSunPosition.y,
                    this.scratchSunPosition.z,
                    this.scratchMoonPosition.x,
                    this.scratchMoonPosition.y,
                    this.scratchMoonPosition.z
            );
        }

        setMatrix(this.gbufferModelView, referenceModelViewMatrix);
        setMatrix(this.gbufferProjection, referenceProjectionMatrix);
        setMatrix(this.gbufferPreviousModelView, pipeline.getPreviousGbufferModelViewMatrix());
        setMatrix(this.gbufferPreviousProjection, pipeline.getPreviousGbufferProjectionMatrix());

        if (this.gbufferModelViewInverse != null || this.normalMatrix != null) {
            this.scratchModelViewInverse.set(referenceModelViewMatrix).invert();

            if (this.gbufferModelViewInverse != null) {
                this.gbufferModelViewInverse.set(this.scratchModelViewInverse);
            }
        }

        if (this.normalMatrix != null) {
            this.scratchNormalMatrix.set(this.scratchModelViewInverse).transpose();
            this.normalMatrix.set(this.scratchNormalMatrix);
        }

        setMatrix(this.modelViewMatrix, referenceModelViewMatrix);
        setMatrix(this.modelViewMatrixInverse, this.scratchModelViewInverse);
        setMatrix(this.projectionMatrix, referenceProjectionMatrix);
        setMatrix(this.projectionMatrixInverse, referenceProjectionInverseMatrix);

        if (this.textureMatrix != null) {
            this.textureMatrix.set(new Matrix4f());
        }

        setMatrix(this.gbufferProjectionInverse, referenceProjectionInverseMatrix);
        setMatrix(this.shadowModelView, pipeline.getShadowModelViewMatrix());
        setMatrix(this.shadowModelViewInverse, pipeline.getShadowModelViewInverseMatrix());
        setMatrix(this.shadowProjection, pipeline.getShadowProjectionMatrix());
        setMatrix(this.shadowProjectionInverse, pipeline.getShadowProjectionInverseMatrix());
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

    public void updateEntityState() {
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

    private static void setMatrix(@Nullable GlUniformMatrix4f uniform, Matrix4fc value) {
        if (uniform != null) {
            uniform.set(value);
        }
    }

}
