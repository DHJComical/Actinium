package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pack.ActiniumShaderProperties;
import com.dhj.actinium.shadows.ActiniumInternalShadowRenderingState;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.gl.array.GlVertexArray;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderParser;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;
import org.taumc.celeritas.mixin.core.terrain.ActiveRenderInfoAccessor;

public final class ActiniumRenderPipeline {
    public static final ActiniumRenderPipeline INSTANCE = new ActiniumRenderPipeline();
    public static final int WORLD_GAUX4_UNIT = 7;
    public static final int TERRAIN_GAUX1_UNIT = 4;
    public static final int TERRAIN_GAUX2_UNIT = 5;
    public static final int TERRAIN_DEPTHTEX0_UNIT = 8;
    public static final int TERRAIN_DEPTHTEX1_UNIT = 9;
    public static final int TERRAIN_NOISETEX_UNIT = 10;
    public static final int TERRAIN_SHADOW_TEX0_UNIT = 11;
    public static final int TERRAIN_SHADOW_TEX1_UNIT = 12;
    public static final int TERRAIN_SHADOW_COLOR0_UNIT = 13;

    private static final Pattern LEGACY_PACK_MARKERS = Pattern.compile("gl_Vertex|gl_MultiTexCoord|gl_FragData|gl_FragColor|gl_ModelViewProjectionMatrix|gl_TextureMatrix|\\bvarying\\b");
    private static final Pattern DRAW_BUFFERS_PATTERN = Pattern.compile("DRAWBUFFERS:([0-7]+)");
    private static final @Nullable Method GL_GET_FLOAT_BUFFER_METHOD = findGlGetFloatBufferMethod();
    private static final @Nullable Method GL_GET_INTEGER_BUFFER_METHOD = findGlGetIntegerBufferMethod();
    private static final String[] SKY_PROGRAMS = {
            "gbuffers_skybasic",
            "gbuffers_skytextured",
            "gbuffers_clouds"
    };
    private static final String[] WEATHER_PROGRAMS = {
            "gbuffers_weather"
    };
    private static final String[] SCENE_POST_PROGRAMS = {
            "prepare",
            "deferred",
            "composite",
            "composite1",
            "composite2",
            "composite3",
            "composite4",
            "composite5",
            "composite6",
            "composite7"
    };
    private static final String[] POST_PROGRAMS = {
            "prepare",
            "deferred",
            "composite",
            "composite1",
            "composite2",
            "composite3",
            "composite4",
            "composite5",
            "composite6",
            "composite7",
            "final"
    };

    private int observedReloadVersion = -1;
    @Getter
    private ActiniumRenderStage currentStage = ActiniumRenderStage.NONE;
    private boolean shadowProgramAvailable;
    private boolean skyProgramAvailable;
    private boolean weatherProgramAvailable;
    private boolean postProgramAvailable;
    private boolean loggedCapabilities;
    private boolean sceneProgramsResolved;
    private boolean finalProgramResolved;
    private boolean loggedPostProgramUse;
    private boolean loggedFinalProgramUse;
    @Getter
    private int frameCounter;
    private int shadowVisibilityFrameCounter;
    private long lastFrameNanos;
    @Getter
    private float frameTimeCounterSeconds;
    @Getter
    private int width;
    @Getter
    private int height;

    private final Matrix4f gbufferModelViewMatrix = new Matrix4f();
    private final Matrix4f gbufferProjectionMatrix = new Matrix4f();
    private final Matrix4f gbufferModelViewInverseMatrix = new Matrix4f();
    private final Matrix4f gbufferProjectionInverseMatrix = new Matrix4f();
    private final Matrix4f skyStageModelViewMatrix = new Matrix4f();
    private final Matrix4f skyStageProjectionMatrix = new Matrix4f();
    private final Matrix4f skyStageModelViewInverseMatrix = new Matrix4f();
    private final Matrix4f skyStageProjectionInverseMatrix = new Matrix4f();
    private final Matrix4f shadowModelViewMatrix = new Matrix4f();
    private final Matrix4f shadowProjectionMatrix = new Matrix4f();
    private final Matrix4f shadowModelViewInverseMatrix = new Matrix4f();
    private final Matrix4f shadowProjectionInverseMatrix = new Matrix4f();
    private final Vector3f scratchVector = new Vector3f();
    private final Vector3d shadowCameraPosition = new Vector3d();
    private final Vector3d worldCameraPosition = new Vector3d();
    @Getter
    private final float[] fogColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    private @Nullable GlVertexArray fullscreenVertexArray;
    private @Nullable ActiniumPostTargets postTargets;
    private @Nullable ActiniumShadowTargets shadowTargets;
    private @Nullable ActiniumWorldTargets worldTargets;
    private @Nullable ActiniumWorldProgram skyProgram;
    private @Nullable ActiniumWorldProgram skyTexturedProgram;
    private @Nullable ActiniumWorldProgram cloudsProgram;
    private @Nullable ActiniumWorldProgram weatherProgram;
    private @Nullable ActiniumWorldProgram activeWorldProgram;
    private @Nullable WorldStageGlState worldStageGlState;
    private @Nullable GlProgram<ActiniumPostShaderInterface> finalProgram;
    private @Nullable GlProgram<ActiniumPostShaderInterface> blitProgram;
    private @Nullable Integer whiteTexture;
    private @Nullable Integer noiseTexture;
    private @Nullable Integer terrainGaux1Texture;
    private @Nullable Integer terrainDepthTexture0;
    private @Nullable Integer terrainDepthTexture1;
    private List<ActiniumPostProgram> scenePrograms = Collections.emptyList();
    private boolean worldTargetsPrepared;
    private ActiniumRenderPipeline() {
    }

    public void beginWorld() {
        this.syncReloadState();
        this.frameCounter = nextFrameId(this.frameCounter);
        this.worldTargetsPrepared = false;
        this.currentStage = ActiniumRenderStage.WORLD;
    }

    public void endWorld() {
        this.currentStage = ActiniumRenderStage.NONE;
    }

