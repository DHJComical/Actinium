package com.dhj.actinium.celeritas.shader_overrides;

import com.dhj.actinium.shadows.ActiniumShadowMatrixAccess;
import com.dhj.actinium.shadows.ActiniumShadowRenderingState;
import com.dhj.actinium.shader.pipeline.ActiniumRenderPipeline;
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
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloatArray;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt2v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix3f;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.lwjgl.MemoryStack;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

final class ActiniumChunkShaderInterface implements ChunkShaderInterface {
    private static final long START_TIME_NANOS = System.nanoTime();
    private static final long MAX_CHUNK_AGE = TimeUnit.SECONDS.toMillis(30);

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;
    private final @Nullable GlUniformFloatArray uniformChunkAges;
    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;
    private final List<? extends ChunkShaderComponent> components;

    private final @Nullable GlUniformMatrix4f irisModelViewMatrix;
    private final @Nullable GlUniformMatrix4f irisModelViewMatrixInverse;
    private final @Nullable GlUniformMatrix4f irisModelViewMatrixInv;
    private final @Nullable GlUniformMatrix4f irisProjectionMatrix;
    private final @Nullable GlUniformMatrix4f irisProjectionMatrixInverse;
    private final @Nullable GlUniformMatrix4f irisProjectionMatrixInv;
    private final @Nullable GlUniformMatrix3f irisNormalMatrix;
    private final @Nullable GlUniformMatrix3f irisNormalMat;
    private final @Nullable GlUniformFloat3v irisRegionOffset;
    private final @Nullable GlUniformFloat4v irisFogColor;
    private final @Nullable GlUniformFloat irisFogDensity;
    private final @Nullable GlUniformFloat irisFogStart;
    private final @Nullable GlUniformFloat irisFogEnd;
    private final @Nullable GlUniformFloat irisCurrentAlphaTest;

    private final @Nullable GlUniformMatrix4f gbufferModelView;
    private final @Nullable GlUniformMatrix4f gbufferModelViewInverse;
    private final @Nullable GlUniformMatrix4f gbufferProjection;
    private final @Nullable GlUniformMatrix4f gbufferProjectionInverse;
    private final @Nullable GlUniformMatrix4f shadowModelView;
    private final @Nullable GlUniformMatrix4f shadowModelViewInverse;
    private final @Nullable GlUniformMatrix4f shadowProjection;
    private final @Nullable GlUniformMatrix4f shadowProjectionInverse;
    private final @Nullable GlUniformFloat3v legacyFogColor;
    private final @Nullable GlUniformFloat3v legacySkyColor;
    private final @Nullable GlUniformFloat3v cameraPosition;
    private final @Nullable GlUniformFloat3v sunPosition;
    private final @Nullable GlUniformFloat3v moonPosition;
    private final @Nullable GlUniformFloat3v shadowLightPosition;
    private final @Nullable GlUniformFloat viewWidth;
    private final @Nullable GlUniformFloat viewHeight;
    private final @Nullable GlUniformFloat pixelSizeX;
    private final @Nullable GlUniformFloat pixelSizeY;
    private final @Nullable GlUniformFloat nearPlane;
    private final @Nullable GlUniformFloat far;
    private final @Nullable GlUniformFloat rainStrength;
    private final @Nullable GlUniformFloat frameTimeCounter;
    private final @Nullable GlUniformInt frameCounter;
    private final @Nullable GlUniformInt worldTime;
    private final @Nullable GlUniformFloat dayNightMix;
    private final @Nullable GlUniformFloat dayMoment;
    private final @Nullable GlUniformFloat dayMixer;
    private final @Nullable GlUniformFloat nightMixer;
    private final @Nullable GlUniformInt moonPhase;
    private final @Nullable GlUniformInt isEyeInWater;
    private final @Nullable GlUniformFloat nightVision;
    private final @Nullable GlUniformFloat blindness;
    private final @Nullable GlUniformFloat darknessFactor;
    private final @Nullable GlUniformFloat darknessLightFactor;
    private final @Nullable GlUniformInt2v eyeBrightnessSmooth;
    private final @Nullable GlUniformInt texSampler;
    private final @Nullable GlUniformInt lightmapSampler;
    private final @Nullable GlUniformInt gaux1Sampler;
    private final @Nullable GlUniformInt gaux2Sampler;
    private final @Nullable GlUniformInt gaux4Sampler;
    private final @Nullable GlUniformInt depthtex0Sampler;
    private final @Nullable GlUniformInt depthtex1Sampler;
    private final @Nullable GlUniformInt noisetexSampler;
    private final @Nullable GlUniformInt shadowtex0Sampler;
    private final @Nullable GlUniformInt shadowtex1Sampler;
    private final @Nullable GlUniformInt shadowcolor0Sampler;

