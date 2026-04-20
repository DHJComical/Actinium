package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pack.ActiniumShaderProperties;
import com.dhj.actinium.shader.pack.ActiniumShaderPackResources;
import com.dhj.actinium.shadows.ActiniumInternalShadowRenderingState;
import com.dhj.actinium.shadows.ActiniumShadowRenderingState;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
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
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
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
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final float TAA_JITTER_SCALE = 0.35f;
    private static final float[] TAA_OFFSET_SEQUENCE_X = {
            0.5f, -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, -0.5f, 0.5f
    };
    private static final float[] TAA_OFFSET_SEQUENCE_Y = {
            0.5f, -0.5f, 0.5f, -0.5f,
            0.5f, -0.5f, 0.5f, -0.5f,
            0.5f, -0.5f, 0.5f, -0.5f,
            0.5f, -0.5f, 0.5f, -0.5f
    };

    private static final Pattern LEGACY_PACK_MARKERS = Pattern.compile("gl_Vertex|gl_MultiTexCoord|gl_FragData|gl_FragColor|gl_ModelViewProjectionMatrix|gl_TextureMatrix|\\bvarying\\b");
    private static final Pattern DRAW_BUFFERS_PATTERN = Pattern.compile("DRAWBUFFERS:([0-7]+)");
    private static final Pattern DECLARED_TARGET_FORMAT_PATTERN = Pattern.compile("const\\s+int\\s+([A-Za-z0-9_]+)Format\\s*=\\s*([A-Z0-9_]+)\\s*;");
    private static final Pattern DECLARED_TARGET_CLEAR_PATTERN = Pattern.compile("const\\s+bool\\s+([A-Za-z0-9_]+)Clear\\s*=\\s*(true|false)\\s*;", Pattern.CASE_INSENSITIVE);
    private static final Pattern DECLARED_TARGET_CLEAR_COLOR_PATTERN = Pattern.compile("const\\s+vec4\\s+([A-Za-z0-9_]+)ClearColor\\s*=\\s*vec4\\s*\\(([^)]*)\\)\\s*;");
    private static final Pattern CONDITIONAL_SOFT_LOD_UNIFORM_PATTERN = Pattern.compile("(?ms)^\\s*#ifdef\\s+BLOOM\\s*\\R\\s*uniform\\s+float\\s+softLod\\s*;\\s*\\R\\s*#endif\\s*");
    private static final Pattern SOFT_LOD_UNIFORM_PATTERN = Pattern.compile("(?m)^\\s*uniform\\s+float\\s+softLod\\s*;");
    private static final Pattern VERSION_LINE_PATTERN = Pattern.compile("(?m)^\\s*#version\\s+.+$");
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
    private static final String[] PRE_SCENE_POST_PROGRAMS = {
            "prepare",
            "prepare1",
            "prepare2",
            "prepare3",
            "prepare4",
            "prepare5",
            "prepare6",
            "prepare7",
            "deferred",
            "deferred1",
            "deferred2",
            "deferred3",
            "deferred4",
            "deferred5",
            "deferred6",
            "deferred7"
    };
    private static final String[] SCENE_POST_PROGRAMS = {
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
    private static final String[] LEGACY_RENDER_TARGET_NAMES = {
            "gcolor",
            "gdepth",
            "gnormal",
            "composite",
            "gaux1",
            "gaux2",
            "gaux3",
            "gaux4"
    };
    private static final int TRACKED_TEXTURE_UNITS = 16;
    private static final boolean ENABLE_PRE_SCENE_PIPELINES = true;
    private static final boolean ENABLE_PRE_SCENE_SHADER_EXECUTION = false;
    private static final boolean ENABLE_PREPARE_SHADER_EXECUTION = true;
    private static final boolean ENABLE_DEFERRED_SHADER_EXECUTION = false;
    private static final boolean ENABLE_EXTERNAL_SCENE_PIPELINE = true;
    private static final boolean ENABLE_EXTERNAL_FINAL_PIPELINE = true;
    private static final boolean ENABLE_EXTERNAL_TERRAIN_REDIRECT = false;

    private int observedReloadVersion = -1;
    @Getter
    private ActiniumRenderStage currentStage = ActiniumRenderStage.NONE;
    private boolean shadowProgramAvailable;
    private boolean skyProgramAvailable;
    private boolean weatherProgramAvailable;
    private boolean postProgramAvailable;
    private boolean loggedCapabilities;
    private boolean sceneProgramsResolved;
    private boolean preSceneProgramsResolved;
    private boolean finalProgramResolved;
    private boolean loggedPostProgramUse;
    private boolean loggedFinalProgramUse;
    @Getter
    private int frameCounter;
    private int shadowVisibilityFrameCounter;
    private long lastFrameNanos;
    @Getter
    private float frameTimeCounterSeconds;
    private float currentFrameDeltaSeconds;
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
    private final Matrix4f previousGbufferModelViewMatrix = new Matrix4f();
    private final Matrix4f previousGbufferProjectionMatrix = new Matrix4f();
    private final Matrix4f servedPreviousGbufferModelViewMatrix = new Matrix4f();
    private final Matrix4f servedPreviousGbufferProjectionMatrix = new Matrix4f();
    private final Vector3f scratchVector = new Vector3f();
    private final Vector3f scratchTaaOffset = new Vector3f();
    private final Vector3d shadowCameraPosition = new Vector3d();
    private final Vector3d worldCameraPosition = new Vector3d();
    @Getter
    private final Vector3d previousWorldCameraPosition = new Vector3d();
    private final Vector3d servedPreviousWorldCameraPosition = new Vector3d();
    @Getter
    private final float[] fogColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    private @Nullable GlVertexArray fullscreenVertexArray;
    private int fullscreenQuadBuffer;
    private @Nullable ActiniumPostTargets postTargets;
    private @Nullable ActiniumPostTargets preSceneTargets;
    private @Nullable ActiniumShadowTargets shadowTargets;
    private @Nullable ActiniumWorldTargets worldTargets;
    private @Nullable ActiniumWorldProgram skyProgram;
    private @Nullable ActiniumWorldProgram skyTexturedProgram;
    private @Nullable ActiniumWorldProgram cloudsProgram;
    private @Nullable ActiniumWorldProgram weatherProgram;
    private @Nullable ActiniumWorldProgram activeWorldProgram;
    private @Nullable WorldStageGlState worldStageGlState;
    private @Nullable GlProgram<ActiniumPostShaderInterface> finalProgram;
    private int[] finalProgramMipmappedBuffers = new int[0];
    private Map<Integer, Boolean> finalProgramExplicitFlips = Collections.emptyMap();
    private @Nullable GlProgram<ActiniumPostShaderInterface> blitProgram;
    private @Nullable Integer whiteTexture;
    private @Nullable Integer noiseTexture;
    private @Nullable Integer terrainGaux2Texture;
    private @Nullable Integer terrainGaux1Texture;
    private @Nullable Integer terrainDepthTexture0;
    private @Nullable Integer terrainDepthTexture1;
    private final Map<String, Integer> packTextureCache = new HashMap<>();
    private @Nullable String activePostTextureStage;
    private List<ActiniumPostProgram> preScenePrograms = Collections.emptyList();
    private List<ActiniumPostProgram> scenePrograms = Collections.emptyList();
    private boolean worldTargetsPrepared;
    private boolean prepareProgramsExecutedThisFrame;
    private boolean deferredProgramsExecutedThisFrame;
    private boolean preTranslucentDepthCapturedThisFrame;
    private int previousUniformFrame = Integer.MIN_VALUE;
    private boolean previousUniformInitialized;
    @Getter
    private float centerDepthSmooth;
    private ActiniumRenderPipeline() {
    }

    public void beginWorld(float partialTicks) {
        this.syncReloadState();
        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer != null) {
            this.width = Math.max(1, framebuffer.framebufferWidth);
            this.height = Math.max(1, framebuffer.framebufferHeight);
        } else {
            this.width = Math.max(1, minecraft.displayWidth);
            this.height = Math.max(1, minecraft.displayHeight);
        }

        long now = System.nanoTime();
        this.currentFrameDeltaSeconds = this.lastFrameNanos == 0L ? 0.0f : Math.max(0.0f, (now - this.lastFrameNanos) / 1_000_000_000.0f);
        this.lastFrameNanos = now;
        this.frameTimeCounterSeconds += this.currentFrameDeltaSeconds;
        if (this.frameTimeCounterSeconds >= 3600.0f) {
            this.frameTimeCounterSeconds = 0.0f;
        }

        this.captureInterpolatedWorldCameraPosition(partialTicks);
        this.frameCounter = nextFrameId(this.frameCounter);
        this.worldTargetsPrepared = false;
        this.prepareProgramsExecutedThisFrame = false;
        this.deferredProgramsExecutedThisFrame = false;
        this.preTranslucentDepthCapturedThisFrame = false;
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
        this.applyTemporalJitterToProjection(this.gbufferProjectionMatrix);
        this.gbufferModelViewInverseMatrix.set(this.gbufferModelViewMatrix).invert();
        this.gbufferProjectionInverseMatrix.set(this.gbufferProjectionMatrix).invert();
        System.arraycopy(ChunkShaderFogComponent.FOG_SERVICE.getFogColor(), 0, this.fogColor, 0, this.fogColor.length);
    }

    private void captureInterpolatedWorldCameraPosition(float partialTicks) {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null) {
            this.worldCameraPosition.zero();
            return;
        }

        double cameraX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double cameraY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + entity.getEyeHeight();
        double cameraZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        this.worldCameraPosition.set(cameraX, cameraY, cameraZ);
    }

    private void applyTemporalJitterToProjection(Matrix4f projectionMatrix) {
        if (!this.isTemporalAntiAliasingActive()) {
            return;
        }

        float offsetX = this.getTaaOffsetX();
        float offsetY = this.getTaaOffsetY();

        projectionMatrix.m00(projectionMatrix.m00() + offsetX * projectionMatrix.m03());
        projectionMatrix.m10(projectionMatrix.m10() + offsetX * projectionMatrix.m13());
        projectionMatrix.m20(projectionMatrix.m20() + offsetX * projectionMatrix.m23());
        projectionMatrix.m30(projectionMatrix.m30() + offsetX * projectionMatrix.m33());

        projectionMatrix.m01(projectionMatrix.m01() + offsetY * projectionMatrix.m03());
        projectionMatrix.m11(projectionMatrix.m11() + offsetY * projectionMatrix.m13());
        projectionMatrix.m21(projectionMatrix.m21() + offsetY * projectionMatrix.m23());
        projectionMatrix.m31(projectionMatrix.m31() + offsetY * projectionMatrix.m33());
    }

    public void capturePreTranslucentDepth() {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled() || !this.hasPostProgram() || this.preTranslucentDepthCapturedThisFrame) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer mainFramebuffer = minecraft.getFramebuffer();

        if (mainFramebuffer == null || mainFramebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, mainFramebuffer.framebufferWidth);
        this.height = Math.max(1, mainFramebuffer.framebufferHeight);
        this.ensureRuntimeResources();

        if (this.postTargets == null) {
            return;
        }

        this.postTargets.ensureSize(this.width, this.height);
        this.postTargets.copyPreTranslucentDepth(mainFramebuffer);
        this.preTranslucentDepthCapturedThisFrame = true;
    }

    public void captureSkyStageState() {
        this.syncReloadState();
        captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.skyStageModelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, this.skyStageProjectionMatrix);
        this.applyTemporalJitterToProjection(this.skyStageProjectionMatrix);
        this.skyStageModelViewInverseMatrix.set(this.skyStageModelViewMatrix).invert();
        this.skyStageProjectionInverseMatrix.set(this.skyStageProjectionMatrix).invert();
    }

    public void renderPostPipeline(float partialTicks) {
        this.syncReloadState();

        if (!ENABLE_EXTERNAL_SCENE_PIPELINE && !ENABLE_EXTERNAL_FINAL_PIPELINE) {
            return;
        }

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
        ShadowPassGlState previousState = ShadowPassGlState.capture();
        this.beginPost();

        try {
            if (this.postTargets != null) {
                Integer worldGaux4Texture = null;

                if (this.worldTargets != null && this.worldTargets.hasSourceColorTexture()) {
                    worldGaux4Texture = this.worldTargets.getSourceGaux4Texture();
                }

                this.postTargets.ensureSize(this.width, this.height);
                this.postTargets.copySceneTextures(mainFramebuffer, worldGaux4Texture);
                this.applyExplicitPreFlips(this.postTargets, "composite_pre");

                if (!this.preTranslucentDepthCapturedThisFrame) {
                    this.postTargets.copyPreTranslucentDepth(mainFramebuffer);
                }

                this.debugLogPreSceneTargets();
                this.updateCenterDepthSmooth();
            }
            this.executeScenePrograms(scenePrograms, partialTicks);
            this.renderFinalPass(mainFramebuffer, finalProgram, partialTicks);
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to execute Actinium shader pack post pipeline; disabling external post programs until the next shader reload", e);
            this.disableExternalProgramsUntilReload();
        } finally {
            previousState.restore(mainFramebuffer, this.width, this.height);
            this.restoreScreenRenderState(mainFramebuffer, this.width, this.height);
            this.endPost();
        }
    }

    public void renderPreparePipeline(float partialTicks) {
        if (!ENABLE_PRE_SCENE_PIPELINES) {
            return;
        }

        if (this.prepareProgramsExecutedThisFrame) {
            return;
        }

        this.renderPreCompositePipeline(partialTicks, this.getPreparePrograms(), true, "prepare");
        this.prepareProgramsExecutedThisFrame = true;
    }

    public void renderDeferredPipeline(float partialTicks) {
        if (!ENABLE_PRE_SCENE_PIPELINES) {
            return;
        }

        if (this.deferredProgramsExecutedThisFrame) {
            return;
        }

        this.renderPreCompositePipeline(partialTicks, this.getDeferredPrograms(), !this.prepareProgramsExecutedThisFrame, "deferred");
        this.deferredProgramsExecutedThisFrame = true;
    }

    private void renderPreCompositePipeline(float partialTicks, List<ActiniumPostProgram> programs, boolean initializeTargets, String stageName) {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled() || programs.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer mainFramebuffer = minecraft.getFramebuffer();

        if (mainFramebuffer == null || mainFramebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, mainFramebuffer.framebufferWidth);
        this.height = Math.max(1, mainFramebuffer.framebufferHeight);
        this.ensureRuntimeResources();
        ShadowPassGlState previousState = ShadowPassGlState.capture();
        this.beginPost();

        try {
            if (this.preSceneTargets != null) {
                this.preSceneTargets.ensureSize(this.width, this.height);

                if (initializeTargets) {
                    this.preSceneTargets.copySceneTextures(mainFramebuffer, null);
                } else {
                    this.preSceneTargets.copySceneInputs(mainFramebuffer);
                }

                this.applyExplicitPreFlips(this.preSceneTargets, stageName + "_pre");

                this.updateCenterDepthSmooth();
                this.debugLogTextureCenter(stageName + ".entry.colortex1", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
                this.debugLogTextureCenter(stageName + ".entry.gaux4", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
            }

            if (this.shouldExecutePreSceneShaders(stageName) && this.preSceneTargets != null) {
                this.executePostPrograms(programs, this.preSceneTargets, partialTicks);
            } else if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog("Skipping pre-scene shader execution for '{}' while keeping pre-pass buffer initialization active", stageName);
            }
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to execute Actinium shader pack {} pipeline; disabling external post programs until the next shader reload", stageName, e);
            this.disableExternalProgramsUntilReload();
        } finally {
            previousState.restore(mainFramebuffer, this.width, this.height);
            this.endPost();
        }
    }

    private void restoreMainFramebufferState(Framebuffer mainFramebuffer) {
        if (mainFramebuffer != null) {
            mainFramebuffer.bindFramebuffer(true);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glDrawBuffer(GL11.GL_BACK);
            GL11.glReadBuffer(GL11.GL_BACK);
        }

        GL20.glUseProgram(0);

        for (int unit = TRACKED_TEXTURE_UNITS - 1; unit >= 0; unit--) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
    }

    private void restoreScreenRenderState(@Nullable Framebuffer framebuffer, int width, int height) {
        this.restoreMainFramebufferState(framebuffer);
        GL11.glViewport(0, 0, Math.max(1, width), Math.max(1, height));
        GL30.glBindVertexArray(0);
        GL11.glColorMask(true, true, true, true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
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

    public boolean shouldSuppressVanillaSkyGeometry() {
        this.syncReloadState();
        return ActiniumShaderPackManager.areShadersEnabled()
                && this.shouldUseExternalWorldPrograms()
                && this.skyProgramAvailable
                && this.postProgramAvailable;
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

    public Matrix4fc getPreviousGbufferModelViewMatrix() {
        this.updatePreviousUniformSnapshots();
        return this.servedPreviousGbufferModelViewMatrix;
    }

    public Matrix4fc getPreviousGbufferProjectionMatrix() {
        this.updatePreviousUniformSnapshots();
        return this.servedPreviousGbufferProjectionMatrix;
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

    public Vector3d getWorldCameraPosition() {
        return this.worldCameraPosition;
    }

    public Vector3d getServedPreviousWorldCameraPosition() {
        this.updatePreviousUniformSnapshots();
        return this.servedPreviousWorldCameraPosition;
    }

    public boolean isTemporalAntiAliasingActive() {
        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return false;
        }

        String aaType = ActiniumShaderPackManager.getEffectiveOptionValue("AA_TYPE");

        if (aaType == null) {
            return false;
        }

        try {
            return Integer.parseInt(aaType.trim()) >= 2;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void updatePreviousUniformSnapshots() {
        if (this.previousUniformFrame == this.frameCounter) {
            return;
        }

        this.previousUniformFrame = this.frameCounter;

        if (!this.previousUniformInitialized) {
            this.previousGbufferModelViewMatrix.set(this.gbufferModelViewMatrix);
            this.previousGbufferProjectionMatrix.set(this.gbufferProjectionMatrix);
            this.previousWorldCameraPosition.set(this.worldCameraPosition);
            this.previousUniformInitialized = true;
        }

        this.servedPreviousGbufferModelViewMatrix.set(this.previousGbufferModelViewMatrix);
        this.servedPreviousGbufferProjectionMatrix.set(this.previousGbufferProjectionMatrix);
        this.servedPreviousWorldCameraPosition.set(this.previousWorldCameraPosition);

        this.previousGbufferModelViewMatrix.set(this.gbufferModelViewMatrix);
        this.previousGbufferProjectionMatrix.set(this.gbufferProjectionMatrix);
        this.previousWorldCameraPosition.set(this.worldCameraPosition);
    }

    public Matrix4f getTemporalJitteredProjection(Matrix4fc source, Matrix4f destination) {
        destination.set(source);
        this.applyTemporalJitterToProjection(destination);
        return destination;
    }

    public int getFrameMod() {
        return Math.floorMod(this.frameCounter, 16);
    }

    public float getDitherShift() {
        final float[] sequence = {
                0.0625f, 0.4375f, 0.875f, 0.625f,
                0.25f, 0.8125f, 0.125f, 0.9375f,
                0.3125f, 0.5f, 0.375f, 0.5625f,
                0.75f, 0.6875f, 0.1875f, 0.0f
        };
        return sequence[this.getFrameMod()];
    }

    public float getSoftLod() {
        float softLodScale = Math.max(1.0f, this.height / 128.0f);
        return Math.max(0.0f, (float) (Math.log(softLodScale) / Math.log(2.0)));
    }

    public float getTaaOffsetX() {
        return TAA_JITTER_SCALE * TAA_OFFSET_SEQUENCE_X[this.getFrameMod()]
                / Math.max(1, this.width > 0 ? this.width : Minecraft.getMinecraft().displayWidth);
    }

    public float getTaaOffsetY() {
        return TAA_JITTER_SCALE * TAA_OFFSET_SEQUENCE_Y[this.getFrameMod()]
                / Math.max(1, this.height > 0 ? this.height : Minecraft.getMinecraft().displayHeight);
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
        if (ENABLE_EXTERNAL_TERRAIN_REDIRECT && this.hasPostProgram()) {
            this.prepareWorldTargets(framebuffer);
            debugCheckGlErrors("pipeline.prepareTerrainInputs.prepareWorldTargets");
        }
        this.ensureTerrainInputTextures();
        debugCheckGlErrors("pipeline.prepareTerrainInputs.ensureTerrainInputTextures");

        framebuffer.bindFramebuffer(true);
        this.copyTexture(framebuffer.framebufferTexture, this.terrainGaux1Texture);
        this.copyFramebufferDepthToTexture(this.terrainDepthTexture0);
        this.copyFramebufferDepthToTexture(this.terrainDepthTexture1);
    }

    public void bindTerrainInputTextures() {
        int gaux1Texture = this.terrainGaux1Texture != null ? this.terrainGaux1Texture : 0;
        int gaux2Texture = this.getGbuffersTextureOverride("gaux2", this.terrainGaux2Texture);
        int depthtex0Texture = this.terrainDepthTexture0 != null ? this.terrainDepthTexture0 : 0;
        int depthtex1Texture = this.terrainDepthTexture1 != null ? this.terrainDepthTexture1 : 0;
        int noiseTextureId = this.getGbuffersTextureOverride("noisetex", this.noiseTexture);

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

    public void bindTerrainPassFramebuffer(TerrainRenderPass pass) {
        this.syncReloadState();

        if (!ENABLE_EXTERNAL_TERRAIN_REDIRECT) {
            return;
        }

        if (!pass.isReverseOrder()) {
            return;
        }

        if (!ActiniumShaderPackManager.areShadersEnabled() || !this.hasPostProgram() || ActiniumShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer == null || framebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, framebuffer.framebufferWidth);
        this.height = Math.max(1, framebuffer.framebufferHeight);
        this.ensureRuntimeResources();
        this.prepareWorldTargets(framebuffer);

        if (this.worldTargets != null) {
            if (this.worldTargets.getSourceColorTexture() > 0) {
                this.copyTexture(framebuffer.framebufferTexture, this.worldTargets.getSourceColorTexture());
                debugCheckGlErrors("terrain.primeFramebuffer:" + pass.name());
            }
            this.worldTargets.bindWriteFramebuffer(framebuffer, new int[]{ActiniumPostTargets.TARGET_COLORTEX1}, false);
            debugCheckGlErrors("terrain.bindFramebuffer:" + pass.name());
        }
    }

    public void unbindTerrainPassFramebuffer(TerrainRenderPass pass) {
        this.syncReloadState();

        if (!ENABLE_EXTERNAL_TERRAIN_REDIRECT) {
            return;
        }

        if (!pass.isReverseOrder()) {
            return;
        }

        if (!ActiniumShaderPackManager.areShadersEnabled() || !this.hasPostProgram() || ActiniumShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();

        if (framebuffer == null || framebuffer.framebufferTexture <= 0 || this.worldTargets == null) {
            return;
        }

        this.worldTargets.endWrite(framebuffer, new int[]{ActiniumPostTargets.TARGET_COLORTEX1}, false);
        this.debugLogTerrainPassState("after-endWrite", pass, framebuffer);
        this.presentTerrainPassResult(framebuffer, new int[]{ActiniumPostTargets.TARGET_COLORTEX1});
        this.debugLogTerrainPassState("after-present", pass, framebuffer);
        debugCheckGlErrors("terrain.unbindFramebuffer:" + pass.name());
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
        this.applyTemporalJitterToProjection(projectionMatrix);
        projectionInverseMatrix.set(projectionMatrix).invert();

        if (this.activeWorldProgram != null) {
            this.unbindWorldStageProgram();
        }

        this.worldStageGlState = WorldStageGlState.capture();
        debugCheckGlErrors("world-stage.capture:" + program.name());
        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Binding world-stage program '{}' for {} with draw buffers {}", program.name(), this.currentStage, Arrays.toString(program.drawBuffers()));
        }
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

    private void executePostPrograms(List<ActiniumPostProgram> programs, float partialTicks) {
        if (this.postTargets != null) {
            this.executePostPrograms(programs, this.postTargets, partialTicks);
        }
    }

    private void executePostPrograms(List<ActiniumPostProgram> programs, ActiniumPostTargets targets, float partialTicks) {
        for (ActiniumPostProgram program : programs) {
            this.preparePostMipmappedInputs(targets, program.mipmappedBuffers());
            targets.bindWriteFramebuffer(program.drawBuffers());
            GL11.glViewport(0, 0, this.width, this.height);
            this.renderFullscreenProgram(program.program(), targets, partialTicks, program.name());
            this.applyPostProgramFlips(targets, program);
            this.debugLogPostStage(program.name());
        }
    }

    private void executeScenePrograms(List<ActiniumPostProgram> scenePrograms, float partialTicks) {
        this.executePostPrograms(scenePrograms, partialTicks);
    }

    private void renderFinalPass(Framebuffer mainFramebuffer, @Nullable GlProgram<ActiniumPostShaderInterface> program, float partialTicks) {
        this.debugLogRenderState("final.prep.before");
        if (this.postTargets != null) {
            this.debugLogTextureSamples("final.input.colortex1", this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
        }

        this.prepareMainFramebufferForFinalPass(mainFramebuffer);
        this.debugLogRenderState("final.prep.after");

        if (program != null) {
            this.preparePostMipmappedInputs(this.finalProgramMipmappedBuffers);
            this.renderFullscreenProgram(program, partialTicks, "final");
            this.applyFinalProgramFlips();
            this.debugLogFramebufferCenter("final.mainFramebuffer", mainFramebuffer.framebufferObject, GL30.GL_COLOR_ATTACHMENT0);
            this.debugLogFramebufferSamples("final.mainFramebuffer", mainFramebuffer.framebufferObject, GL30.GL_COLOR_ATTACHMENT0);
            this.finishMainFramebufferFinalPass();
            return;
        }

        this.renderBlitFallback(mainFramebuffer, partialTicks);
        this.finishMainFramebufferFinalPass();
    }

    private void renderBlitFallback(Framebuffer mainFramebuffer, float partialTicks) {
        GlProgram<ActiniumPostShaderInterface> program = this.getBlitProgram();
        this.renderFullscreenProgram(program, partialTicks, null);
    }

    private boolean shouldExecutePreSceneShaders(String stageName) {
        if (!ENABLE_PRE_SCENE_SHADER_EXECUTION) {
            return ENABLE_PREPARE_SHADER_EXECUTION && "prepare".equals(stageName)
                    || ENABLE_DEFERRED_SHADER_EXECUTION && "deferred".equals(stageName);
        }

        return true;
    }

    private void debugLogPreSceneTargets() {
        if (!ActiniumShaderPackManager.isDebugEnabled() || !this.shouldEmitVerboseDebugFrame() || this.preSceneTargets == null) {
            return;
        }

        this.debugLogTextureSamples("pre-scene.capture.colortex1", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
        this.debugLogTextureSamples("pre-scene.capture.gaux4", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
    }

    private void applyExplicitPreFlips(ActiniumPostTargets targets, String stageName) {
        if (targets == null) {
            return;
        }

        Map<Integer, Boolean> explicitFlips = ActiniumShaderPackManager.getActiveShaderProperties().getExplicitFlips(stageName);
        explicitFlips.forEach((targetIndex, shouldFlip) -> {
            if (Boolean.TRUE.equals(shouldFlip)) {
                targets.flipTarget(targetIndex);
            }
        });
    }

    private void applyPostProgramFlips(ActiniumPostTargets targets, ActiniumPostProgram program) {
        if (targets == null) {
            return;
        }

        for (int drawBuffer : program.drawBuffers()) {
            if (program.explicitFlips().get(drawBuffer) == Boolean.FALSE) {
                continue;
            }

            targets.flipTarget(drawBuffer);
        }

        program.explicitFlips().forEach((targetIndex, shouldFlip) -> {
            if (Boolean.TRUE.equals(shouldFlip)) {
                targets.flipTarget(targetIndex);
            }
        });
    }

    private void applyFinalProgramFlips() {
        if (this.postTargets == null || this.finalProgramExplicitFlips.isEmpty()) {
            return;
        }

        this.finalProgramExplicitFlips.forEach((targetIndex, shouldFlip) -> {
            if (Boolean.TRUE.equals(shouldFlip)) {
                this.postTargets.flipTarget(targetIndex);
            }
        });
    }

    private void prepareMainFramebufferForFinalPass(Framebuffer mainFramebuffer) {
        mainFramebuffer.bindFramebuffer(true);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glViewport(0, 0, this.width, this.height);
        GL11.glColorMask(true, true, true, true);
        GlStateManager.clearColor(this.fogColor[0], this.fogColor[1], this.fogColor[2], 1.0f);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_ALWAYS);
        GlStateManager.depthMask(false);
    }

    private void finishMainFramebufferFinalPass() {
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
    }

    private void renderFullscreenProgram(GlProgram<ActiniumPostShaderInterface> program, float partialTicks, @Nullable String textureStage) {
        if (this.postTargets != null) {
            this.renderFullscreenProgram(program, this.postTargets, partialTicks, textureStage);
        }
    }

    private void renderFullscreenProgram(GlProgram<ActiniumPostShaderInterface> program, ActiniumPostTargets targets, float partialTicks, @Nullable String textureStage) {
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.depthMask(false);
        GL11.glColorMask(true, true, true, true);

        program.bind();
        program.getInterface().setupState(this, targets, partialTicks, this.currentFrameDeltaSeconds, this.frameTimeCounterSeconds, this.frameCounter);
        this.activePostTextureStage = textureStage;
        this.bindPipelineTextures(targets);
        this.debugLogRenderState("fullscreen." + (textureStage != null ? textureStage : "blit") + ".beforeDraw");

        if (this.fullscreenVertexArray != null) {
            GL30.glBindVertexArray(this.fullscreenVertexArray.handle());
        }
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        this.unbindPipelineTextures();
        this.activePostTextureStage = null;
        program.unbind();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
    }

    private void bindPipelineTextures(ActiniumPostTargets targets) {
        if (targets != null) {
            this.bindPipelineTexture("colortex0", 0, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX0));
            this.bindPipelineTexture("colortex1", 1, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
            this.bindPipelineTexture("colortex2", 2, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX2));
            this.bindPipelineTexture("colortex3", 3, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX3));
            this.bindPipelineTexture("gaux1", 4, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX1));
            this.bindPipelineTexture("gaux2", 5, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX2));
            this.bindPipelineTexture("gaux3", 6, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX3));
            this.bindPipelineTexture("gaux4", 7, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
            bindTexture(8, targets.getDepthTexture(0));
            bindTexture(9, targets.getDepthTexture(1));
            this.bindPipelineTexture("noisetex", 10, this.noiseTexture);
            this.bindPipelineTexture("shadowtex0", TERRAIN_SHADOW_TEX0_UNIT, this.shadowTargets != null ? this.shadowTargets.getDepthTexture(0) : this.whiteTexture);
            this.bindPipelineTexture("shadowtex1", TERRAIN_SHADOW_TEX1_UNIT, this.shadowTargets != null ? this.shadowTargets.getDepthTexture(1) : this.whiteTexture);
            this.bindPipelineTexture("shadowcolor0", TERRAIN_SHADOW_COLOR0_UNIT, this.shadowTargets != null ? this.shadowTargets.getColorTexture() : this.whiteTexture);
        }
    }

    private void bindPipelineTexture(String samplerName, int unit, @Nullable Integer fallbackTexture) {
        bindTexture(unit, this.getPostTextureOverride(samplerName, fallbackTexture));
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

    private void preparePostMipmappedInputs(int[] mipmappedBuffers) {
        this.preparePostMipmappedInputs(this.postTargets, mipmappedBuffers);
    }

    private void preparePostMipmappedInputs(@Nullable ActiniumPostTargets targets, int[] mipmappedBuffers) {
        if (targets == null) {
            return;
        }

        for (int targetIndex : mipmappedBuffers) {
            this.generateTextureMipmaps(targets.getSourceTexture(targetIndex), false);
        }
    }

    private void generateTextureMipmaps(int textureId, boolean integerTexture) {
        if (textureId <= 0) {
            return;
        }

        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, integerTexture ? GL11.GL_NEAREST_MIPMAP_NEAREST : GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousBinding);
        GL13.glActiveTexture(previousActiveTexture);
    }

    private void debugLogPostStage(String programName) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || this.postTargets == null) {
            return;
        }

        int colortex1 = this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1);
        int gaux3 = this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX3);
        this.debugLogTextureCenter(programName + ".colortex1", colortex1);
        this.debugLogTextureCenter(programName + ".gaux3", gaux3);
        this.debugLogTextureSamples(programName + ".colortex1", colortex1);
    }

    private void debugLogTextureCenter(String label, int textureId) {
        if (!this.shouldEmitVerboseDebugFrame() || textureId <= 0) {
            return;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int framebuffer = GL30.glGenFramebuffers();
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glReadPixels(Math.max(0, this.width / 2), Math.max(0, this.height / 2), 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
            int r = pixel.get(0) & 0xFF;
            int g = pixel.get(1) & 0xFF;
            int b = pixel.get(2) & 0xFF;
            int a = pixel.get(3) & 0xFF;
            this.debugLog("Post debug '{}' center pixel rgba=[{}, {}, {}, {}]", label, r, g, b, a);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    private int getGbuffersTextureOverride(String samplerName, @Nullable Integer fallbackTexture) {
        return this.getDeclaredStageTexture("gbuffers", samplerName, fallbackTexture);
    }

    private int getPostTextureOverride(String samplerName, @Nullable Integer fallbackTexture) {
        if (this.activePostTextureStage == null) {
            return fallbackTexture != null ? fallbackTexture : 0;
        }

        return this.getDeclaredStageTexture(this.resolvePostTextureStageName(this.activePostTextureStage), samplerName, fallbackTexture);
    }

    private String resolvePostTextureStageName(String programName) {
        String lowerName = programName.toLowerCase(Locale.ROOT);
        if (lowerName.startsWith("prepare")) {
            return "prepare";
        }
        if (lowerName.startsWith("deferred")) {
            return "deferred";
        }
        if (lowerName.startsWith("composite") || lowerName.startsWith("final")) {
            return "composite";
        }
        return lowerName;
    }

    private int getDeclaredStageTexture(String stageName, String samplerName, @Nullable Integer fallbackTexture) {
        ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();
        String relativePath = properties.getStageTexturePath(stageName, samplerName);

        if (relativePath == null && "composite".equals(stageName)) {
            relativePath = properties.getStageTexturePath("final", samplerName);
        }

        if (relativePath == null) {
            return fallbackTexture != null ? fallbackTexture : 0;
        }

        final String resolvedPath = relativePath;
        Integer textureId = this.packTextureCache.computeIfAbsent(stageName + ":" + samplerName, key -> this.loadPackTexture(resolvedPath));
        return textureId != null && textureId > 0 ? textureId : fallbackTexture != null ? fallbackTexture : 0;
    }

    private int loadPackTexture(String relativePath) {
        ActiniumShaderPackResources resources = ActiniumShaderPackManager.getActivePackResources();
        if (resources == null) {
            return 0;
        }

        try (InputStream stream = resources.openResource(relativePath)) {
            if (stream == null) {
                this.debugLog("Shader pack texture '{}' is missing", relativePath);
                return 0;
            }

            return uploadPackTexture(TextureUtil.readBufferedImage(stream));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader pack texture " + relativePath, e);
        }
    }

    private static int uploadPackTexture(java.awt.image.BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        image.getRGB(0, 0, width, height, pixels, 0, width);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();

        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private void updateCenterDepthSmooth() {
        if (this.postTargets == null) {
            return;
        }

        int depthTexture = this.postTargets.getDepthTexture(0);
        if (depthTexture <= 0) {
            return;
        }

        float currentDepth = this.readDepthTextureCenter(depthTexture);
        if (this.centerDepthSmooth == 0.0f) {
            this.centerDepthSmooth = currentDepth;
            return;
        }

        float blend = 0.15f;
        this.centerDepthSmooth += (currentDepth - this.centerDepthSmooth) * blend;
    }

    private float readDepthTextureCenter(int depthTexture) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int framebuffer = GL30.glGenFramebuffers();
        FloatBuffer pixel = BufferUtils.createFloatBuffer(1);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
            GL11.glReadBuffer(GL11.GL_NONE);
            GL11.glReadPixels(Math.max(0, this.width / 2), Math.max(0, this.height / 2), 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, pixel);
            return pixel.get(0);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    private void debugLogFramebufferCenter(String label, int framebufferId, int readBufferAttachment) {
        if (!this.shouldEmitVerboseDebugFrame() || framebufferId <= 0) {
            return;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
            GL11.glReadBuffer(readBufferAttachment);
            GL11.glReadPixels(Math.max(0, this.width / 2), Math.max(0, this.height / 2), 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
            int r = pixel.get(0) & 0xFF;
            int g = pixel.get(1) & 0xFF;
            int b = pixel.get(2) & 0xFF;
            int a = pixel.get(3) & 0xFF;
            this.debugLog("Post debug '{}' center pixel rgba=[{}, {}, {}, {}]", label, r, g, b, a);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
        }
    }

    private void debugLogTextureSamples(String label, int textureId) {
        if (!this.shouldEmitVerboseDebugFrame() || textureId <= 0) {
            return;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int framebuffer = GL30.glGenFramebuffers();

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            this.debugReadSamplePoints(label, this.width, this.height);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    private void debugLogFramebufferSamples(String label, int framebufferId, int readBufferAttachment) {
        if (!this.shouldEmitVerboseDebugFrame() || framebufferId <= 0) {
            return;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
            GL11.glReadBuffer(readBufferAttachment);
            this.debugReadSamplePoints(label, this.width, this.height);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
        }
    }

    private void debugReadSamplePoints(String label, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        String[] names = {"tl", "tr", "bl", "br", "center"};
        int[][] points = {
                {Math.max(0, width / 4), Math.max(0, (height * 3) / 4)},
                {Math.max(0, (width * 3) / 4), Math.max(0, (height * 3) / 4)},
                {Math.max(0, width / 4), Math.max(0, height / 4)},
                {Math.max(0, (width * 3) / 4), Math.max(0, height / 4)},
                {Math.max(0, width / 2), Math.max(0, height / 2)}
        };
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < points.length; i++) {
            ByteBuffer pixel = BufferUtils.createByteBuffer(4);
            GL11.glReadPixels(points[i][0], points[i][1], 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(names[i])
                    .append("=[")
                    .append(pixel.get(0) & 0xFF).append('/')
                    .append(pixel.get(1) & 0xFF).append('/')
                    .append(pixel.get(2) & 0xFF).append('/')
                    .append(pixel.get(3) & 0xFF).append(']');
        }

        this.debugLog("Post debug '{}' samples {}", label, builder);
    }

    private void debugLogRenderState(String label) {
        if (!this.shouldEmitVerboseDebugFrame()) {
            return;
        }

        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        invokeGlGetInteger(GL11.GL_VIEWPORT, viewport);
        IntBuffer scissor = BufferUtils.createIntBuffer(16);
        invokeGlGetInteger(GL11.GL_SCISSOR_BOX, scissor);

        this.debugLog(
                "Render state '{}' fb={}, draw={}, read={}, program={}, vao={}, viewport=[{}, {}, {}, {}], scissorEnabled={}, scissor=[{}, {}, {}, {}], blend={}, depth={}, alpha={}, cull={}",
                label,
                GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
                GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                GL11.glGetInteger(GL11.GL_READ_BUFFER),
                GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3),
                GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                scissor.get(0), scissor.get(1), scissor.get(2), scissor.get(3),
                GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                GL11.glIsEnabled(GL11.GL_CULL_FACE)
        );
    }

    private boolean shouldEmitVerboseDebugFrame() {
        return ActiniumShaderPackManager.isDebugEnabled() && this.frameCounter % 60 == 0;
    }

    private void unbindWorldStageTextures() {
        unbindTexture(WORLD_GAUX4_UNIT);
        this.unbindTerrainShadowTextures();
    }

    private List<ActiniumPostProgram> getPreScenePrograms() {
        if (!this.shouldUseExternalScenePrograms()) {
            return Collections.emptyList();
        }

        if (this.preSceneProgramsResolved) {
            return this.preScenePrograms;
        }

        this.preSceneProgramsResolved = true;
        this.preScenePrograms = this.resolvePostPrograms(PRE_SCENE_POST_PROGRAMS, "pre-scene");
        return this.preScenePrograms;
    }

    private List<ActiniumPostProgram> getPreparePrograms() {
        List<ActiniumPostProgram> programs = this.getPreScenePrograms();

        if (programs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiniumPostProgram> filtered = new ArrayList<>();
        for (ActiniumPostProgram program : programs) {
            if (program.name().startsWith("prepare")) {
                filtered.add(program);
            }
        }
        return filtered;
    }

    private List<ActiniumPostProgram> getDeferredPrograms() {
        List<ActiniumPostProgram> programs = this.getPreScenePrograms();

        if (programs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiniumPostProgram> filtered = new ArrayList<>();
        for (ActiniumPostProgram program : programs) {
            if (program.name().startsWith("deferred")) {
                filtered.add(program);
            }
        }
        return filtered;
    }

    private List<ActiniumPostProgram> getScenePrograms() {
        if (!this.shouldUseExternalScenePrograms()) {
            return Collections.emptyList();
        }

        if (this.sceneProgramsResolved) {
            return this.scenePrograms;
        }

        this.sceneProgramsResolved = true;
        this.scenePrograms = this.resolvePostPrograms(SCENE_POST_PROGRAMS, "scene");
        return this.scenePrograms;
    }

    private List<ActiniumPostProgram> resolvePostPrograms(String[] programNames, String logCategory) {
        List<ActiniumPostProgram> resolved = new ArrayList<>();
        ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();

        for (String programName : programNames) {
            String fragmentSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT);

            if (fragmentSource == null) {
                continue;
            }

            String vertexSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.VERTEX);
            ProgramMetadata metadata = ProgramMetadata.parse(fragmentSource);
            int[] drawBuffers = metadata.drawBuffers();
            this.debugLog(
                    "Resolved post program '{}' metadata: drawBuffers={}, mipmappedBuffers={}",
                    programName,
                    Arrays.toString(drawBuffers),
                    Arrays.toString(metadata.mipmappedBuffers())
            );

            try {
                resolved.add(new ActiniumPostProgram(
                        programName,
                        this.createProgram(programName, vertexSource, fragmentSource, drawBuffers),
                        drawBuffers,
                        metadata.mipmappedBuffers(),
                        properties.getExplicitFlips(programName)
                ));
            } catch (RuntimeException e) {
                ActiniumShaders.logger().warn("Failed to compile external shader pack {} post program '{}'", logCategory, programName, e);
            }
        }

        if (!resolved.isEmpty() && !this.loggedPostProgramUse) {
            this.loggedPostProgramUse = true;
            ActiniumShaders.logger().info("Using external shader pack scene post programs: {}", resolved.stream().map(ActiniumPostProgram::name).reduce((a, b) -> a + ", " + b).orElse("unknown"));
        }
        return resolved;
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
        ProgramMetadata metadata = ProgramMetadata.parse(fragmentSource);
        this.finalProgramMipmappedBuffers = metadata.mipmappedBuffers();
        this.debugLog(
                "Resolved post program '{}' metadata: drawBuffers={}, mipmappedBuffers={}",
                "final",
                "[0]",
                Arrays.toString(this.finalProgramMipmappedBuffers)
        );

        try {
            this.finalProgram = this.createProgram("final", vertexSource, fragmentSource, new int[]{0});
            this.finalProgramExplicitFlips = ActiniumShaderPackManager.getActiveShaderProperties().getExplicitFlips("final");
            if (!this.loggedFinalProgramUse) {
                this.loggedFinalProgramUse = true;
                ActiniumShaders.logger().info("Using external shader pack final pass");
            }
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to compile external shader pack program 'final'", e);
            this.finalProgram = null;
            this.finalProgramExplicitFlips = Collections.emptyMap();
        }

        return this.finalProgram;
    }

    private GlProgram<ActiniumPostShaderInterface> getBlitProgram() {
        if (this.blitProgram != null) {
            return this.blitProgram;
        }

        String vertexSource = String.join("\n",
                "#version 330 core",
                "layout(location = 0) in vec3 a_Position;",
                "layout(location = 2) in vec2 a_TexCoord;",
                "out vec2 texcoord;",
                "void main() {",
                "    texcoord = a_TexCoord;",
                "    gl_Position = vec4(a_Position.xy * 2.0 - 1.0, a_Position.z, 1.0);",
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

        shaderSource = ShaderParser.parseShader(shaderSource, this::resolveShaderSource, ShaderConstants.EMPTY);

        if (this.isLegacyPackProgram(shaderSource)) {
            this.debugLog("Applying legacy fullscreen shader adapter to '{}' {}", programName, type);
            shaderSource = ActiniumLegacyFullscreenShaderAdapter.translate(type, shaderSource);
        }

        shaderSource = this.applyPostShaderCompatibilityFixes(type, shaderSource);

        return new GlShader(type, "actinium:external/" + programName + "." + type.fileExtension, shaderSource);
    }

    private String applyPostShaderCompatibilityFixes(ShaderType type, String shaderSource) {
        if (type == ShaderType.FRAGMENT && shaderSource.contains("softLod")) {
            shaderSource = CONDITIONAL_SOFT_LOD_UNIFORM_PATTERN.matcher(shaderSource)
                    .replaceAll("uniform float softLod;\n");
        }

        if (type == ShaderType.FRAGMENT
                && shaderSource.contains("softLod")
                && !SOFT_LOD_UNIFORM_PATTERN.matcher(shaderSource).find()) {
            Matcher versionMatcher = VERSION_LINE_PATTERN.matcher(shaderSource);
            String injectedUniform = "\n#ifndef ACTINIUM_INJECT_SOFTLOD_UNIFORM\n"
                    + "#define ACTINIUM_INJECT_SOFTLOD_UNIFORM\n"
                    + "uniform float softLod;\n"
                    + "#endif\n";

            if (versionMatcher.find()) {
                int insertIndex = versionMatcher.end();
                return shaderSource.substring(0, insertIndex)
                        + injectedUniform
                        + shaderSource.substring(insertIndex);
            }

            return "#ifndef ACTINIUM_INJECT_SOFTLOD_UNIFORM\n"
                    + "#define ACTINIUM_INJECT_SOFTLOD_UNIFORM\n"
                    + "uniform float softLod;\n"
                    + "#endif\n"
                    + shaderSource;
        }

        return shaderSource;
    }

    private boolean isLegacyPackProgram(String source) {
        return LEGACY_PACK_MARKERS.matcher(source).find();
    }

    private boolean shouldUseExternalScenePrograms() {
        return ENABLE_EXTERNAL_SCENE_PIPELINE
                && ActiniumShaderPackManager.areShadersEnabled()
                && ActiniumShaderPackManager.getActivePackResources() != null;
    }

    private boolean shouldUseExternalWorldPrograms() {
        return false;
    }

    private boolean shouldUseExternalFinalProgram() {
        return ENABLE_EXTERNAL_FINAL_PIPELINE && this.shouldUseExternalScenePrograms();
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
            this.fullscreenQuadBuffer = GL15.glGenBuffers();
            FloatBuffer fullscreenQuad = BufferUtils.createFloatBuffer(20);
            fullscreenQuad.put(new float[]{
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                    1.0f, 1.0f, 0.0f, 1.0f, 1.0f
            });
            fullscreenQuad.flip();

            GL30.glBindVertexArray(this.fullscreenVertexArray.handle());
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.fullscreenQuadBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fullscreenQuad, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0L);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
        }

        if (this.postTargets == null) {
            this.postTargets = new ActiniumPostTargets(this.createConfiguredPostTargetFormats(), this.createConfiguredPostTargetSettings());
        }

        if (this.preSceneTargets == null) {
            this.preSceneTargets = new ActiniumPostTargets(this.createConfiguredPostTargetFormats(), this.createConfiguredPostTargetSettings());
        }

        if (this.shadowTargets == null) {
            this.shadowTargets = new ActiniumShadowTargets();
        }

        if (this.worldTargets == null) {
            this.worldTargets = new ActiniumWorldTargets(
                    ActiniumPostTargets.ColorFormat.RGBA16F,
                    ActiniumPostTargets.ColorFormat.RGBA16F
            );
        }

        if (this.whiteTexture == null) {
            this.whiteTexture = createSolidTexture(255, 255, 255, 255);
        }

        if (this.noiseTexture == null) {
            this.noiseTexture = createNoiseTexture();
        }

        this.ensureTerrainInputTextures();
    }

    private static ActiniumPostTargets.ColorFormat[] createDefaultPostTargetFormats() {
        return new ActiniumPostTargets.ColorFormat[]{
                ActiniumPostTargets.ColorFormat.R8,
                ActiniumPostTargets.ColorFormat.RGBA16F,
                ActiniumPostTargets.ColorFormat.R8,
                ActiniumPostTargets.ColorFormat.RGBA16F,
                ActiniumPostTargets.ColorFormat.RGBA16F,
                ActiniumPostTargets.ColorFormat.R8,
                ActiniumPostTargets.ColorFormat.R16F,
                ActiniumPostTargets.ColorFormat.RGBA16F
        };
    }

    private ActiniumPostTargets.ColorFormat[] createConfiguredPostTargetFormats() {
        ActiniumPostTargets.ColorFormat[] formats = createDefaultPostTargetFormats();

        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return formats;
        }

        for (String programName : POST_PROGRAMS) {
            String fragmentSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT);

            if (fragmentSource != null) {
                this.applyDeclaredPostTargetFormats(this.resolvePostDirectiveSource(fragmentSource), formats);
            }
        }

        return formats;
    }

    private static ActiniumPostTargets.TargetSettings[] createDefaultPostTargetSettings() {
        return new ActiniumPostTargets.TargetSettings[]{
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 1.0f),
                new ActiniumPostTargets.TargetSettings(true, 1.0f, 1.0f, 1.0f, 1.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f)
        };
    }

    private ActiniumPostTargets.TargetSettings[] createConfiguredPostTargetSettings() {
        ActiniumPostTargets.TargetSettings[] settings = createDefaultPostTargetSettings();

        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return settings;
        }

        for (String programName : POST_PROGRAMS) {
            String fragmentSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT);

            if (fragmentSource != null) {
                this.applyDeclaredPostTargetSettings(this.resolvePostDirectiveSource(fragmentSource), settings);
            }
        }

        return settings;
    }

    private String resolvePostDirectiveSource(String fragmentSource) {
        return ShaderParser.parseShader(fragmentSource, this::resolveShaderSource, ShaderConstants.EMPTY);
    }

    private void applyDeclaredPostTargetFormats(String shaderSource, ActiniumPostTargets.ColorFormat[] formats) {
        Matcher matcher = DECLARED_TARGET_FORMAT_PATTERN.matcher(shaderSource);

        while (matcher.find()) {
            int targetIndex = resolvePostTargetIndex(matcher.group(1));

            if (targetIndex < 0 || targetIndex >= formats.length) {
                continue;
            }

            ActiniumPostTargets.ColorFormat colorFormat = resolveDeclaredColorFormat(matcher.group(2));

            if (colorFormat == null) {
                continue;
            }

            formats[targetIndex] = colorFormat;
        }
    }

    private void applyDeclaredPostTargetSettings(String shaderSource, ActiniumPostTargets.TargetSettings[] settings) {
        Matcher clearMatcher = DECLARED_TARGET_CLEAR_PATTERN.matcher(shaderSource);

        while (clearMatcher.find()) {
            int targetIndex = resolvePostTargetIndex(clearMatcher.group(1));

            if (targetIndex < 0 || targetIndex >= settings.length) {
                continue;
            }

            ActiniumPostTargets.TargetSettings current = settings[targetIndex];
            settings[targetIndex] = new ActiniumPostTargets.TargetSettings(
                    Boolean.parseBoolean(clearMatcher.group(2)),
                    current.clearRed(),
                    current.clearGreen(),
                    current.clearBlue(),
                    current.clearAlpha()
            );
        }

        Matcher clearColorMatcher = DECLARED_TARGET_CLEAR_COLOR_PATTERN.matcher(shaderSource);

        while (clearColorMatcher.find()) {
            int targetIndex = resolvePostTargetIndex(clearColorMatcher.group(1));

            if (targetIndex < 0 || targetIndex >= settings.length) {
                continue;
            }

            float[] color = parseVec4(clearColorMatcher.group(2));

            if (color == null) {
                continue;
            }

            ActiniumPostTargets.TargetSettings current = settings[targetIndex];
            settings[targetIndex] = new ActiniumPostTargets.TargetSettings(
                    current.clear(),
                    color[0],
                    color[1],
                    color[2],
                    color[3]
            );
        }
    }

    private static @Nullable float[] parseVec4(String value) {
        String[] parts = value.split(",");

        if (parts.length != 4) {
            return null;
        }

        float[] result = new float[4];

        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim().replace("f", "").replace("F", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return result;
    }

    private static int resolvePostTargetIndex(String bufferName) {
        if (bufferName.startsWith("colortex")) {
            try {
                return Integer.parseInt(bufferName.substring("colortex".length()));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        for (int i = 0; i < LEGACY_RENDER_TARGET_NAMES.length; i++) {
            if (LEGACY_RENDER_TARGET_NAMES[i].equals(bufferName)) {
                return i;
            }
        }

        return -1;
    }

    private static @Nullable ActiniumPostTargets.ColorFormat resolveDeclaredColorFormat(String formatName) {
        return switch (formatName) {
            case "R8" -> ActiniumPostTargets.ColorFormat.R8;
            case "RG8" -> ActiniumPostTargets.ColorFormat.RG8;
            case "RGB8" -> ActiniumPostTargets.ColorFormat.RGB8;
            case "RGBA8" -> ActiniumPostTargets.ColorFormat.RGBA8;
            case "RGB10_A2" -> ActiniumPostTargets.ColorFormat.RGB10_A2;
            case "RG16F" -> ActiniumPostTargets.ColorFormat.RG16F;
            case "RGB16F" -> ActiniumPostTargets.ColorFormat.RGB16F;
            case "RGBA16F" -> ActiniumPostTargets.ColorFormat.RGBA16F;
            case "R16F" -> ActiniumPostTargets.ColorFormat.R16F;
            case "R32F" -> ActiniumPostTargets.ColorFormat.R32F;
            case "RG32F" -> ActiniumPostTargets.ColorFormat.RG32F;
            case "RGBA32F" -> ActiniumPostTargets.ColorFormat.RGBA32F;
            case "R11F_G11F_B10F" -> ActiniumPostTargets.ColorFormat.R11F_G11F_B10F;
            default -> null;
        };
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

        for (ActiniumPostProgram sceneProgram : this.preScenePrograms) {
            sceneProgram.program().delete();
        }

        this.preScenePrograms = Collections.emptyList();
        this.preSceneProgramsResolved = false;
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
        this.finalProgramExplicitFlips = Collections.emptyMap();
        this.frameCounter = 0;
        this.shadowVisibilityFrameCounter = 0;
        this.lastFrameNanos = 0L;
        this.frameTimeCounterSeconds = 0.0f;
        this.currentFrameDeltaSeconds = 0.0f;
        this.width = 0;
        this.height = 0;
        this.prepareProgramsExecutedThisFrame = false;
        this.deferredProgramsExecutedThisFrame = false;
        this.preTranslucentDepthCapturedThisFrame = false;
        this.worldCameraPosition.zero();
        this.previousWorldCameraPosition.zero();
        this.servedPreviousWorldCameraPosition.zero();
        this.previousGbufferModelViewMatrix.identity();
        this.previousGbufferProjectionMatrix.identity();
        this.servedPreviousGbufferModelViewMatrix.identity();
        this.servedPreviousGbufferProjectionMatrix.identity();
        this.previousUniformFrame = Integer.MIN_VALUE;
        this.previousUniformInitialized = false;

        if (this.fullscreenVertexArray != null) {
            this.fullscreenVertexArray.delete();
            this.fullscreenVertexArray = null;
        }

        if (this.fullscreenQuadBuffer != 0) {
            GL15.glDeleteBuffers(this.fullscreenQuadBuffer);
            this.fullscreenQuadBuffer = 0;
        }

        if (this.postTargets != null) {
            this.postTargets.delete();
            this.postTargets = null;
        }

        if (this.preSceneTargets != null) {
            this.preSceneTargets.delete();
            this.preSceneTargets = null;
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

        if (this.terrainGaux2Texture != null) {
            GL11.glDeleteTextures(this.terrainGaux2Texture);
            this.terrainGaux2Texture = null;
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

        for (Integer textureId : this.packTextureCache.values()) {
            if (textureId != null && textureId > 0) {
                GL11.glDeleteTextures(textureId);
            }
        }
        this.packTextureCache.clear();

        this.worldTargetsPrepared = false;
        ActiniumInternalShadowRenderingState.clear();
    }

    private void disableExternalProgramsUntilReload() {
        this.deleteWorldPrograms();

        for (ActiniumPostProgram sceneProgram : this.scenePrograms) {
            sceneProgram.program().delete();
        }

        for (ActiniumPostProgram sceneProgram : this.preScenePrograms) {
            sceneProgram.program().delete();
        }

        this.preScenePrograms = Collections.emptyList();
        this.preSceneProgramsResolved = true;
        this.scenePrograms = Collections.emptyList();
        this.sceneProgramsResolved = true;

        if (this.finalProgram != null) {
            this.finalProgram.delete();
            this.finalProgram = null;
        }

        this.finalProgramExplicitFlips = Collections.emptyMap();
        this.finalProgramResolved = true;
        this.postProgramAvailable = false;
        this.prepareProgramsExecutedThisFrame = false;
        this.deferredProgramsExecutedThisFrame = false;
        this.preTranslucentDepthCapturedThisFrame = false;
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
        int framebufferWidth = framebuffer != null ? Math.max(1, framebuffer.framebufferWidth) : Math.max(1, this.width);
        int framebufferHeight = framebuffer != null ? Math.max(1, framebuffer.framebufferHeight) : Math.max(1, this.height);
        this.restoreScreenRenderState(framebuffer, framebufferWidth, framebufferHeight);
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
        if (!this.shouldUseExternalWorldPrograms()) {
            return null;
        }

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

        if (this.terrainGaux2Texture == null) {
            this.terrainGaux2Texture = createColorTexture(this.width, this.height);
            this.debugLog("Allocated terrain gaux2 texture {} with size {}x{}", this.terrainGaux2Texture, this.width, this.height);
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
            float[] fogClearColor = ChunkShaderFogComponent.FOG_SERVICE.getFogColor();
            float clearRed = fogClearColor.length > 0 ? fogClearColor[0] : 0.0f;
            float clearGreen = fogClearColor.length > 1 ? fogClearColor[1] : clearRed;
            float clearBlue = fogClearColor.length > 2 ? fogClearColor[2] : clearGreen;
            this.worldTargets.beginFrame(framebuffer, clearRed, clearGreen, clearBlue, 1.0f);
            debugCheckGlErrors("pipeline.prepareWorldTargets.beginFrame");
            this.worldTargetsPrepared = true;
            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog("Prepared world-stage MRT targets for {}x{} with fog clear color [{}, {}, {}]", this.width, this.height, clearRed, clearGreen, clearBlue);
            }
        }
    }

    private int[] resolveWorldStageDrawBuffers(ActiniumWorldStage stage, String resolvedFragmentSource) {
        int[] drawBuffers = parseDrawBuffers(resolvedFragmentSource);

        if (this.shouldForceLegacySkyDrawBuffers(stage, resolvedFragmentSource, drawBuffers)) {
            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog("Forcing world-stage program '{}' draw buffers to [1, 7] from writebuffers.glsl compatibility rule", stage.programName());
            }
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
            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog(
                        "Skipping world-stage draw buffer compatibility check for '{}' because src/writebuffers.glsl is unavailable in the active pack",
                        stage.programName()
                );
            }
            return false;
        }
        boolean hasSkyBasicBranch = writeBuffersSource.contains("MC_VERSION < 11604 && defined GBUFFER_SKYBASIC");
        boolean hasSkyBasicDrawBuffers = hasSkyBasicBranch && writeBuffersSource.contains("/* DRAWBUFFERS:17 */");

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog(
                    "Resolved world-stage draw buffers for '{}' as {} before compatibility fix; skybasicBranch={}, skybasicDrawBuffers17={}",
                    stage.programName(),
                    Arrays.toString(parsedDrawBuffers),
                    hasSkyBasicBranch,
                    hasSkyBasicDrawBuffers
            );
        }

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

    private void presentTerrainPassResult(Framebuffer mainFramebuffer, int[] drawBuffers) {
        if (this.worldTargets == null || mainFramebuffer.framebufferTexture <= 0) {
            return;
        }

        for (int drawBuffer : drawBuffers) {
            if (drawBuffer == ActiniumPostTargets.TARGET_COLORTEX1 && this.worldTargets.hasSourceColorTexture()) {
                this.copyTexture(this.worldTargets.getSourceColorTexture(), mainFramebuffer.framebufferTexture);
                debugCheckGlErrors("terrain.presentMainFramebuffer");
                return;
            }
        }
    }

    private void debugLogTerrainPassState(String stage, TerrainRenderPass pass, Framebuffer framebuffer) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || this.worldTargets == null) {
            return;
        }

        this.debugLogTextureCenter("terrain." + pass.name() + "." + stage + ".worldColor", this.worldTargets.getSourceColorTexture());
        this.debugLogTextureCenter("terrain." + pass.name() + "." + stage + ".mainFramebuffer", framebuffer.framebufferTexture);
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

    private static void invokeGlGetInteger(int parameter, IntBuffer buffer) {
        if (GL_GET_INTEGER_BUFFER_METHOD == null) {
            throw new IllegalStateException("LWJGL GL11.glGetInteger(int, IntBuffer) is unavailable");
        }

        try {
            GL_GET_INTEGER_BUFFER_METHOD.invoke(null, parameter, buffer);
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
        return parseDrawBuffers(fragmentSource, false);
    }

    private static int[] parseDrawBuffers(String fragmentSource, boolean preferWidestMatch) {
        Matcher matcher = DRAW_BUFFERS_PATTERN.matcher(fragmentSource);
        String value = null;
        int maxLength = -1;

        while (matcher.find()) {
            String candidate = matcher.group(1);

            if (!preferWidestMatch) {
                value = candidate;
                continue;
            }

            if (candidate.length() >= maxLength) {
                value = candidate;
                maxLength = candidate.length();
            }
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
                "layout(location = 0) in vec3 a_Position;",
                "layout(location = 2) in vec2 a_TexCoord;",
                "out vec2 texcoord;",
                "void main() {",
                "    texcoord = a_TexCoord;",
                "    gl_Position = vec4(a_Position.xy * 2.0 - 1.0, a_Position.z, 1.0);",
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
        private final int[] textureBindings2d;
        private final int currentProgram;
        private final int vertexArrayBinding;
        private final int arrayBufferBinding;
        private final int elementArrayBufferBinding;
        private final int matrixMode;
        private final int shadeModel;
        private final int polygonModeFront;
        private final int polygonModeBack;
        private final float lineWidth;
        private final int depthFunc;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final int cullFaceMode;
        private final int frontFace;
        private final boolean blendEnabled;
        private final boolean cullEnabled;
        private final boolean depthEnabled;
        private final boolean alphaEnabled;
        private final boolean fogEnabled;
        private final boolean texture2dEnabled;
        private final boolean depthMask;
        private final boolean scissorEnabled;
        private final int[] scissorBox;
        private final boolean[] colorMask;
        private final float[] currentColor;
        private final int alphaFunc;
        private final float alphaRef;
        private final boolean attribStackPushed;
        private final boolean matrixStacksPushed;

        private ShadowPassGlState(int framebufferBinding,
                                  int drawBuffer,
                                  int readBuffer,
                                  int activeTexture,
                                  int textureBinding2d,
                                  int[] textureBindings2d,
                                  int currentProgram,
                                  int vertexArrayBinding,
                                  int arrayBufferBinding,
                                  int elementArrayBufferBinding,
                                  int matrixMode,
                                  int shadeModel,
                                  int polygonModeFront,
                                  int polygonModeBack,
                                  float lineWidth,
                                  int depthFunc,
                                  int blendSrcRgb,
                                  int blendDstRgb,
                                  int blendSrcAlpha,
                                  int blendDstAlpha,
                                  int cullFaceMode,
                                  int frontFace,
                                  boolean blendEnabled,
                                  boolean cullEnabled,
                                  boolean depthEnabled,
                                  boolean alphaEnabled,
                                  boolean fogEnabled,
                                  boolean texture2dEnabled,
                                  boolean depthMask,
                                  boolean scissorEnabled,
                                  int[] scissorBox,
                                  boolean[] colorMask,
                                  float[] currentColor,
                                  int alphaFunc,
                                  float alphaRef,
                                  boolean attribStackPushed,
                                  boolean matrixStacksPushed) {
            this.framebufferBinding = framebufferBinding;
            this.drawBuffer = drawBuffer;
            this.readBuffer = readBuffer;
            this.activeTexture = activeTexture;
            this.textureBinding2d = textureBinding2d;
            this.textureBindings2d = textureBindings2d;
            this.currentProgram = currentProgram;
            this.vertexArrayBinding = vertexArrayBinding;
            this.arrayBufferBinding = arrayBufferBinding;
            this.elementArrayBufferBinding = elementArrayBufferBinding;
            this.matrixMode = matrixMode;
            this.shadeModel = shadeModel;
            this.polygonModeFront = polygonModeFront;
            this.polygonModeBack = polygonModeBack;
            this.lineWidth = lineWidth;
            this.depthFunc = depthFunc;
            this.blendSrcRgb = blendSrcRgb;
            this.blendDstRgb = blendDstRgb;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.cullFaceMode = cullFaceMode;
            this.frontFace = frontFace;
            this.blendEnabled = blendEnabled;
            this.cullEnabled = cullEnabled;
            this.depthEnabled = depthEnabled;
            this.alphaEnabled = alphaEnabled;
            this.fogEnabled = fogEnabled;
            this.texture2dEnabled = texture2dEnabled;
            this.depthMask = depthMask;
            this.scissorEnabled = scissorEnabled;
            this.scissorBox = scissorBox;
            this.colorMask = colorMask;
            this.currentColor = currentColor;
            this.alphaFunc = alphaFunc;
            this.alphaRef = alphaRef;
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

            IntBuffer colorMaskBuffer = BufferUtils.createIntBuffer(16);
            invokeGlGetInteger(GL11.GL_COLOR_WRITEMASK, colorMaskBuffer);
            FloatBuffer alphaRefBuffer = BufferUtils.createFloatBuffer(16);
            invokeGlGetFloat(GL11.GL_ALPHA_TEST_REF, alphaRefBuffer);
            FloatBuffer lineWidthBuffer = BufferUtils.createFloatBuffer(16);
            invokeGlGetFloat(GL11.GL_LINE_WIDTH, lineWidthBuffer);
            FloatBuffer currentColorBuffer = BufferUtils.createFloatBuffer(16);
            invokeGlGetFloat(GL11.GL_CURRENT_COLOR, currentColorBuffer);
            IntBuffer polygonModeBuffer = BufferUtils.createIntBuffer(16);
            invokeGlGetInteger(GL11.GL_POLYGON_MODE, polygonModeBuffer);
            IntBuffer scissorBoxBuffer = BufferUtils.createIntBuffer(16);
            invokeGlGetInteger(GL11.GL_SCISSOR_BOX, scissorBoxBuffer);
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] textureBindings2d = new int[TRACKED_TEXTURE_UNITS];

            for (int unit = 0; unit < TRACKED_TEXTURE_UNITS; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                textureBindings2d[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }

            GL13.glActiveTexture(activeTexture);

            return new ShadowPassGlState(
                    GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                    GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    activeTexture,
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),
                    textureBindings2d,
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                    GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING),
                    GL11.glGetInteger(GL11.GL_MATRIX_MODE),
                    GL11.glGetInteger(GL11.GL_SHADE_MODEL),
                    polygonModeBuffer.get(0),
                    polygonModeBuffer.get(1),
                    lineWidthBuffer.get(0),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL11.GL_CULL_FACE_MODE),
                    GL11.glGetInteger(GL11.GL_FRONT_FACE),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                    GL11.glIsEnabled(GL11.GL_FOG),
                    GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                    new int[]{
                            scissorBoxBuffer.get(0),
                            scissorBoxBuffer.get(1),
                            scissorBoxBuffer.get(2),
                            scissorBoxBuffer.get(3)
                    },
                    new boolean[]{
                            colorMaskBuffer.get(0) != 0,
                            colorMaskBuffer.get(1) != 0,
                            colorMaskBuffer.get(2) != 0,
                            colorMaskBuffer.get(3) != 0
                    },
                    new float[]{
                            currentColorBuffer.get(0),
                            currentColorBuffer.get(1),
                            currentColorBuffer.get(2),
                            currentColorBuffer.get(3)
                    },
                    GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),
                    alphaRefBuffer.get(0),
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

            for (int unit = 0; unit < this.textureBindings2d.length; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureBindings2d[unit]);
            }

            GL13.glActiveTexture(this.activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureBinding2d);
            GL20.glUseProgram(this.currentProgram);
            GL30.glBindVertexArray(this.vertexArrayBinding);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBufferBinding);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.elementArrayBufferBinding);
            GL11.glMatrixMode(this.matrixMode);
            GL11.glShadeModel(this.shadeModel);
            GL11.glPolygonMode(GL11.GL_FRONT, this.polygonModeFront);
            GL11.glPolygonMode(GL11.GL_BACK, this.polygonModeBack);
            GL11.glLineWidth(this.lineWidth);
            GL11.glDepthFunc(this.depthFunc);
            GL14.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
            GL11.glCullFace(this.cullFaceMode);
            GL11.glFrontFace(this.frontFace);

            setEnabled(GL11.GL_BLEND, this.blendEnabled);
            setEnabled(GL11.GL_CULL_FACE, this.cullEnabled);
            setEnabled(GL11.GL_DEPTH_TEST, this.depthEnabled);
            setEnabled(GL11.GL_ALPHA_TEST, this.alphaEnabled);
            setEnabled(GL11.GL_FOG, this.fogEnabled);
            setEnabled(GL11.GL_TEXTURE_2D, this.texture2dEnabled);
            setEnabled(GL11.GL_SCISSOR_TEST, this.scissorEnabled);
            GL11.glDepthMask(this.depthMask);
            GL11.glScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
            GL11.glColorMask(this.colorMask[0], this.colorMask[1], this.colorMask[2], this.colorMask[3]);
            GL11.glColor4f(this.currentColor[0], this.currentColor[1], this.currentColor[2], this.currentColor[3]);
            GL11.glAlphaFunc(this.alphaFunc, this.alphaRef);

            if (this.framebufferBinding != 0) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferBinding);
                GL11.glDrawBuffer(this.drawBuffer);
                GL11.glReadBuffer(this.readBuffer);
            } else if (mainFramebuffer != null) {
                mainFramebuffer.bindFramebuffer(true);
                GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
                GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            } else {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                GL11.glDrawBuffer(this.drawBuffer);
                GL11.glReadBuffer(this.readBuffer);
            }

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
        private final int[] textureBindings2d;
        private final int currentProgram;
        private final boolean blendEnabled;
        private final boolean cullEnabled;
        private final boolean depthEnabled;
        private final boolean alphaEnabled;
        private final boolean fogEnabled;
        private final boolean texture2dEnabled;
        private final boolean depthMask;
        private final int[] viewport;

        private WorldStageGlState(int framebufferBinding,
                                  int drawBuffer,
                                  int readBuffer,
                                  int activeTexture,
                                  int[] textureBindings2d,
                                  int currentProgram,
                                  boolean blendEnabled,
                                  boolean cullEnabled,
                                  boolean depthEnabled,
                                  boolean alphaEnabled,
                                  boolean fogEnabled,
                                  boolean texture2dEnabled,
                                  boolean depthMask,
                                  int[] viewport) {
            this.framebufferBinding = framebufferBinding;
            this.drawBuffer = drawBuffer;
            this.readBuffer = readBuffer;
            this.activeTexture = activeTexture;
            this.textureBindings2d = textureBindings2d;
            this.currentProgram = currentProgram;
            this.blendEnabled = blendEnabled;
            this.cullEnabled = cullEnabled;
            this.depthEnabled = depthEnabled;
            this.alphaEnabled = alphaEnabled;
            this.fogEnabled = fogEnabled;
            this.texture2dEnabled = texture2dEnabled;
            this.depthMask = depthMask;
            this.viewport = viewport;
        }

        public static WorldStageGlState capture() {
            IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
            invokeGlGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] textureBindings2d = new int[TRACKED_TEXTURE_UNITS];

            for (int unit = 0; unit < TRACKED_TEXTURE_UNITS; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                textureBindings2d[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }

            GL13.glActiveTexture(activeTexture);

            return new WorldStageGlState(
                    GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
                    GL11.glGetInteger(GL11.GL_READ_BUFFER),
                    activeTexture,
                    textureBindings2d,
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                    GL11.glIsEnabled(GL11.GL_FOG),
                    GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
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
            if (this.framebufferBinding != 0) {
                GL11.glDrawBuffer(this.drawBuffer);
                GL11.glReadBuffer(this.readBuffer);
            }
            GL20.glUseProgram(this.currentProgram);

            for (int unit = 0; unit < this.textureBindings2d.length; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureBindings2d[unit]);
            }

            GL13.glActiveTexture(this.activeTexture);
            setEnabled(GL11.GL_BLEND, this.blendEnabled);
            setEnabled(GL11.GL_CULL_FACE, this.cullEnabled);
            setEnabled(GL11.GL_DEPTH_TEST, this.depthEnabled);
            setEnabled(GL11.GL_ALPHA_TEST, this.alphaEnabled);
            setEnabled(GL11.GL_FOG, this.fogEnabled);
            setEnabled(GL11.GL_TEXTURE_2D, this.texture2dEnabled);
            GL11.glDepthMask(this.depthMask);
            GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }

    private static final class ActiniumPostProgram {
        private final String name;
        private final GlProgram<ActiniumPostShaderInterface> program;
        private final int[] drawBuffers;
        private final int[] mipmappedBuffers;
        private final Map<Integer, Boolean> explicitFlips;

        private ActiniumPostProgram(String name, GlProgram<ActiniumPostShaderInterface> program, int[] drawBuffers, int[] mipmappedBuffers, Map<Integer, Boolean> explicitFlips) {
            this.name = name;
            this.program = program;
            this.drawBuffers = drawBuffers;
            this.mipmappedBuffers = mipmappedBuffers;
            this.explicitFlips = explicitFlips;
        }

        public String name() {
            return this.name;
        }

        public GlProgram<ActiniumPostShaderInterface> program() {
            return this.program;
        }

        public int[] drawBuffers() {
            return this.drawBuffers;
        }

        public int[] mipmappedBuffers() {
            return this.mipmappedBuffers;
        }

        public Map<Integer, Boolean> explicitFlips() {
            return this.explicitFlips;
        }
    }

    private static final class ProgramMetadata {
        private final int[] drawBuffers;
        private final int[] mipmappedBuffers;

        private ProgramMetadata(int[] drawBuffers, int[] mipmappedBuffers) {
            this.drawBuffers = drawBuffers;
            this.mipmappedBuffers = mipmappedBuffers;
        }

        public static ProgramMetadata parse(String shaderSource) {
            java.util.Map<String, String> defines = new java.util.HashMap<>();
            java.util.ArrayList<ConditionalFrame> stack = new java.util.ArrayList<>();
            java.util.LinkedHashSet<Integer> mipmappedTargets = new java.util.LinkedHashSet<>();
            String drawBufferDirective = null;
            String renderTargetsDirective = null;
            int drawBufferLocation = -1;
            int renderTargetsLocation = -1;
            boolean active = true;
            int location = 0;

            for (String rawLine : shaderSource.split("\\R")) {
                String line = stripLineComment(rawLine);
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#ifdef ")) {
                    String name = trimmed.substring("#ifdef ".length()).trim();
                    boolean condition = defines.containsKey(name);
                    stack.add(new ConditionalFrame(active, condition, active && condition));
                    active = active && condition;
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#ifndef ")) {
                    String name = trimmed.substring("#ifndef ".length()).trim();
                    boolean condition = !defines.containsKey(name);
                    stack.add(new ConditionalFrame(active, condition, active && condition));
                    active = active && condition;
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#if ")) {
                    boolean condition = evaluateExpression(trimmed.substring("#if ".length()).trim(), defines);
                    stack.add(new ConditionalFrame(active, condition, active && condition));
                    active = active && condition;
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#elif ")) {
                    if (!stack.isEmpty()) {
                        ConditionalFrame frame = stack.get(stack.size() - 1);
                        boolean condition = !frame.branchTaken && evaluateExpression(trimmed.substring("#elif ".length()).trim(), defines);
                        frame.branchTaken |= condition;
                        frame.active = frame.parentActive && condition;
                        active = frame.active;
                    }
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#else")) {
                    if (!stack.isEmpty()) {
                        ConditionalFrame frame = stack.get(stack.size() - 1);
                        boolean condition = !frame.branchTaken;
                        frame.branchTaken = true;
                        frame.active = frame.parentActive && condition;
                        active = frame.active;
                    }
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#endif")) {
                    if (!stack.isEmpty()) {
                        ConditionalFrame frame = stack.remove(stack.size() - 1);
                        active = frame.parentActive;
                    }
                    location += rawLine.length() + 1;
                    continue;
                }

                if (!active) {
                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#define ")) {
                    int valueStart = trimmed.indexOf(' ', "#define ".length());
                    String name;
                    String value;

                    if (valueStart >= 0) {
                        name = trimmed.substring("#define ".length(), valueStart).trim();
                        value = trimmed.substring(valueStart + 1).trim();
                    } else {
                        name = trimmed.substring("#define ".length()).trim();
                        value = "1";
                    }

                    if (!name.isEmpty() && !name.contains("(")) {
                        defines.put(name, value.isEmpty() ? "1" : value);
                    }

                    location += rawLine.length() + 1;
                    continue;
                }

                if (trimmed.startsWith("#undef ")) {
                    defines.remove(trimmed.substring("#undef ".length()).trim());
                    location += rawLine.length() + 1;
                    continue;
                }

                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/\\*\\s*(DRAWBUFFERS|RENDERTARGETS)\\s*:\\s*([^*]*?)\\s*\\*/").matcher(line);
                while (matcher.find()) {
                    String type = matcher.group(1);
                    String directive = matcher.group(2).trim();
                    int directiveLocation = location + matcher.start();

                    if ("DRAWBUFFERS".equals(type) && directiveLocation >= drawBufferLocation) {
                        drawBufferDirective = directive;
                        drawBufferLocation = directiveLocation;
                    } else if ("RENDERTARGETS".equals(type) && directiveLocation >= renderTargetsLocation) {
                        renderTargetsDirective = directive;
                        renderTargetsLocation = directiveLocation;
                    }
                }

                parseMipmapDirective(trimmed, mipmappedTargets);
                location += rawLine.length() + 1;
            }

            String appliedDirective;
            if (drawBufferLocation > renderTargetsLocation) {
                appliedDirective = drawBufferDirective;
            } else if (renderTargetsLocation >= 0) {
                appliedDirective = renderTargetsDirective;
            } else {
                appliedDirective = drawBufferDirective;
            }

            int[] drawBuffers = parseDrawBufferDirective(appliedDirective);
            int[] mipmappedBuffers = mipmappedTargets.stream().mapToInt(Integer::intValue).toArray();
            return new ProgramMetadata(drawBuffers, mipmappedBuffers);
        }

        public int[] drawBuffers() {
            return this.drawBuffers;
        }

        public int[] mipmappedBuffers() {
            return this.mipmappedBuffers;
        }

        private static void parseMipmapDirective(String trimmedLine, java.util.Set<Integer> mipmappedTargets) {
            if (!trimmedLine.startsWith("const bool ")) {
                return;
            }

            int equalsIndex = trimmedLine.indexOf('=');
            if (equalsIndex < 0) {
                return;
            }

            String name = trimmedLine.substring("const bool ".length(), equalsIndex).trim();
            String value = trimDirectiveValue(trimmedLine.substring(equalsIndex + 1));

            if (!name.endsWith("MipmapEnabled")) {
                return;
            }

            String bufferName = name.substring(0, name.length() - "MipmapEnabled".length());
            int targetIndex = resolveTargetIndex(bufferName);

            if (targetIndex < 0) {
                return;
            }

            if (parseBoolean(value)) {
                mipmappedTargets.add(targetIndex);
            } else {
                mipmappedTargets.remove(targetIndex);
            }
        }

        private static int resolveTargetIndex(String bufferName) {
            if (bufferName.startsWith("colortex")) {
                try {
                    return Integer.parseInt(bufferName.substring("colortex".length()));
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }

            for (int i = 0; i < LEGACY_RENDER_TARGET_NAMES.length; i++) {
                if (LEGACY_RENDER_TARGET_NAMES[i].equals(bufferName)) {
                    return i;
                }
            }

            return -1;
        }

        private static int[] parseDrawBufferDirective(@Nullable String directive) {
            if (directive == null || directive.isEmpty()) {
                return new int[]{ActiniumPostTargets.TARGET_COLORTEX1};
            }

            if (directive.indexOf(',') >= 0) {
                return Arrays.stream(directive.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .mapToInt(Integer::parseInt)
                        .toArray();
            }

            int[] drawBuffers = new int[directive.length()];
            for (int i = 0; i < directive.length(); i++) {
                drawBuffers[i] = Character.digit(directive.charAt(i), 10);
            }
            return drawBuffers;
        }

        private static boolean parseBoolean(String value) {
            return switch (value.toLowerCase()) {
                case "true", "on", "yes", "1" -> true;
                default -> false;
            };
        }

        private static String trimDirectiveValue(String value) {
            String trimmed = value.trim();
            if (trimmed.endsWith(";")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }
            return trimOuterParentheses(trimmed);
        }

        private static String stripLineComment(String line) {
            int commentStart = line.indexOf("//");
            return commentStart >= 0 ? line.substring(0, commentStart) : line;
        }

        private static boolean evaluateExpression(String expression, java.util.Map<String, String> defines) {
            String trimmed = trimOuterParentheses(expression.trim());

            int operatorIndex = findTopLevelOperator(trimmed, "||");
            if (operatorIndex >= 0) {
                return evaluateExpression(trimmed.substring(0, operatorIndex), defines)
                        || evaluateExpression(trimmed.substring(operatorIndex + 2), defines);
            }

            operatorIndex = findTopLevelOperator(trimmed, "&&");
            if (operatorIndex >= 0) {
                return evaluateExpression(trimmed.substring(0, operatorIndex), defines)
                        && evaluateExpression(trimmed.substring(operatorIndex + 2), defines);
            }

            if (trimmed.startsWith("!")) {
                return !evaluateExpression(trimmed.substring(1), defines);
            }

            if (trimmed.startsWith("defined(") && trimmed.endsWith(")")) {
                return defines.containsKey(trimmed.substring("defined(".length(), trimmed.length() - 1).trim());
            }

            for (String operator : new String[]{"==", "!=", ">=", "<=", ">", "<"}) {
                operatorIndex = findTopLevelOperator(trimmed, operator);
                if (operatorIndex >= 0) {
                    double left = resolveNumeric(trimmed.substring(0, operatorIndex), defines);
                    double right = resolveNumeric(trimmed.substring(operatorIndex + operator.length()), defines);
                    return switch (operator) {
                        case "==" -> left == right;
                        case "!=" -> left != right;
                        case ">=" -> left >= right;
                        case "<=" -> left <= right;
                        case ">" -> left > right;
                        case "<" -> left < right;
                        default -> false;
                    };
                }
            }

            return resolveTruthy(trimmed, defines);
        }

        private static int findTopLevelOperator(String expression, String operator) {
            int depth = 0;

            for (int i = 0; i <= expression.length() - operator.length(); i++) {
                char c = expression.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }

                if (depth == 0 && expression.startsWith(operator, i)) {
                    return i;
                }
            }

            return -1;
        }

        private static boolean resolveTruthy(String token, java.util.Map<String, String> defines) {
            String resolved = resolveToken(token.trim(), defines);

            if (resolved.isEmpty()) {
                return false;
            }

            if ("true".equalsIgnoreCase(resolved)) {
                return true;
            }

            if ("false".equalsIgnoreCase(resolved)) {
                return false;
            }

            try {
                return Float.parseFloat(resolved) != 0.0f;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        private static double resolveNumeric(String token, java.util.Map<String, String> defines) {
            String resolved = resolveToken(token.trim(), defines);

            if ("true".equalsIgnoreCase(resolved)) {
                return 1.0;
            }

            if ("false".equalsIgnoreCase(resolved)) {
                return 0.0;
            }

            try {
                return Double.parseDouble(resolved);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        private static String resolveToken(String token, java.util.Map<String, String> defines) {
            String trimmed = trimOuterParentheses(token.trim());
            String resolved = defines.get(trimmed);
            return resolved != null ? resolved.trim() : trimmed;
        }

        private static String trimOuterParentheses(String value) {
            String trimmed = value.trim();

            while (trimmed.startsWith("(") && trimmed.endsWith(")") && hasBalancedOuterParentheses(trimmed)) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }

            return trimmed;
        }

        private static boolean hasBalancedOuterParentheses(String value) {
            int depth = 0;

            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0 && i < value.length() - 1) {
                        return false;
                    }
                }
            }

            return depth == 0;
        }
    }

    private static final class ConditionalFrame {
        private final boolean parentActive;
        private boolean branchTaken;
        private boolean active;

        private ConditionalFrame(boolean parentActive, boolean branchTaken, boolean active) {
            this.parentActive = parentActive;
            this.branchTaken = branchTaken;
            this.active = active;
        }
    }
}