    public void beginSky() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.SKY;
    }

    public void endSky() {
        if (this.currentStage == ActiniumRenderStage.SKY || this.currentStage == ActiniumRenderStage.SKY_TEXTURED) {
            this.currentStage = ActiniumRenderStage.WORLD;
        }
    }

    public void beginSkyTextured() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.SKY_TEXTURED;
    }

    public void beginClouds() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.CLOUDS;
    }

    public void endClouds() {
        if (this.currentStage == ActiniumRenderStage.CLOUDS) {
            this.currentStage = ActiniumRenderStage.WORLD;
        }
    }

    public void beginWeather() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.WEATHER;
    }

    public void endWeather() {
        if (this.currentStage == ActiniumRenderStage.WEATHER) {
            this.currentStage = ActiniumRenderStage.WORLD;
        }
    }

    public void beginPost() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.POST;
    }

    public void endPost() {
        if (this.currentStage == ActiniumRenderStage.POST) {
            this.currentStage = ActiniumRenderStage.NONE;
        }
    }

    public void captureWorldState() {
        this.syncReloadState();
        captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.gbufferModelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, this.gbufferProjectionMatrix);
        this.gbufferModelViewInverseMatrix.set(this.gbufferModelViewMatrix).invert();
        this.gbufferProjectionInverseMatrix.set(this.gbufferProjectionMatrix).invert();
        System.arraycopy(ChunkShaderFogComponent.FOG_SERVICE.getFogColor(), 0, this.fogColor, 0, this.fogColor.length);
    }

    public void captureSkyStageState() {
        this.syncReloadState();
        captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.skyStageModelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, this.skyStageProjectionMatrix);
        this.skyStageModelViewInverseMatrix.set(this.skyStageModelViewMatrix).invert();
        this.skyStageProjectionInverseMatrix.set(this.skyStageProjectionMatrix).invert();
    }

    public void renderPostPipeline(float partialTicks) {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer mainFramebuffer = minecraft.getFramebuffer();

        if (mainFramebuffer == null || mainFramebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, mainFramebuffer.framebufferWidth);
        this.height = Math.max(1, mainFramebuffer.framebufferHeight);

        List<ActiniumPostProgram> scenePrograms = this.getScenePrograms();
        GlProgram<ActiniumPostShaderInterface> finalProgram = this.getFinalProgram();

        if (scenePrograms.isEmpty() && finalProgram == null) {
            return;
        }

        this.ensureRuntimeResources();
        this.beginPost();

        try {
            if (this.postTargets != null) {
                this.postTargets.ensureSize(this.width, this.height);
                this.postTargets.resetSources();
                Integer worldColorTexture = null;
                Integer worldGaux4Texture = null;

                if (this.worldTargets != null && this.worldTargets.hasSourceColorTexture()) {
                    worldColorTexture = this.worldTargets.getSourceColorTexture();
                    worldGaux4Texture = this.worldTargets.getSourceGaux4Texture();
                }

                this.postTargets.copySceneTextures(mainFramebuffer, worldColorTexture, worldGaux4Texture);
            }
            this.executeScenePrograms(scenePrograms, partialTicks);
            this.renderFinalPass(mainFramebuffer, finalProgram, partialTicks);
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to execute Actinium shader pack post pipeline; disabling external post programs until the next shader reload", e);
            this.disableExternalProgramsUntilReload();
        } finally {
            this.endPost();
        }
    }

    public boolean hasShadowProgram() {
        this.syncReloadState();
        return this.shadowProgramAvailable;
    }

    public boolean hasSkyProgram() {
        this.syncReloadState();
        return this.skyProgramAvailable;
    }

    public boolean hasWeatherProgram() {
        this.syncReloadState();
        return this.weatherProgramAvailable;
    }

    public boolean hasPostProgram() {
        this.syncReloadState();
        return this.postProgramAvailable;
    }

    public Matrix4fc getGbufferModelViewMatrix() {
        return this.gbufferModelViewMatrix;
    }

    public Matrix4fc getGbufferProjectionMatrix() {
        return this.gbufferProjectionMatrix;
    }

    public Matrix4fc getGbufferModelViewInverseMatrix() {
        return this.gbufferModelViewInverseMatrix;
    }

    public Matrix4fc getGbufferProjectionInverseMatrix() {
        return this.gbufferProjectionInverseMatrix;
    }

    public Matrix4fc getShadowModelViewMatrix() {
        return this.shadowModelViewMatrix;
    }

    public Matrix4fc getShadowProjectionMatrix() {
        return this.shadowProjectionMatrix;
    }

    public Matrix4fc getShadowModelViewInverseMatrix() {
        return this.shadowModelViewInverseMatrix;
    }

    public Matrix4fc getShadowProjectionInverseMatrix() {
        return this.shadowProjectionInverseMatrix;
    }

    public Matrix4fc getSkyStageModelViewMatrix() {
        return this.skyStageModelViewMatrix;
    }

    public Matrix4fc getSkyStageProjectionInverseMatrix() {
        return this.skyStageProjectionInverseMatrix;
    }

    public void renderShadowPass(float partialTicks) {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled() || !this.hasShadowProgram()) {
            return;
        }

        ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();

        if (!properties.isShadowEnabled()) {
            ActiniumInternalShadowRenderingState.clear();
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer mainFramebuffer = minecraft.getFramebuffer();
        this.width = Math.max(1, mainFramebuffer != null ? mainFramebuffer.framebufferWidth : minecraft.displayWidth);
        this.height = Math.max(1, mainFramebuffer != null ? mainFramebuffer.framebufferHeight : minecraft.displayHeight);

        this.ensureRuntimeResources();
        this.computeShadowMatrices(partialTicks, properties);

        int resolution = chooseShadowResolution(properties);

        if (!this.renderShadowTerrainPass(properties, resolution, partialTicks)) {
            if (this.shadowTargets != null) {
                this.shadowTargets.ensureSize(resolution);
                this.shadowTargets.clear();
            }
        }

        ActiniumInternalShadowRenderingState.update(this.shadowModelViewMatrix, this.shadowProjectionMatrix);
    }

    public Vector3f transformDirection(float x, float y, float z) {
        return this.gbufferModelViewMatrix.transformDirection(x, y, z, this.scratchVector).normalize();
    }

    public void bindTerrainShadowTextures() {
        int shadowTex0 = this.shadowTargets != null ? this.shadowTargets.getDepthTexture(0) : 0;
        int shadowTex1 = this.shadowTargets != null ? this.shadowTargets.getDepthTexture(1) : 0;
        int shadowColor0 = this.shadowTargets != null ? this.shadowTargets.getColorTexture() : 0;
        bindTexture(TERRAIN_SHADOW_TEX0_UNIT, shadowTex0);
        bindTexture(TERRAIN_SHADOW_TEX1_UNIT, shadowTex1);
        bindTexture(TERRAIN_SHADOW_COLOR0_UNIT, shadowColor0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public void prepareTerrainInputs() {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return;
        }

        clearGlErrorsSilently();

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer == null || framebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, framebuffer.framebufferWidth);
        this.height = Math.max(1, framebuffer.framebufferHeight);
        this.ensureRuntimeResources();
        debugCheckGlErrors("pipeline.prepareTerrainInputs.ensureRuntimeResources");
        this.prepareWorldTargets(framebuffer);
        debugCheckGlErrors("pipeline.prepareTerrainInputs.prepareWorldTargets");
        this.ensureTerrainInputTextures();
        debugCheckGlErrors("pipeline.prepareTerrainInputs.ensureTerrainInputTextures");

        framebuffer.bindFramebuffer(true);
        this.copyTexture(framebuffer.framebufferTexture, this.terrainGaux1Texture);
        this.copyFramebufferDepthToTexture(this.terrainDepthTexture0);
        this.copyFramebufferDepthToTexture(this.terrainDepthTexture1);
    }

    public void bindTerrainInputTextures() {
        int gaux1Texture = this.terrainGaux1Texture != null ? this.terrainGaux1Texture : 0;
        int gaux2Texture = this.noiseTexture != null ? this.noiseTexture : 0;
        int depthtex0Texture = this.terrainDepthTexture0 != null ? this.terrainDepthTexture0 : 0;
        int depthtex1Texture = this.terrainDepthTexture1 != null ? this.terrainDepthTexture1 : 0;
        int noiseTextureId = this.noiseTexture != null ? this.noiseTexture : 0;

        bindTexture(TERRAIN_GAUX1_UNIT, gaux1Texture);
        bindTexture(TERRAIN_GAUX2_UNIT, gaux2Texture);
        bindTexture(TERRAIN_DEPTHTEX0_UNIT, depthtex0Texture);
        bindTexture(TERRAIN_DEPTHTEX1_UNIT, depthtex1Texture);
        bindTexture(TERRAIN_NOISETEX_UNIT, noiseTextureId);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public void unbindTerrainInputTextures() {
        unbindTexture(TERRAIN_NOISETEX_UNIT);
        unbindTexture(TERRAIN_DEPTHTEX1_UNIT);
        unbindTexture(TERRAIN_DEPTHTEX0_UNIT);
        unbindTexture(TERRAIN_GAUX2_UNIT);
        unbindTexture(TERRAIN_GAUX1_UNIT);
    }

    public void unbindTerrainShadowTextures() {
        unbindTexture(TERRAIN_SHADOW_COLOR0_UNIT);
        unbindTexture(TERRAIN_SHADOW_TEX1_UNIT);
        unbindTexture(TERRAIN_SHADOW_TEX0_UNIT);
    }

    public void bindWorldGaux4Texture() {
        int textureId = this.worldTargets != null
                ? this.worldTargets.getSourceGaux4TextureOrDefault(this.whiteTexture)
                : this.whiteTexture != null ? this.whiteTexture : 0;
        bindTexture(WORLD_GAUX4_UNIT, textureId);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public void unbindWorldGaux4Texture() {
        unbindTexture(WORLD_GAUX4_UNIT);
    }

    public void bindWorldStageProgram(float partialTicks) {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return;
        }

        clearGlErrorsSilently();

        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft.world == null || minecraft.getRenderViewEntity() == null) {
            return;
        }

        ActiniumWorldProgram program = this.getCurrentWorldStageProgram();

        if (program == null) {
            return;
        }
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer == null || framebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, framebuffer.framebufferWidth);
        this.height = Math.max(1, framebuffer.framebufferHeight);
        this.ensureRuntimeResources();
        debugCheckGlErrors("pipeline.bindWorldStageProgram.ensureRuntimeResources");
        this.prepareWorldTargets(framebuffer);
        debugCheckGlErrors("pipeline.bindWorldStageProgram.prepareWorldTargets");

        Matrix4f modelViewMatrix = new Matrix4f();
        Matrix4f projectionMatrix = new Matrix4f();
        Matrix4f projectionInverseMatrix = new Matrix4f();

        captureMatrix(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        projectionInverseMatrix.set(projectionMatrix).invert();

        if (this.activeWorldProgram != null) {
            this.unbindWorldStageProgram();
        }

        this.worldStageGlState = WorldStageGlState.capture();
        debugCheckGlErrors("world-stage.capture:" + program.name());
        this.debugLog("Binding world-stage program '{}' for {} with draw buffers {}", program.name(), this.currentStage, Arrays.toString(program.drawBuffers()));
        boolean renderColorTex1ToMain = !this.hasPostProgram();
        if (this.worldTargets != null) {
            this.worldTargets.bindWriteFramebuffer(framebuffer, program.drawBuffers(), renderColorTex1ToMain);
        }
        debugCheckGlErrors("world-stage.bindFramebuffer:" + program.name());
        this.activeWorldProgram = program;
        program.program().bind();
        debugCheckGlErrors("world-stage.bindProgram:" + program.name());
        program.program().getInterface().setupState(this, modelViewMatrix, projectionInverseMatrix);
        this.bindWorldStageTextures();
        debugCheckGlErrors("world-stage.bindTextures:" + program.name());
    }

    public void unbindWorldStageProgram() {
        if (this.activeWorldProgram == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer != null && framebuffer.framebufferTexture > 0) {
            this.width = Math.max(1, framebuffer.framebufferWidth);
            this.height = Math.max(1, framebuffer.framebufferHeight);
            this.ensureRuntimeResources();
            boolean renderColorTex1ToMain = !this.hasPostProgram();
            if (this.worldTargets != null) {
                this.worldTargets.endWrite(framebuffer, this.activeWorldProgram.drawBuffers(), renderColorTex1ToMain);
                this.presentWorldStageResult(framebuffer, this.activeWorldProgram.drawBuffers());
            }
            debugCheckGlErrors("world-stage.endWrite:" + this.activeWorldProgram.name());
        }

        this.unbindWorldStageTextures();
        this.activeWorldProgram.program().unbind();
        this.activeWorldProgram = null;
        debugCheckGlErrors("world-stage.unbindProgram");

        if (this.worldStageGlState != null) {
            this.worldStageGlState.restore();
            this.worldStageGlState = null;
            debugCheckGlErrors("world-stage.restoreState");
        }
    }

    private void executeScenePrograms(List<ActiniumPostProgram> scenePrograms, float partialTicks) {
        for (ActiniumPostProgram sceneProgram : scenePrograms) {
            if (this.postTargets != null) {
                this.postTargets.bindWriteFramebuffer(sceneProgram.drawBuffers());
            }
            GL11.glViewport(0, 0, this.width, this.height);
            this.renderFullscreenProgram(sceneProgram.program(), partialTicks);
            this.postTargets.flipWrittenTargets(sceneProgram.drawBuffers());
        }
    }

    private void renderFinalPass(Framebuffer mainFramebuffer, @Nullable GlProgram<ActiniumPostShaderInterface> program, float partialTicks) {
        if (program != null) {
            mainFramebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, this.width, this.height);
            this.renderFullscreenProgram(program, partialTicks);
            return;
        }

        this.renderBlitFallback(mainFramebuffer, partialTicks);
    }

    private void renderBlitFallback(Framebuffer mainFramebuffer, float partialTicks) {
        GlProgram<ActiniumPostShaderInterface> program = this.getBlitProgram();
        mainFramebuffer.bindFramebuffer(true);
        GL11.glViewport(0, 0, this.width, this.height);
        this.renderFullscreenProgram(program, partialTicks);
    }

    private void renderFullscreenProgram(GlProgram<ActiniumPostShaderInterface> program, float partialTicks) {
        long now = System.nanoTime();
        float frameDeltaSeconds = this.lastFrameNanos == 0L ? 0.0f : Math.max(0.0f, (now - this.lastFrameNanos) / 1_000_000_000.0f);
        this.lastFrameNanos = now;
        this.frameTimeCounterSeconds += frameDeltaSeconds;
        if (this.frameTimeCounterSeconds >= 3600.0f) {
            this.frameTimeCounterSeconds = 0.0f;
        }

        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        program.bind();
        program.getInterface().setupState(this, this.postTargets, partialTicks, frameDeltaSeconds, this.frameTimeCounterSeconds, this.frameCounter);
        this.bindPipelineTextures();

        if (this.fullscreenVertexArray != null) {
            GL30.glBindVertexArray(this.fullscreenVertexArray.handle());
        }
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        this.unbindPipelineTextures();
        program.unbind();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
    }

    private void bindPipelineTextures() {
        if (this.postTargets != null) {
            bindTexture(0, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX0));
            bindTexture(1, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
            bindTexture(2, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX2));
            bindTexture(3, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX3));
            bindTexture(4, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX1));
            bindTexture(5, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX2));
            bindTexture(6, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX3));
            bindTexture(7, this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
            bindTexture(8, this.postTargets.getDepthTexture(0));
            bindTexture(9, this.postTargets.getDepthTexture(1));
            bindTexture(10, this.noiseTexture);
            bindTexture(TERRAIN_SHADOW_TEX0_UNIT, this.shadowTargets != null ? this.shadowTargets.getDepthTexture(0) : this.whiteTexture);
            bindTexture(TERRAIN_SHADOW_TEX1_UNIT, this.shadowTargets != null ? this.shadowTargets.getDepthTexture(1) : this.whiteTexture);
            bindTexture(TERRAIN_SHADOW_COLOR0_UNIT, this.shadowTargets != null ? this.shadowTargets.getColorTexture() : this.whiteTexture);
        }
    }

    private void bindWorldStageTextures() {
        int gaux4Texture = this.worldTargets != null
                ? this.worldTargets.getSourceGaux4TextureOrDefault(this.whiteTexture)
                : this.whiteTexture != null ? this.whiteTexture : 0;
        bindTexture(WORLD_GAUX4_UNIT, gaux4Texture);
        this.bindTerrainShadowTextures();
    }

    private void unbindPipelineTextures() {
        for (int unit = TERRAIN_SHADOW_COLOR0_UNIT; unit >= 0; unit--) {
            unbindTexture(unit);
        }
    }

    private void unbindWorldStageTextures() {
        unbindTexture(WORLD_GAUX4_UNIT);
        this.unbindTerrainShadowTextures();
    }

    private List<ActiniumPostProgram> getScenePrograms() {
        if (!this.shouldUseExternalScenePrograms()) {
            return Collections.emptyList();
        }

        if (this.sceneProgramsResolved) {
            return this.scenePrograms;
        }

        this.sceneProgramsResolved = true;
        List<ActiniumPostProgram> resolved = new ArrayList<>();

        for (String programName : SCENE_POST_PROGRAMS) {
            String fragmentSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT);

            if (fragmentSource == null) {
                continue;
            }

            String vertexSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.VERTEX);
            int[] drawBuffers = parseDrawBuffers(fragmentSource);

            try {
                resolved.add(new ActiniumPostProgram(programName, this.createProgram(programName, vertexSource, fragmentSource, drawBuffers), drawBuffers));
            } catch (RuntimeException e) {
                ActiniumShaders.logger().warn("Failed to compile external shader pack post program '{}'", programName, e);
            }
        }

        if (!resolved.isEmpty() && !this.loggedPostProgramUse) {
            this.loggedPostProgramUse = true;
            ActiniumShaders.logger().info("Using external shader pack scene post programs: {}", resolved.stream().map(ActiniumPostProgram::name).reduce((a, b) -> a + ", " + b).orElse("unknown"));
        }

        this.scenePrograms = resolved;
        return this.scenePrograms;
    }

    private @Nullable GlProgram<ActiniumPostShaderInterface> getFinalProgram() {
        if (!this.shouldUseExternalFinalProgram()) {
            return null;
        }

        if (this.finalProgramResolved) {
            return this.finalProgram;
        }

        this.finalProgramResolved = true;
        String fragmentSource = ActiniumShaderPackManager.getProgramSource("final", ShaderType.FRAGMENT);

        if (fragmentSource == null) {
            this.finalProgram = null;
            return null;
        }

        String vertexSource = ActiniumShaderPackManager.getProgramSource("final", ShaderType.VERTEX);

        try {
            this.finalProgram = this.createProgram("final", vertexSource, fragmentSource, new int[]{0});
            if (!this.loggedFinalProgramUse) {
                this.loggedFinalProgramUse = true;
                ActiniumShaders.logger().info("Using external shader pack final pass");
            }
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to compile external shader pack program 'final'", e);
            this.finalProgram = null;
        }

        return this.finalProgram;
    }

    private GlProgram<ActiniumPostShaderInterface> getBlitProgram() {
        if (this.blitProgram != null) {
            return this.blitProgram;
        }

        String vertexSource = String.join("\n",
                "#version 330 core",
                "out vec2 texcoord;",
                "void main() {",
                "    vec2 pos = vec2((gl_VertexID & 1) * 2 - 1, ((gl_VertexID >> 1) & 1) * 2 - 1);",
                "    texcoord = pos * 0.5 + 0.5;",
                "    gl_Position = vec4(pos, 0.0, 1.0);",
                "}",
                ""
        );
        String fragmentSource = String.join("\n",
                "#version 330 core",
                "uniform sampler2D colortex1;",
                "in vec2 texcoord;",
                "out vec4 fragColor0;",
                "void main() {",
                "    fragColor0 = texture(colortex1, texcoord);",
                "}",
                ""
        );

        this.blitProgram = this.createProgram("actinium_internal_blit", vertexSource, fragmentSource, new int[]{0});
        return this.blitProgram;
    }

    private GlProgram<ActiniumPostShaderInterface> createProgram(String programName, @Nullable String vertexSource, String fragmentSource, int[] drawBuffers) {
        List<GlShader> shaders = new ArrayList<>(2);
        boolean legacyProgram = this.isLegacyPackProgram((vertexSource != null ? vertexSource : "") + "\n" + fragmentSource);

        shaders.add(this.loadPostShader(programName, ShaderType.VERTEX, vertexSource));
        shaders.add(this.loadPostShader(programName, ShaderType.FRAGMENT, fragmentSource));

        try {
            GlProgram.Builder builder = GlProgram.builder("actinium:post/" + programName);
            shaders.forEach(builder::attachShader);

            for (int i = 0; i < drawBuffers.length; i++) {
                builder.bindFragmentData(legacyProgram ? "fragColor" + i : (i == 0 ? "fragColor0" : "fragColor" + i), i);
            }

            return builder.link(ActiniumPostShaderInterface::new);
        } finally {
            shaders.forEach(GlShader::delete);
        }
    }

    private GlShader loadPostShader(String programName, ShaderType type, @Nullable String source) {
        String shaderSource = source;

        if (shaderSource == null) {
            shaderSource = type == ShaderType.VERTEX ? defaultVertexSource() : defaultFragmentSource();
        }

        if (this.isLegacyPackProgram(shaderSource)) {
            shaderSource = ActiniumLegacyFullscreenShaderAdapter.translate(type, shaderSource);
        }

        shaderSource = ShaderParser.parseShader(shaderSource, this::resolveShaderSource, ShaderConstants.EMPTY);

        return new GlShader(type, "actinium:external/" + programName + "." + type.fileExtension, shaderSource);
    }

    private boolean isLegacyPackProgram(String source) {
        return LEGACY_PACK_MARKERS.matcher(source).find();
    }

    private boolean shouldUseExternalScenePrograms() {
        return false;
    }

    private boolean shouldUseExternalFinalProgram() {
        return this.shouldUseExternalScenePrograms();
    }

    private String resolveShaderSource(String path) {
        String activeShaderSource = ActiniumShaderPackManager.getShaderSource(path);

        if (activeShaderSource != null) {
            return activeShaderSource;
        }

        return ShaderLoader.getShaderSource(path);
    }

    private void ensureRuntimeResources() {
        if (this.fullscreenVertexArray == null) {
            this.fullscreenVertexArray = new GlVertexArray();
        }

        if (this.postTargets == null) {
            this.postTargets = new ActiniumPostTargets();
        }

        if (this.shadowTargets == null) {
            this.shadowTargets = new ActiniumShadowTargets();
        }

        if (this.worldTargets == null) {
            this.worldTargets = new ActiniumWorldTargets();
        }

        if (this.whiteTexture == null) {
            this.whiteTexture = createSolidTexture(255, 255, 255, 255);
        }

        if (this.noiseTexture == null) {
            this.noiseTexture = createNoiseTexture();
        }

        this.ensureTerrainInputTextures();
    }

    private void syncReloadState() {
        int reloadVersion = ActiniumShaderPackManager.getReloadVersion();

        if (this.observedReloadVersion == reloadVersion) {
            return;
        }

        this.observedReloadVersion = reloadVersion;
        this.deleteRuntimeResources();
        this.currentStage = ActiniumRenderStage.NONE;
        this.shadowProgramAvailable = hasAnyStageProgram("shadow");
        this.skyProgramAvailable = hasAnyStageProgram(SKY_PROGRAMS);
        this.weatherProgramAvailable = hasAnyStageProgram(WEATHER_PROGRAMS);
        this.postProgramAvailable = this.hasUsablePostProgram();
        this.loggedCapabilities = false;
        this.debugLog(
                "Observed shader reload version {}: shadow={}, sky={}, weather={}, post={}",
                reloadVersion,
                this.shadowProgramAvailable,
                this.skyProgramAvailable,
                this.weatherProgramAvailable,
                this.postProgramAvailable
        );
        this.logCapabilities();
    }

    private boolean hasUsablePostProgram() {
        return this.shouldUseExternalFinalProgram() && hasProgram("final")
                || this.shouldUseExternalScenePrograms() && hasAnyStageProgram(SCENE_POST_PROGRAMS);
    }

    private void logCapabilities() {
        if (this.loggedCapabilities || !ActiniumShaderPackManager.areShadersEnabled()) {
            return;
        }

        this.loggedCapabilities = true;
        Set<String> capabilities = new LinkedHashSet<>();

        if (this.shadowProgramAvailable) {
            capabilities.add("shadow");
        }

        if (this.skyProgramAvailable) {
            capabilities.add("sky");
        }

        if (this.weatherProgramAvailable) {
            capabilities.add("weather");
        }

        if (this.postProgramAvailable) {
            capabilities.add("post");
        }

        if (capabilities.isEmpty()) {
            ActiniumShaders.logger().info("Active shader pack does not expose shadow/sky/weather/post programs yet");
        } else {
            ActiniumShaders.logger().info("Active shader pack stage programs detected: {}", String.join(", ", capabilities));
        }
    }

    private void deleteRuntimeResources() {
        this.deleteWorldPrograms();

        for (ActiniumPostProgram sceneProgram : this.scenePrograms) {
            sceneProgram.program().delete();
        }

        this.scenePrograms = Collections.emptyList();
        this.sceneProgramsResolved = false;

        if (this.finalProgram != null) {
            this.finalProgram.delete();
            this.finalProgram = null;
        }

        if (this.blitProgram != null) {
            this.blitProgram.delete();
            this.blitProgram = null;
        }

        this.finalProgramResolved = false;
        this.loggedPostProgramUse = false;
        this.loggedFinalProgramUse = false;
        this.frameCounter = 0;
        this.shadowVisibilityFrameCounter = 0;
        this.lastFrameNanos = 0L;
        this.frameTimeCounterSeconds = 0.0f;
        this.width = 0;
        this.height = 0;

        if (this.fullscreenVertexArray != null) {
            this.fullscreenVertexArray.delete();
            this.fullscreenVertexArray = null;
        }

        if (this.postTargets != null) {
            this.postTargets.delete();
            this.postTargets = null;
        }

        if (this.shadowTargets != null) {
            this.shadowTargets.delete();
            this.shadowTargets = null;
        }

        if (this.worldTargets != null) {
            this.worldTargets.delete();
            this.worldTargets = null;
        }

        if (this.whiteTexture != null) {
            GL11.glDeleteTextures(this.whiteTexture);
            this.whiteTexture = null;
        }

        if (this.noiseTexture != null) {
            GL11.glDeleteTextures(this.noiseTexture);
            this.noiseTexture = null;
        }

        if (this.terrainGaux1Texture != null) {
            GL11.glDeleteTextures(this.terrainGaux1Texture);
            this.terrainGaux1Texture = null;
        }

        if (this.terrainDepthTexture0 != null) {
            GL11.glDeleteTextures(this.terrainDepthTexture0);
            this.terrainDepthTexture0 = null;
        }

        if (this.terrainDepthTexture1 != null) {
            GL11.glDeleteTextures(this.terrainDepthTexture1);
            this.terrainDepthTexture1 = null;
        }

        this.worldTargetsPrepared = false;
        ActiniumInternalShadowRenderingState.clear();
    }

    private void disableExternalProgramsUntilReload() {
        this.deleteWorldPrograms();

        for (ActiniumPostProgram sceneProgram : this.scenePrograms) {
            sceneProgram.program().delete();
        }

        this.scenePrograms = Collections.emptyList();
        this.sceneProgramsResolved = true;

        if (this.finalProgram != null) {
            this.finalProgram.delete();
            this.finalProgram = null;
        }

        this.finalProgramResolved = true;
    }

    private void deleteWorldPrograms() {
        this.releaseActiveWorldStageState();

        if (this.skyProgram != null) {
            this.skyProgram.program().delete();
            this.skyProgram = null;
        }

        if (this.skyTexturedProgram != null) {
            this.skyTexturedProgram.program().delete();
            this.skyTexturedProgram = null;
        }

        if (this.cloudsProgram != null) {
            this.cloudsProgram.program().delete();
            this.cloudsProgram = null;
        }

        if (this.weatherProgram != null) {
            this.weatherProgram.program().delete();
            this.weatherProgram = null;
        }

        this.activeWorldProgram = null;
    }

    public void resetVanillaRenderState() {
        this.releaseActiveWorldStageState();
        this.currentStage = ActiniumRenderStage.NONE;
        this.worldTargetsPrepared = false;

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer != null) {
            framebuffer.bindFramebuffer(true);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }

        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        debugCheckGlErrors("pipeline.resetVanillaRenderState");
    }

    private void releaseActiveWorldStageState() {
        if (this.activeWorldProgram != null) {
            this.unbindWorldStageTextures();
            this.activeWorldProgram.program().unbind();
            this.activeWorldProgram = null;
            debugCheckGlErrors("world-stage.releaseProgram");
        }

        if (this.worldStageGlState != null) {
            this.worldStageGlState.restore();
            this.worldStageGlState = null;
            debugCheckGlErrors("world-stage.releaseRestore");
        }
    }

    private @Nullable ActiniumWorldProgram getCurrentWorldStageProgram() {
        return switch (this.currentStage) {
            case SKY -> this.getWorldStageProgram(ActiniumWorldStage.SKY);
            case SKY_TEXTURED -> this.getWorldStageProgram(ActiniumWorldStage.SKY_TEXTURED);
            case CLOUDS -> this.getWorldStageProgram(ActiniumWorldStage.CLOUDS);
            case WEATHER -> this.getWorldStageProgram(ActiniumWorldStage.WEATHER);
            default -> null;
        };
    }

    private @Nullable ActiniumWorldProgram getWorldStageProgram(ActiniumWorldStage stage) {
        return switch (stage) {
            case SKY -> {
                if (this.skyProgram == null) {
                    this.skyProgram = this.createWorldStageProgram(stage);
                }
                yield this.skyProgram;
            }
            case SKY_TEXTURED -> {
                if (this.skyTexturedProgram == null) {
                    this.skyTexturedProgram = this.createWorldStageProgram(stage);
                }
                yield this.skyTexturedProgram;
            }
            case CLOUDS -> {
                if (this.cloudsProgram == null) {
                    this.cloudsProgram = this.createWorldStageProgram(stage);
                }
                yield this.cloudsProgram;
            }
            case WEATHER -> {
                if (this.weatherProgram == null) {
                    this.weatherProgram = this.createWorldStageProgram(stage);
                }
                yield this.weatherProgram;
            }
        };
    }

    private @Nullable ActiniumWorldProgram createWorldStageProgram(ActiniumWorldStage stage) {
        String vertexSource = ActiniumShaderPackManager.getProgramSource(stage.programName(), ShaderType.VERTEX);
        String fragmentSource = ActiniumShaderPackManager.getProgramSource(stage.programName(), ShaderType.FRAGMENT);

        if (vertexSource == null || fragmentSource == null) {
            return null;
        }

        String resolvedVertexSource = ShaderParser.parseShader(vertexSource, this::resolveShaderSource, ShaderConstants.EMPTY);
        String resolvedFragmentSource = ShaderParser.parseShader(fragmentSource, this::resolveShaderSource, ShaderConstants.EMPTY);
        int[] drawBuffers = this.resolveWorldStageDrawBuffers(stage, resolvedFragmentSource);
        List<GlShader> shaders = new ArrayList<>(2);
        shaders.add(new GlShader(ShaderType.VERTEX, "actinium:world/" + stage.programName() + "." + ShaderType.VERTEX.fileExtension, resolvedVertexSource));
        shaders.add(new GlShader(ShaderType.FRAGMENT, "actinium:world/" + stage.programName() + "." + ShaderType.FRAGMENT.fileExtension, resolvedFragmentSource));

        try {
            GlProgram.Builder builder = GlProgram.builder("actinium:world/" + stage.programName());
            shaders.forEach(builder::attachShader);
            this.debugLog("Compiled world-stage program '{}' with draw buffers {}", stage.programName(), Arrays.toString(drawBuffers));
            return new ActiniumWorldProgram(stage.programName(), builder.link(ActiniumWorldShaderInterface::new), drawBuffers);
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to compile external shader pack world-stage program '{}'", stage.programName(), e);
            return null;
        } finally {
            shaders.forEach(GlShader::delete);
        }
    }

    private void ensureTerrainInputTextures() {
        if (this.width <= 0 || this.height <= 0) {
            this.debugLog("Skipping terrain input allocation because framebuffer size is invalid: {}x{}", this.width, this.height);
            return;
        }

        if (this.terrainGaux1Texture == null) {
            this.terrainGaux1Texture = createColorTexture(this.width, this.height);
            this.debugLog("Allocated terrain gaux1 texture {} with size {}x{}", this.terrainGaux1Texture, this.width, this.height);
        }

        if (this.terrainDepthTexture0 == null) {
            this.terrainDepthTexture0 = createDepthTexture(this.width, this.height);
            this.debugLog("Allocated terrain depthtex0 texture {} with size {}x{}", this.terrainDepthTexture0, this.width, this.height);
        }

        if (this.terrainDepthTexture1 == null) {
            this.terrainDepthTexture1 = createDepthTexture(this.width, this.height);
            this.debugLog("Allocated terrain depthtex1 texture {} with size {}x{}", this.terrainDepthTexture1, this.width, this.height);
        }
    }

    private void prepareWorldTargets(Framebuffer framebuffer) {
        if (this.worldTargets == null || this.width <= 0 || this.height <= 0) {
            return;
        }

        clearGlErrorsSilently();
        this.worldTargets.ensureSize(this.width, this.height);
        debugCheckGlErrors("pipeline.prepareWorldTargets.ensureSize");

        if (!this.worldTargetsPrepared) {
            this.worldTargets.beginFrame(framebuffer);
            debugCheckGlErrors("pipeline.prepareWorldTargets.beginFrame");
            this.worldTargetsPrepared = true;
            this.debugLog("Prepared world-stage MRT targets for {}x{}", this.width, this.height);
        }
    }

    private int[] resolveWorldStageDrawBuffers(ActiniumWorldStage stage, String resolvedFragmentSource) {
        int[] drawBuffers = parseDrawBuffers(resolvedFragmentSource);

        if (this.shouldForceLegacySkyDrawBuffers(stage, resolvedFragmentSource, drawBuffers)) {
            this.debugLog("Forcing world-stage program '{}' draw buffers to [1, 7] from writebuffers.glsl compatibility rule", stage.programName());
            return new int[]{ActiniumPostTargets.TARGET_COLORTEX1, ActiniumPostTargets.TARGET_GAUX4};
        }

        return drawBuffers;
    }

    private boolean shouldForceLegacySkyDrawBuffers(ActiniumWorldStage stage, String resolvedFragmentSource, int[] parsedDrawBuffers) {
        if (stage != ActiniumWorldStage.SKY) {
            return false;
        }

        if (parsedDrawBuffers.length == 2
                && parsedDrawBuffers[0] == ActiniumPostTargets.TARGET_COLORTEX1
                && parsedDrawBuffers[1] == ActiniumPostTargets.TARGET_GAUX4) {
            return false;
        }

        if (!resolvedFragmentSource.contains("GBUFFER_SKYBASIC")) {
            return false;
        }

        String writeBuffersSource = this.getActivePackShaderSource();
        if (writeBuffersSource == null || writeBuffersSource.isBlank()) {
            this.debugLog(
                    "Skipping world-stage draw buffer compatibility check for '{}' because src/writebuffers.glsl is unavailable in the active pack",
                    stage.programName()
            );
            return false;
        }
        boolean hasSkyBasicBranch = writeBuffersSource.contains("MC_VERSION < 11604 && defined GBUFFER_SKYBASIC");
        boolean hasSkyBasicDrawBuffers = hasSkyBasicBranch && writeBuffersSource.contains("/* DRAWBUFFERS:17 */");

        this.debugLog(
                "Resolved world-stage draw buffers for '{}' as {} before compatibility fix; skybasicBranch={}, skybasicDrawBuffers17={}",
                stage.programName(),
                Arrays.toString(parsedDrawBuffers),
                hasSkyBasicBranch,
                hasSkyBasicDrawBuffers
        );

        return hasSkyBasicDrawBuffers;
    }

    private void presentWorldStageResult(Framebuffer mainFramebuffer, int[] drawBuffers) {
        if (this.worldTargets == null || mainFramebuffer.framebufferTexture <= 0 || !this.hasPostProgram()) {
            return;
        }

        for (int drawBuffer : drawBuffers) {
            if (drawBuffer == ActiniumPostTargets.TARGET_COLORTEX1 && this.worldTargets.hasSourceColorTexture()) {
                this.copyTexture(this.worldTargets.getSourceColorTexture(), mainFramebuffer.framebufferTexture);
                debugCheckGlErrors("world-stage.presentMainFramebuffer");
                return;
            }
        }
    }

    private @Nullable String getActivePackShaderSource() {
        String normalizedPath = "src/writebuffers.glsl".startsWith("/") ? "src/writebuffers.glsl".substring(1) : "src/writebuffers.glsl";
        return ActiniumShaderPackManager.getShaderSource("actinium:" + normalizedPath);
    }

    private void copyTexture(int sourceTexture, @Nullable Integer destinationTexture) {
        if (destinationTexture == null || sourceTexture <= 0) {
            return;
        }

        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        int readFramebuffer = GL30.glGenFramebuffers();
        int drawFramebuffer = GL30.glGenFramebuffers();

        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sourceTexture, 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, destinationTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL30.glDeleteFramebuffers(readFramebuffer);
            GL30.glDeleteFramebuffers(drawFramebuffer);
        }
    }

    private void copyFramebufferDepthToTexture(@Nullable Integer textureId) {
        if (textureId == null) {
            return;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, 0, 0, this.width, this.height, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void debugLog(String message, Object... args) {
        if (!ActiniumShaderPackManager.isDebugEnabled()) {
            return;
        }

        ActiniumShaders.logger().info("[DEBUG] " + message, args);
    }

    private static int nextFrameId(int current) {
        if (current == Integer.MAX_VALUE) {
            return 1;
        }

        return Math.max(1, current + 1);
    }

    private static boolean hasAnyStageProgram(String... programNames) {
        for (String programName : programNames) {
            if (hasProgram(programName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasProgram(String programName) {
        return ActiniumShaderPackManager.getProgramSource(programName, ShaderType.VERTEX) != null
                || ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT) != null;
    }

    private static void captureMatrix(int matrixType, Matrix4f destination) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        invokeGlGetFloat(matrixType, buffer);
        destination.set(buffer);
    }

    private void computeShadowMatrices(float partialTicks, ActiniumShaderProperties properties) {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft.world == null || minecraft.getRenderViewEntity() == null) {
            this.worldCameraPosition.zero();
            this.shadowCameraPosition.zero();
            this.shadowModelViewMatrix.identity();
            this.shadowProjectionMatrix.identity();
            this.shadowModelViewInverseMatrix.identity();
            this.shadowProjectionInverseMatrix.identity();
            return;
        }

        double camX = minecraft.getRenderViewEntity().lastTickPosX + (minecraft.getRenderViewEntity().posX - minecraft.getRenderViewEntity().lastTickPosX) * partialTicks;
        double camY = minecraft.getRenderViewEntity().lastTickPosY + (minecraft.getRenderViewEntity().posY - minecraft.getRenderViewEntity().lastTickPosY) * partialTicks + minecraft.getRenderViewEntity().getEyeHeight();
        double camZ = minecraft.getRenderViewEntity().lastTickPosZ + (minecraft.getRenderViewEntity().posZ - minecraft.getRenderViewEntity().lastTickPosZ) * partialTicks;
        this.worldCameraPosition.set(camX, camY, camZ);
        this.shadowCameraPosition.set(this.worldCameraPosition);

        float skyAngle = minecraft.world.getCelestialAngle(partialTicks);
        float sunAngle = skyAngle < 0.75f ? skyAngle + 0.25f : skyAngle - 0.75f;
        float shadowAngle = sunAngle > 0.5f ? sunAngle - 0.5f : sunAngle;
        float intervalSize = Math.max(0.0f, properties.getShadowIntervalSize());
        float shadowDistance = Math.max(16.0f, Math.min(
                properties.getShadowDistance(),
                minecraft.gameSettings.renderDistanceChunks * 16.0f
        ));
        float nearPlane = properties.getShadowNearPlane();
        float farPlane = Math.max(nearPlane + 1.0f, properties.getShadowFarPlane());

        this.shadowModelViewMatrix.identity()
                .translate(0.0f, 0.0f, -100.0f)
                .rotateX((float) Math.toRadians(90.0f))
                .rotateZ((float) Math.toRadians(shadowAngle * -360.0f))
                .rotateX((float) Math.toRadians(properties.getSunPathRotation()));

        if (intervalSize > 0.0f) {
            float halfInterval = intervalSize * 0.5f;
            float offsetX = (float) camX % intervalSize - halfInterval;
            float offsetY = (float) camY % intervalSize - halfInterval;
            float offsetZ = (float) camZ % intervalSize - halfInterval;
            this.shadowModelViewMatrix.translate(offsetX, offsetY, offsetZ);
        }

        this.shadowProjectionMatrix.identity().ortho(-shadowDistance, shadowDistance, -shadowDistance, shadowDistance, nearPlane, farPlane);
        this.shadowModelViewInverseMatrix.set(this.shadowModelViewMatrix).invert();
        this.shadowProjectionInverseMatrix.set(this.shadowProjectionMatrix).invert();
    }

    private int chooseShadowResolution(ActiniumShaderProperties properties) {
        int configured = properties.getShadowMapResolution();

        if (configured > 0) {
            return configured;
        }

        int maxDimension = Math.max(this.width, this.height);
        return maxDimension > 1024 ? 1024 : 512;
    }

    private boolean renderShadowTerrainPass(ActiniumShaderProperties properties, int resolution, float partialTicks) {
        CeleritasWorldRenderer renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer == null) {
            return false;
        }

        if (this.shadowTargets != null) {
            this.shadowTargets.ensureSize(resolution);
        }
        FloatBuffer previousProjection = copyFloatBuffer(ActiveRenderInfoAccessor.getProjectionMatrix());
        FloatBuffer previousModelView = copyFloatBuffer(ActiveRenderInfoAccessor.getModelViewMatrix());
        Minecraft minecraft = Minecraft.getMinecraft();
        ShadowPassGlState glState = ShadowPassGlState.capture();
        int shadowFrame = nextFrameId(this.shadowVisibilityFrameCounter);
        this.shadowVisibilityFrameCounter = shadowFrame;

        try {
            ActiveRenderInfoAccessor.setProjectionMatrix(toBuffer(this.shadowProjectionMatrix));
            ActiveRenderInfoAccessor.setModelViewMatrix(toBuffer(this.shadowModelViewMatrix));
            ActiniumInternalShadowRenderingState.begin(this.shadowModelViewMatrix, this.shadowProjectionMatrix);
            this.shadowTargets.beginWrite();
            RenderDevice.enterManagedCode();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.bindTexture(minecraft.getTextureMapBlocks().getGlTextureId());
            GlStateManager.enableTexture2D();
            minecraft.entityRenderer.enableLightmap();

            renderer.setupTerrain(
                    this.createShadowViewport(),
                    this.createShadowCameraState(partialTicks),
                    shadowFrame,
                    minecraft.player != null && minecraft.player.isSpectator(),
                    false
            );

            renderer.drawChunkLayer(BlockRenderLayer.SOLID, this.worldCameraPosition.x, this.worldCameraPosition.y, this.worldCameraPosition.z);
            renderer.drawChunkLayer(BlockRenderLayer.CUTOUT_MIPPED, this.worldCameraPosition.x, this.worldCameraPosition.y, this.worldCameraPosition.z);
            renderer.drawChunkLayer(BlockRenderLayer.CUTOUT, this.worldCameraPosition.x, this.worldCameraPosition.y, this.worldCameraPosition.z);

            if (properties.isShadowTranslucent()) {
                renderer.drawChunkLayer(BlockRenderLayer.TRANSLUCENT, this.worldCameraPosition.x, this.worldCameraPosition.y, this.worldCameraPosition.z);
            }

            this.shadowTargets.endWrite();
            return true;
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to render Actinium shadow terrain pass, falling back to cleared shadow targets", e);
            return false;
        } finally {
            minecraft.entityRenderer.disableLightmap();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            RenderDevice.exitManagedCode();
            ActiniumInternalShadowRenderingState.end();
            ActiveRenderInfoAccessor.setProjectionMatrix(previousProjection);
            ActiveRenderInfoAccessor.setModelViewMatrix(previousModelView);
            glState.restore(minecraft.getFramebuffer(), this.width, this.height);
        }
    }

    private Viewport createShadowViewport() {
        Matrix4f combinedMatrix = new Matrix4f(this.shadowProjectionMatrix).mul(this.shadowModelViewMatrix);
        FrustumIntersection frustum = new FrustumIntersection();
        frustum.set(combinedMatrix, true);
        return new Viewport(new SimpleFrustum(frustum), new Vector3d(this.worldCameraPosition));
    }

    private SimpleWorldRenderer.CameraState createShadowCameraState(float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        double camX = 0;
        if (minecraft.getRenderViewEntity() != null) {
            camX = minecraft.getRenderViewEntity().lastTickPosX + (minecraft.getRenderViewEntity().posX - minecraft.getRenderViewEntity().lastTickPosX) * partialTicks;
        }
        double camY = minecraft.getRenderViewEntity().lastTickPosY + (minecraft.getRenderViewEntity().posY - minecraft.getRenderViewEntity().lastTickPosY) * partialTicks + minecraft.getRenderViewEntity().getEyeHeight();
        double camZ = minecraft.getRenderViewEntity().lastTickPosZ + (minecraft.getRenderViewEntity().posZ - minecraft.getRenderViewEntity().lastTickPosZ) * partialTicks;
        Entity renderViewEntity = minecraft.getRenderViewEntity();
        return new SimpleWorldRenderer.CameraState(
                camX,
                camY,
                camZ,
                renderViewEntity.rotationPitch,
                renderViewEntity.rotationYaw,
                ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff()
        );
    }

    private static @Nullable Method findGlGetFloatBufferMethod() {
        try {
            Method method = GL11.class.getMethod("glGetFloat", int.class, FloatBuffer.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static @Nullable Method findGlGetIntegerBufferMethod() {
        try {
            Method method = GL11.class.getMethod("glGetInteger", int.class, IntBuffer.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void invokeGlGetFloat(int matrixType, FloatBuffer buffer) {
        if (GL_GET_FLOAT_BUFFER_METHOD == null) {
            throw new IllegalStateException("LWJGL GL11.glGetFloat(int, FloatBuffer) is unavailable");
        }

        try {
            GL_GET_FLOAT_BUFFER_METHOD.invoke(null, matrixType, buffer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to capture OpenGL matrix state", e);
        }
    }

    private static void invokeGlGetInteger(IntBuffer buffer) {
        if (GL_GET_INTEGER_BUFFER_METHOD == null) {
            throw new IllegalStateException("LWJGL GL11.glGetInteger(int, IntBuffer) is unavailable");
        }

        try {
            GL_GET_INTEGER_BUFFER_METHOD.invoke(null, GL11.GL_VIEWPORT, buffer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to capture OpenGL integer state", e);
        }
    }

    private static FloatBuffer toBuffer(Matrix4f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        matrix.get(buffer);
        buffer.flip();
        return buffer;
    }

    private static FloatBuffer copyFloatBuffer(FloatBuffer source) {
        FloatBuffer duplicate = BufferUtils.createFloatBuffer(source.capacity());
        FloatBuffer src = source.duplicate();
        src.clear();
        duplicate.put(src);
        duplicate.flip();
        return duplicate;
    }

    private static int[] parseDrawBuffers(String fragmentSource) {
        Matcher matcher = DRAW_BUFFERS_PATTERN.matcher(fragmentSource);
        String value = null;

        while (matcher.find()) {
            value = matcher.group(1);
        }

        if (value == null || value.isEmpty()) {
            return new int[]{ActiniumPostTargets.TARGET_COLORTEX1};
        }

        int[] drawBuffers = new int[value.length()];

        for (int i = 0; i < value.length(); i++) {
            drawBuffers[i] = value.charAt(i) - '0';
        }

        return drawBuffers;
    }

    private static String defaultVertexSource() {
        return String.join("\n",
                "#version 330 core",
                "out vec2 texcoord;",
                "void main() {",
                "    vec2 pos = vec2((gl_VertexID & 1) * 2 - 1, ((gl_VertexID >> 1) & 1) * 2 - 1);",
                "    texcoord = pos * 0.5 + 0.5;",
                "    gl_Position = vec4(pos, 0.0, 1.0);",
                "}",
                ""
        );
    }

    private static String defaultFragmentSource() {
        return String.join("\n",
                "#version 330 core",
                "uniform sampler2D colortex1;",
                "in vec2 texcoord;",
                "out vec4 fragColor0;",
                "void main() {",
                "    fragColor0 = texture(colortex1, texcoord);",
                "}",
                ""
        );
    }

    private static int createSolidTexture(int r, int g, int b, int a) {
        int texture = GL11.glGenTextures();
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        pixel.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
        pixel.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return texture;
    }

    private static int createNoiseTexture() {
        int texture = GL11.glGenTextures();
        int size = 64;
        ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);

        for (int i = 0; i < size * size; i++) {
            int value = (i * 73 + 19) & 0xFF;
            pixels.put((byte) value).put((byte) value).put((byte) value).put((byte) 255);
        }

        pixels.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return texture;
    }

    private static int createColorTexture(int width, int height) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static int createDepthTexture(int width, int height) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    static void debugCheckGlErrors(String context) {
        if (!ActiniumShaderPackManager.isDebugEnabled()) {
            return;
        }

        int error;
        boolean logged = false;

        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            logged = true;
            ActiniumShaders.logger().warn("[DEBUG] OpenGL error at {}: {} ({})", context, glErrorName(error), error);
        }

        if (logged) {
            ActiniumShaders.logger().warn("[DEBUG] OpenGL error stream consumed at {}", context);
        }
    }

    static void debugResetGlErrors(String context) {
        if (!ActiniumShaderPackManager.isDebugEnabled()) {
            return;
        }

        int cleared = drainGlErrors();

        if (cleared > 0) {
            ActiniumShaders.logger().info("[DEBUG] Cleared {} pre-existing OpenGL error(s) before {}", cleared, context);
        }
    }

    static void clearGlErrorsSilently() {
        drainGlErrors();
    }

    private static int drainGlErrors() {
        int error;
        int cleared = 0;

        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            cleared++;
        }

        return cleared;
    }

    private static String glErrorName(int error) {
        return switch (error) {
            case GL11.GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL11.GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL11.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL11.GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
            case GL11.GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
            case GL11.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            case GL30.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            default -> "UNKNOWN_GL_ERROR";
        };
    }

    private static void bindTexture(int unit, int textureId) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    private static void unbindTexture(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private static final class ShadowPassGlState {
        private final int framebufferBinding;
        private final int drawBuffer;
        private final int readBuffer;
        private final int activeTexture;
        private final int textureBinding2d;
        private final int currentProgram;
        private final int matrixMode;
        private final int shadeModel;
        private final boolean blendEnabled;
        private final boolean cullEnabled;
        private final boolean depthEnabled;
        private final boolean alphaEnabled;
        private final boolean fogEnabled;
        private final boolean texture2dEnabled;
        private final boolean depthMask;
        private final boolean attribStackPushed;
        private final boolean matrixStacksPushed;

        private ShadowPassGlState(int framebufferBinding,
                                  int drawBuffer,
                                  int readBuffer,
                                  int activeTexture,
                                  int textureBinding2d,
                                  int currentProgram,
                                  int matrixMode,
                                  int shadeModel,
                                  boolean blendEnabled,
                                  boolean cullEnabled,
                                  boolean depthEnabled,
                                  boolean alphaEnabled,
                                  boolean fogEnabled,
                                  boolean texture2dEnabled,
                                  boolean depthMask,
                                  boolean attribStackPushed,
                                  boolean matrixStacksPushed) {
            this.framebufferBinding = framebufferBinding;
            this.drawBuffer = drawBuffer;
            this.readBuffer = readBuffer;
            this.activeTexture = activeTexture;
            this.textureBinding2d = textureBinding2d;
            this.currentProgram = currentProgram;
            this.matrixMode = matrixMode;
            this.shadeModel = shadeModel;
            this.blendEnabled = blendEnabled;
            this.cullEnabled = cullEnabled;
            this.depthEnabled = depthEnabled;
            this.alphaEnabled = alphaEnabled;
            this.fogEnabled = fogEnabled;
            this.texture2dEnabled = texture2dEnabled;
            this.depthMask = depthMask;
            this.attribStackPushed = attribStackPushed;
            this.matrixStacksPushed = matrixStacksPushed;
        }

        public static ShadowPassGlState capture() {
            boolean attribStackPushed = false;
            boolean matrixStacksPushed = false;

            try {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                attribStackPushed = true;

                int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                GL11.glPushMatrix();
                GL11.glMatrixMode(matrixMode);
                matrixStacksPushed = true;
            } catch (RuntimeException ignored) {
            }

            return new ShadowPassGlState(
                    GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                    GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL11.GL_MATRIX_MODE),
                    GL11.glGetInteger(GL11.GL_SHADE_MODEL),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                    GL11.glIsEnabled(GL11.GL_FOG),
                    GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    attribStackPushed,
                    matrixStacksPushed
            );
        }

        public void restore(@Nullable Framebuffer mainFramebuffer, int fallbackWidth, int fallbackHeight) {
            if (this.matrixStacksPushed) {
                try {
                    GL11.glMatrixMode(GL11.GL_TEXTURE);
                    GL11.glPopMatrix();
                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPopMatrix();
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    GL11.glPopMatrix();
                } catch (RuntimeException ignored) {
                }
            }

            if (this.attribStackPushed) {
                try {
                    GL11.glPopAttrib();
                } catch (RuntimeException ignored) {
                }
            }

            GL13.glActiveTexture(this.activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureBinding2d);
            GL20.glUseProgram(this.currentProgram);
            GL11.glMatrixMode(this.matrixMode);
            GL11.glShadeModel(this.shadeModel);

            setEnabled(GL11.GL_BLEND, this.blendEnabled);
            setEnabled(GL11.GL_CULL_FACE, this.cullEnabled);
            setEnabled(GL11.GL_DEPTH_TEST, this.depthEnabled);
            setEnabled(GL11.GL_ALPHA_TEST, this.alphaEnabled);
            setEnabled(GL11.GL_FOG, this.fogEnabled);
            setEnabled(GL11.GL_TEXTURE_2D, this.texture2dEnabled);
            GL11.glDepthMask(this.depthMask);

            if (this.framebufferBinding != 0) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferBinding);
            } else if (mainFramebuffer != null) {
                mainFramebuffer.bindFramebuffer(true);
            } else {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            }

            GL11.glDrawBuffer(this.drawBuffer);
            GL11.glReadBuffer(this.readBuffer);
            GL11.glViewport(0, 0, Math.max(1, fallbackWidth), Math.max(1, fallbackHeight));
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }

    private static final class WorldStageGlState {
        private final int framebufferBinding;
        private final int drawBuffer;
        private final int readBuffer;
        private final int activeTexture;
        private final int textureBinding2d;
        private final int currentProgram;
        private final int[] viewport;

        private WorldStageGlState(int framebufferBinding,
                                  int drawBuffer,
                                  int readBuffer,
                                  int activeTexture,
                                  int textureBinding2d,
                                  int currentProgram,
                                  int[] viewport) {
            this.framebufferBinding = framebufferBinding;
            this.drawBuffer = drawBuffer;
            this.readBuffer = readBuffer;
            this.activeTexture = activeTexture;
            this.textureBinding2d = textureBinding2d;
            this.currentProgram = currentProgram;
            this.viewport = viewport;
        }

        public static WorldStageGlState capture() {
            IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
            invokeGlGetInteger(viewportBuffer);

            return new WorldStageGlState(
                    GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                    GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    new int[]{
                            viewportBuffer.get(0),
                            viewportBuffer.get(1),
                            viewportBuffer.get(2),
                            viewportBuffer.get(3)
                    }
            );
        }

        public void restore() {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferBinding);
            GL11.glDrawBuffer(this.drawBuffer);
            GL11.glReadBuffer(this.readBuffer);
            GL20.glUseProgram(this.currentProgram);
            GL13.glActiveTexture(this.activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureBinding2d);
            GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
        }
    }
}