    private final float defaultAlphaTest;
    private final boolean usesTerrainInputs;
    private GlPrimitiveType primitiveType;

    private final Matrix4f currentModelView = new Matrix4f();
    private final Matrix4f currentProjection = new Matrix4f();
    private final Matrix4f scratchModelViewInverse = new Matrix4f();
    private final Matrix4f scratchProjectionInverse = new Matrix4f();
    private final Matrix4f scratchShadowModelView = new Matrix4f();
    private final Matrix4f scratchShadowProjection = new Matrix4f();
    private final Matrix4f scratchShadowModelViewInverse = new Matrix4f();
    private final Matrix4f scratchShadowProjectionInverse = new Matrix4f();
    private final Matrix3f scratchNormalMatrix = new Matrix3f();
    private final Vector3f scratchSunPosition = new Vector3f();
    private final Vector3f scratchMoonPosition = new Vector3f();

    private boolean shadowPassActive;
    private int legacyFrameCounter;

    public ActiniumChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);
        this.uniformChunkAges = context.bindUniformIfPresent("celeritas_ChunkAges", GlUniformFloatArray::new);
        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        bindTextureUniform(this.uniformTextures, ChunkShaderTextureSlot.BLOCK, context, "u_BlockTex", "tex");
        if (!options.pass().hasNoLightmap()) {
            bindTextureUniform(this.uniformTextures, ChunkShaderTextureSlot.LIGHT, context, "u_LightTex", "lightmap");
        }

        this.components = options.components().stream().map(c -> c.create(context)).toList();
        this.irisModelViewMatrix = context.bindUniformIfPresent("iris_ModelViewMatrix", GlUniformMatrix4f::new);
        this.irisModelViewMatrixInverse = context.bindUniformIfPresent("iris_ModelViewMatrixInverse", GlUniformMatrix4f::new);
        this.irisModelViewMatrixInv = context.bindUniformIfPresent("iris_ModelViewMatrixInv", GlUniformMatrix4f::new);
        this.irisProjectionMatrix = context.bindUniformIfPresent("iris_ProjectionMatrix", GlUniformMatrix4f::new);
        this.irisProjectionMatrixInverse = context.bindUniformIfPresent("iris_ProjectionMatrixInverse", GlUniformMatrix4f::new);
        this.irisProjectionMatrixInv = context.bindUniformIfPresent("iris_ProjectionMatrixInv", GlUniformMatrix4f::new);
        this.irisNormalMatrix = context.bindUniformIfPresent("iris_NormalMatrix", GlUniformMatrix3f::new);
        this.irisNormalMat = context.bindUniformIfPresent("iris_NormalMat", GlUniformMatrix3f::new);
        this.irisRegionOffset = context.bindUniformIfPresent("iris_RegionOffset", GlUniformFloat3v::new);
        this.irisFogColor = context.bindUniformIfPresent("iris_FogColor", GlUniformFloat4v::new);
        this.irisFogDensity = context.bindUniformIfPresent("iris_FogDensity", GlUniformFloat::new);
        this.irisFogStart = context.bindUniformIfPresent("iris_FogStart", GlUniformFloat::new);
        this.irisFogEnd = context.bindUniformIfPresent("iris_FogEnd", GlUniformFloat::new);
        this.irisCurrentAlphaTest = context.bindUniformIfPresent("iris_currentAlphaTest", GlUniformFloat::new);

        this.gbufferModelView = context.bindUniformIfPresent("gbufferModelView", GlUniformMatrix4f::new);
        this.gbufferModelViewInverse = context.bindUniformIfPresent("gbufferModelViewInverse", GlUniformMatrix4f::new);
        this.gbufferProjection = context.bindUniformIfPresent("gbufferProjection", GlUniformMatrix4f::new);
        this.gbufferProjectionInverse = context.bindUniformIfPresent("gbufferProjectionInverse", GlUniformMatrix4f::new);
        this.shadowModelView = context.bindUniformIfPresent("shadowModelView", GlUniformMatrix4f::new);
        this.shadowModelViewInverse = context.bindUniformIfPresent("shadowModelViewInverse", GlUniformMatrix4f::new);
        this.shadowProjection = context.bindUniformIfPresent("shadowProjection", GlUniformMatrix4f::new);
        this.shadowProjectionInverse = context.bindUniformIfPresent("shadowProjectionInverse", GlUniformMatrix4f::new);
        this.legacyFogColor = context.bindUniformIfPresent("fogColor", GlUniformFloat3v::new);
        this.legacySkyColor = context.bindUniformIfPresent("skyColor", GlUniformFloat3v::new);
        this.cameraPosition = context.bindUniformIfPresent("cameraPosition", GlUniformFloat3v::new);
        this.sunPosition = context.bindUniformIfPresent("sunPosition", GlUniformFloat3v::new);
        this.moonPosition = context.bindUniformIfPresent("moonPosition", GlUniformFloat3v::new);
        this.shadowLightPosition = context.bindUniformIfPresent("shadowLightPosition", GlUniformFloat3v::new);
        this.viewWidth = context.bindUniformIfPresent("viewWidth", GlUniformFloat::new);
        this.viewHeight = context.bindUniformIfPresent("viewHeight", GlUniformFloat::new);
        this.pixelSizeX = context.bindUniformIfPresent("pixelSizeX", GlUniformFloat::new);
        this.pixelSizeY = context.bindUniformIfPresent("pixelSizeY", GlUniformFloat::new);
        this.nearPlane = context.bindUniformIfPresent("near", GlUniformFloat::new);
        this.far = context.bindUniformIfPresent("far", GlUniformFloat::new);
        this.rainStrength = context.bindUniformIfPresent("rainStrength", GlUniformFloat::new);
        this.frameTimeCounter = context.bindUniformIfPresent("frameTimeCounter", GlUniformFloat::new);
        this.frameCounter = context.bindUniformIfPresent("frameCounter", GlUniformInt::new);
        this.worldTime = context.bindUniformIfPresent("worldTime", GlUniformInt::new);
        this.dayNightMix = context.bindUniformIfPresent("dayNightMix", GlUniformFloat::new);
        this.dayMoment = context.bindUniformIfPresent("dayMoment", GlUniformFloat::new);
        this.dayMixer = context.bindUniformIfPresent("dayMixer", GlUniformFloat::new);
        this.nightMixer = context.bindUniformIfPresent("nightMixer", GlUniformFloat::new);
        this.moonPhase = context.bindUniformIfPresent("moonPhase", GlUniformInt::new);
        this.isEyeInWater = context.bindUniformIfPresent("isEyeInWater", GlUniformInt::new);
        this.nightVision = context.bindUniformIfPresent("nightVision", GlUniformFloat::new);
        this.blindness = context.bindUniformIfPresent("blindness", GlUniformFloat::new);
        this.darknessFactor = context.bindUniformIfPresent("darknessFactor", GlUniformFloat::new);
        this.darknessLightFactor = context.bindUniformIfPresent("darknessLightFactor", GlUniformFloat::new);
        this.eyeBrightnessSmooth = context.bindUniformIfPresent("eyeBrightnessSmooth", GlUniformInt2v::new);
        this.texSampler = context.bindUniformIfPresent("tex", GlUniformInt::new);
        this.lightmapSampler = context.bindUniformIfPresent("lightmap", GlUniformInt::new);
        this.gaux1Sampler = context.bindUniformIfPresent("gaux1", GlUniformInt::new);
        this.gaux2Sampler = context.bindUniformIfPresent("gaux2", GlUniformInt::new);
        this.gaux4Sampler = context.bindUniformIfPresent("gaux4", GlUniformInt::new);
        this.depthtex0Sampler = context.bindUniformIfPresent("depthtex0", GlUniformInt::new);
        this.depthtex1Sampler = context.bindUniformIfPresent("depthtex1", GlUniformInt::new);
        this.noisetexSampler = context.bindUniformIfPresent("noisetex", GlUniformInt::new);
        this.shadowtex0Sampler = context.bindUniformIfPresent("shadowtex0", GlUniformInt::new);
        this.shadowtex1Sampler = context.bindUniformIfPresent("shadowtex1", GlUniformInt::new);
        this.shadowcolor0Sampler = context.bindUniformIfPresent("shadowcolor0", GlUniformInt::new);

        this.defaultAlphaTest = options.pass().supportsFragmentDiscard() ? 0.1f : 0.0f;
        this.usesTerrainInputs = options.pass().isReverseOrder();
    }

    @Override
    public void setupState(TerrainRenderPass pass) {
        if (pass.primitiveType() == QuadPrimitiveType.DIRECT) {
            this.primitiveType = GlPrimitiveType.QUADS;
        } else if (pass.primitiveType() == QuadPrimitiveType.TRIANGULATED) {
            this.primitiveType = GlPrimitiveType.TRIANGLES;
        } else {
            throw new IllegalArgumentException("Unknown primitive type");
        }

        for (var component : this.components) {
            component.setup();
        }

        this.shadowPassActive = ActiniumShadowRenderingState.areShadowsCurrentlyBeingRendered();
        this.legacyFrameCounter++;

        float[] fogColor = ChunkShaderFogComponent.FOG_SERVICE.getFogColor();

        if (this.irisFogColor != null) {
            this.irisFogColor.set(fogColor);
        }

        if (this.irisFogDensity != null) {
            this.irisFogDensity.set(ChunkShaderFogComponent.FOG_SERVICE.getFogDensity());
        }

        if (this.irisFogStart != null) {
            this.irisFogStart.set(ChunkShaderFogComponent.FOG_SERVICE.getFogStart());
        }

        if (this.irisFogEnd != null) {
            this.irisFogEnd.set(ChunkShaderFogComponent.FOG_SERVICE.getFogEnd());
        }

        if (this.irisCurrentAlphaTest != null) {
            this.irisCurrentAlphaTest.set(this.defaultAlphaTest);
        }

        if (this.legacyFogColor != null) {
            this.legacyFogColor.set(fogColor[0], fogColor[1], fogColor[2]);
        }

        Minecraft minecraft = Minecraft.getMinecraft();

        if (this.legacySkyColor != null && minecraft.world != null && minecraft.getRenderViewEntity() != null) {
            Vec3d currentSkyColor = minecraft.world.getSkyColor(minecraft.getRenderViewEntity(), minecraft.getRenderPartialTicks());
            this.legacySkyColor.set((float) currentSkyColor.x, (float) currentSkyColor.y, (float) currentSkyColor.z);
        }

        int width = Math.max(1, minecraft.displayWidth);
        int height = Math.max(1, minecraft.displayHeight);

        if (this.viewWidth != null) {
            this.viewWidth.set((float) width);
        }

        if (this.viewHeight != null) {
            this.viewHeight.set((float) height);
        }

        if (this.pixelSizeX != null) {
            this.pixelSizeX.set(1.0f / width);
        }

        if (this.pixelSizeY != null) {
            this.pixelSizeY.set(1.0f / height);
        }

        if (this.nearPlane != null) {
            this.nearPlane.set(0.05f);
        }

        if (this.far != null) {
            this.far.set(Math.max(16.0f, minecraft.gameSettings.renderDistanceChunks * 16.0f));
        }

        if (this.frameTimeCounter != null) {
            this.frameTimeCounter.set((System.nanoTime() - START_TIME_NANOS) / 1_000_000_000.0f);
        }

        if (this.frameCounter != null) {
            this.frameCounter.setInt(this.legacyFrameCounter);
        }

        if (this.darknessFactor != null) {
            this.darknessFactor.set(0.0f);
        }

        if (this.darknessLightFactor != null) {
            this.darknessLightFactor.set(0.0f);
        }

        if (this.texSampler != null) {
            this.texSampler.setInt(0);
        }

        if (this.lightmapSampler != null) {
            this.lightmapSampler.setInt(1);
        }

        if (this.gaux1Sampler != null) {
            this.gaux1Sampler.setInt(ActiniumRenderPipeline.TERRAIN_GAUX1_UNIT);
        }

        if (this.gaux2Sampler != null) {
            this.gaux2Sampler.setInt(ActiniumRenderPipeline.TERRAIN_GAUX2_UNIT);
        }

        if (this.gaux4Sampler != null) {
            this.gaux4Sampler.setInt(ActiniumRenderPipeline.WORLD_GAUX4_UNIT);
        }

        if (this.depthtex0Sampler != null) {
            this.depthtex0Sampler.setInt(ActiniumRenderPipeline.TERRAIN_DEPTHTEX0_UNIT);
        }

        if (this.depthtex1Sampler != null) {
            this.depthtex1Sampler.setInt(ActiniumRenderPipeline.TERRAIN_DEPTHTEX1_UNIT);
        }

        if (this.noisetexSampler != null) {
            this.noisetexSampler.setInt(ActiniumRenderPipeline.TERRAIN_NOISETEX_UNIT);
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

        if (this.usesTerrainInputs) {
            ActiniumRenderPipeline.INSTANCE.prepareTerrainInputs();
            ActiniumRenderPipeline.INSTANCE.bindTerrainInputTextures();
        }
        ActiniumRenderPipeline.INSTANCE.bindWorldGaux4Texture();
        ActiniumRenderPipeline.INSTANCE.bindTerrainShadowTextures();
        this.pushLegacyRuntimeState(minecraft);
        this.pushLegacyMatrices();
    }

    @Override
    public void restoreState() {
        if (this.usesTerrainInputs) {
            ActiniumRenderPipeline.INSTANCE.unbindTerrainInputTextures();
        }
        ActiniumRenderPipeline.INSTANCE.unbindWorldGaux4Texture();
        ActiniumRenderPipeline.INSTANCE.unbindTerrainShadowTextures();
    }

    @Override
    public void setProjectionMatrix(Matrix4fc matrix) {
        this.uniformProjectionMatrix.set(matrix);
        this.currentProjection.set(matrix);

        if (this.irisProjectionMatrix != null) {
            this.irisProjectionMatrix.set(matrix);
        }

        if (this.irisProjectionMatrixInverse != null || this.irisProjectionMatrixInv != null || this.gbufferProjectionInverse != null || this.shadowProjectionInverse != null) {
            this.scratchProjectionInverse.set(matrix);
            this.scratchProjectionInverse.invert();

            if (this.irisProjectionMatrixInverse != null) {
                this.irisProjectionMatrixInverse.set(this.scratchProjectionInverse);
            }

            if (this.irisProjectionMatrixInv != null) {
                this.irisProjectionMatrixInv.set(this.scratchProjectionInverse);
            }
        }

        this.pushLegacyMatrices();
    }

    @Override
    public void setModelViewMatrix(Matrix4fc matrix) {
        this.uniformModelViewMatrix.set(matrix);
        this.currentModelView.set(matrix);

        if (this.irisModelViewMatrix != null) {
            this.irisModelViewMatrix.set(matrix);
        }

        if (this.irisModelViewMatrixInverse != null || this.irisModelViewMatrixInv != null || this.irisNormalMatrix != null || this.irisNormalMat != null
                || this.gbufferModelViewInverse != null || this.shadowModelViewInverse != null) {
            this.scratchModelViewInverse.set(matrix);
            this.scratchModelViewInverse.invert();

            if (this.irisModelViewMatrixInverse != null) {
                this.irisModelViewMatrixInverse.set(this.scratchModelViewInverse);
            }

            if (this.irisModelViewMatrixInv != null) {
                this.irisModelViewMatrixInv.set(this.scratchModelViewInverse);
            }

            if (this.irisNormalMatrix != null || this.irisNormalMat != null) {
                this.scratchNormalMatrix.set(this.scratchModelViewInverse);
                this.scratchNormalMatrix.transpose();

                if (this.irisNormalMatrix != null) {
                    this.irisNormalMatrix.set(this.scratchNormalMatrix);
                }

                if (this.irisNormalMat != null) {
                    this.irisNormalMat.set(this.scratchNormalMatrix);
                }
            }
        }

        this.pushLegacyMatrices();
    }

    @Override
    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);

        if (this.irisRegionOffset != null) {
            this.irisRegionOffset.set(x, y, z);
        }
    }

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    @Override
    public void setTextureSlot(ChunkShaderTextureSlot slot, int val) {
        GlUniformInt uniform = this.uniformTextures.get(slot);

        if (uniform != null) {
            uniform.setInt(val);
        }

        if (slot == ChunkShaderTextureSlot.BLOCK && this.texSampler != null) {
            this.texSampler.setInt(val);
        }

        if (slot == ChunkShaderTextureSlot.LIGHT && this.lightmapSampler != null) {
            this.lightmapSampler.setInt(val);
        }
    }

    @Override
    public void setSectionAges(long timestamp, long[] loadTimes) {
        if (this.uniformChunkAges == null) {
            return;
        }

        try (MemoryStack stack = LWJGL.stackPush()) {
            FloatBuffer buffer = stack.callocFloat(loadTimes.length);
            long pointer = LWJGL.memAddress(buffer);

            for (long loadTime : loadTimes) {
                LWJGL.memPutFloat(pointer, (float) Math.min(MAX_CHUNK_AGE, (timestamp - loadTime) / 1_000_000L));
                pointer += 4;
            }

            this.uniformChunkAges.set(buffer);
        }
    }

    private void pushLegacyMatrices() {
        Matrix4f shadowModelViewMatrix = this.currentModelView;
        Matrix4f shadowProjectionMatrix = this.currentProjection;
        Matrix4f shadowModelViewInverseMatrix = this.scratchModelViewInverse;
        Matrix4f shadowProjectionInverseMatrix = this.scratchProjectionInverse;

        if (!this.shadowPassActive && (this.shadowModelView != null || this.shadowModelViewInverse != null || this.shadowProjection != null || this.shadowProjectionInverse != null)
                && ActiniumShadowMatrixAccess.fillShadowMatrices(this.scratchShadowModelView, this.scratchShadowProjection)) {
            this.scratchShadowModelViewInverse.set(this.scratchShadowModelView).invert();
            this.scratchShadowProjectionInverse.set(this.scratchShadowProjection).invert();
            shadowModelViewMatrix = this.scratchShadowModelView;
            shadowProjectionMatrix = this.scratchShadowProjection;
            shadowModelViewInverseMatrix = this.scratchShadowModelViewInverse;
            shadowProjectionInverseMatrix = this.scratchShadowProjectionInverse;
        }

        if (this.gbufferModelView != null) {
            this.gbufferModelView.set(this.currentModelView);
        }

        if (this.gbufferProjection != null) {
            this.gbufferProjection.set(this.currentProjection);
        }

        if (this.gbufferModelViewInverse != null) {
            this.gbufferModelViewInverse.set(this.scratchModelViewInverse);
        }

        if (this.gbufferProjectionInverse != null) {
            this.gbufferProjectionInverse.set(this.scratchProjectionInverse);
        }

        if (this.shadowModelView != null) {
            this.shadowModelView.set(shadowModelViewMatrix);
        }

        if (this.shadowProjection != null) {
            this.shadowProjection.set(shadowProjectionMatrix);
        }

        if (this.shadowModelViewInverse != null) {
            this.shadowModelViewInverse.set(shadowModelViewInverseMatrix);
        }

        if (this.shadowProjectionInverse != null) {
            this.shadowProjectionInverse.set(shadowProjectionInverseMatrix);
        }

        this.pushLegacyRuntimeState(Minecraft.getMinecraft());
    }

    private void pushLegacyRuntimeState(Minecraft minecraft) {
        Entity entity = minecraft.getRenderViewEntity();

        if (entity == null) {
            return;
        }

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

        if (minecraft.world != null) {
            float celestialAngle = minecraft.world.getCelestialAngle(0.0f) * ((float) Math.PI * 2.0f);
            int currentWorldTime = ActiniumCommonUniforms.getWorldTime(minecraft.world);
            float currentDayMoment = ActiniumCommonUniforms.getDayMoment(currentWorldTime);
            float rawDayNightMix = ActiniumCommonUniforms.getDayNightMix(currentWorldTime);
            float rain = minecraft.world.getRainStrength(0.0f);

            if (this.worldTime != null) {
                this.worldTime.setInt(currentWorldTime);
            }

            if (this.dayNightMix != null) {
                this.dayNightMix.set(rawDayNightMix);
            }

            if (this.dayMoment != null) {
                this.dayMoment.set(currentDayMoment);
            }

            if (this.dayMixer != null) {
                this.dayMixer.set(ActiniumCommonUniforms.getDayMixer(currentDayMoment));
            }

            if (this.nightMixer != null) {
                this.nightMixer.set(ActiniumCommonUniforms.getNightMixer(currentDayMoment));
            }

            if (this.moonPhase != null) {
                this.moonPhase.setInt(ActiniumCommonUniforms.getMoonPhase(minecraft.world));
            }

            if (this.rainStrength != null) {
                this.rainStrength.set(rain);
            }

            float sunX = -MathHelper.sin(celestialAngle);
            float sunY = MathHelper.cos(celestialAngle);
            float sunZ = 0.0f;

            this.currentModelView.transformDirection(sunX, sunY, sunZ, this.scratchSunPosition);
            this.currentModelView.transformDirection(-sunX, -sunY, -sunZ, this.scratchMoonPosition);

            if (this.sunPosition != null) {
                this.sunPosition.set(this.scratchSunPosition.x, this.scratchSunPosition.y, this.scratchSunPosition.z);
            }

            if (this.moonPosition != null) {
                this.moonPosition.set(this.scratchMoonPosition.x, this.scratchMoonPosition.y, this.scratchMoonPosition.z);
            }

            if (this.shadowLightPosition != null) {
                Vector3f light = rawDayNightMix >= 0.5f ? this.scratchSunPosition : this.scratchMoonPosition;
                this.shadowLightPosition.set(light.x, light.y, light.z);
            }
        }

        if (this.nightVision != null) {
            this.nightVision.set(getNightVisionStrength(entity));
        }

        if (this.blindness != null) {
            this.blindness.set(getBlindnessStrength(entity));
        }
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

        return 0.7f + MathHelper.sin((effect.getDuration() - 0.0f) * ((float) Math.PI * 0.2f)) * 0.3f;
    }

    private static float getBlindnessStrength(Entity entity) {
        if (!(entity instanceof EntityLivingBase living)) {
            return 0.0f;
        }

        PotionEffect effect = living.getActivePotionEffect(MobEffects.BLINDNESS);
        return effect != null ? 1.0f : 0.0f;
    }

    private static void bindTextureUniform(Map<ChunkShaderTextureSlot, GlUniformInt> uniforms, ChunkShaderTextureSlot slot, ShaderBindingContext context, String primaryName, String fallbackName) {
        GlUniformInt uniform = context.bindUniformIfPresent(primaryName, GlUniformInt::new);

        if (uniform == null) {
            uniform = context.bindUniformIfPresent(fallbackName, GlUniformInt::new);
        }

        if (uniform != null) {
            uniforms.put(slot, uniform);
        }
    }
}
