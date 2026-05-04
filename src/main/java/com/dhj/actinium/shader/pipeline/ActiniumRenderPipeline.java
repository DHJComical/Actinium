package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import com.dhj.actinium.shader.pack.ActiniumShaderProperties;
import com.dhj.actinium.shader.pack.ActiniumShaderPackResources;
import com.dhj.actinium.shadows.ActiniumInternalShadowRenderingState;
import com.dhj.actinium.shadows.ActiniumShadowRenderingState;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
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
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.taumc.celeritas.lwjgl.GL42;
import org.taumc.celeritas.mixin.core.terrain.EntityRendererAccessor;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

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
    public static final int TERRAIN_GAUX3_UNIT = 6;
    public static final int TERRAIN_DEPTHTEX0_UNIT = 8;
    public static final int TERRAIN_DEPTHTEX1_UNIT = 9;
    public static final int TERRAIN_NOISETEX_UNIT = 10;
    public static final int TERRAIN_SHADOW_TEX0_UNIT = 11;
    public static final int TERRAIN_SHADOW_TEX1_UNIT = 12;
    public static final int TERRAIN_SHADOW_COLOR0_UNIT = 13;
    public static final int POST_SHADOW_TEX0_UNIT = 4;
    public static final int POST_SHADOW_TEX1_UNIT = 5;
    public static final int POST_DEPTHTEX0_UNIT = 6;
    public static final int POST_GAUX1_UNIT = 7;
    public static final int POST_GAUX2_UNIT = 8;
    public static final int POST_GAUX3_UNIT = 9;
    public static final int POST_GAUX4_UNIT = 10;
    public static final int POST_DEPTHTEX1_UNIT = 11;
    public static final int POST_DEPTHTEX2_UNIT = 12;
    public static final int POST_SHADOW_COLOR0_UNIT = 13;
    public static final int POST_SHADOW_COLOR1_UNIT = 14;
    public static final int POST_NOISETEX_UNIT = 15;
    public static final int POST_COLORTEX8_UNIT = 16;
    public static final int POST_COLORTEX9_UNIT = 17;
    public static final int POST_COLORTEX10_UNIT = 18;
    public static final int POST_COLORTEX11_UNIT = 19;
    public static final int POST_COLORTEX12_UNIT = 20;
    public static final int POST_COLORTEX13_UNIT = 21;
    public static final int POST_COLORTEX14_UNIT = 22;
    public static final int POST_COLORTEX15_UNIT = 23;
    private static final int GL_STATE_MANAGER_TEXTURE_UNITS = 8;
    private static final float TAA_JITTER_SCALE = 0.35f;
    private static final float SKY_HORIZON_TOP = 16.0f;
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
            "gbuffers_weather",
            "gbuffers_textured_lit",
            "gbuffers_textured",
            "gbuffers_basic"
    };
    private static final String[] PARTICLE_PROGRAMS = {
            "gbuffers_particles",
            "gbuffers_textured_lit",
            "gbuffers_textured",
            "gbuffers_basic"
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
    private static final int TRACKED_TEXTURE_UNITS = 24;
    private static final boolean ENABLE_PRE_SCENE_PIPELINES = true;
    private static final boolean ENABLE_PRE_SCENE_SHADER_EXECUTION = false;
    private static final boolean ENABLE_PREPARE_SHADER_EXECUTION = true;
    private static final boolean ENABLE_DEFERRED_SHADER_EXECUTION = true;
    private static final boolean ENABLE_EXTERNAL_SCENE_PIPELINE = true;
    private static final boolean ENABLE_EXTERNAL_FINAL_PIPELINE = true;
    private static final boolean ENABLE_EXTERNAL_TERRAIN_REDIRECT = false;
    private static final boolean ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT = true;
    private static final boolean ENABLE_EXTERNAL_SKY_BASIC_STAGE = true;
    private static final boolean ENABLE_EXTERNAL_SKY_TEXTURED_STAGE = true;
    private static final boolean ENABLE_EXTERNAL_CLOUDS_STAGE = false;
    private static final boolean ENABLE_EXTERNAL_ENTITIES_STAGE = false;
    private static final float DEFERRED_SKY_DEPTH_THRESHOLD = 0.9999f;
    private static final float FOCUS_DEPTH_SKY_THRESHOLD = 0.999999f;
    private static final int FOCUS_DEPTH_SAMPLE_RADIUS = 1;
    private static final int TERRAIN_INPUT_GAUX1 = 1;
    private static final int TERRAIN_INPUT_GAUX2 = 1 << 1;
    private static final int TERRAIN_INPUT_DEPTHTEX0 = 1 << 2;
    private static final int TERRAIN_INPUT_DEPTHTEX1 = 1 << 3;

    private int observedReloadVersion = -1;
    @Getter
    private ActiniumRenderStage currentStage = ActiniumRenderStage.NONE;
    @Getter
    private ActiniumPipelinePhase currentPhase = ActiniumPipelinePhase.NONE;
    private ActiniumRenderStage postReturnStage = ActiniumRenderStage.NONE;
    private ActiniumPipelinePhase postReturnPhase = ActiniumPipelinePhase.NONE;
    private boolean shadowProgramAvailable;
    private boolean skyProgramAvailable;
    private boolean particleProgramAvailable;
    private boolean weatherProgramAvailable;
    private boolean postProgramAvailable;
    @Getter
    private boolean renderingShadowPass;
    private boolean loggedCapabilities;
    private boolean sceneProgramsResolved;
    private boolean preSceneProgramsResolved;
    private boolean finalProgramResolved;
    private boolean shadowEntityProgramResolved;
    private boolean loggedPostProgramUse;
    private boolean loggedFinalProgramUse;
    private boolean loggedPostImageBindingDisabled;
    @Getter
    private int frameCounter;
    private int shadowVisibilityFrameCounter;
    private long lastFrameNanos;
    @Getter
    private float frameTimeCounterSeconds;
    @Getter
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
    private final Vector3d shaderCameraPosition = new Vector3d();
    private final Vector3d previousShaderCameraPosition = new Vector3d();
    private final Vector3d shaderCameraPositionUnshifted = new Vector3d();
    private final Vector3d previousShaderCameraPositionUnshifted = new Vector3d();
    private final Vector3d shaderCameraShift = new Vector3d();
    private boolean shaderCameraPositionInitialized;
    private final Matrix4f scratchWorldStageModelViewMatrix = new Matrix4f();
    private final Matrix4f scratchWorldStageProjectionMatrix = new Matrix4f();
    private final Matrix4f scratchWorldStageProjectionInverseMatrix = new Matrix4f();
    private final Matrix4f scratchManagedSkyMatrix = new Matrix4f();
    private final Matrix4f scratchShadowStabilizationMatrix = new Matrix4f();
    private final Matrix4f scratchCelestialModelViewMatrix = new Matrix4f();
    private final Vector3f scratchVector = new Vector3f();
    private final Vector3f scratchTaaOffset = new Vector3f();
    private final Vector3f managedSkyColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private final Vector3f capturedSkyColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private final Vector3f managedSkyUpPosition = new Vector3f(0.0f, 100.0f, 0.0f);
    private final Vector3f managedSkySunPosition = new Vector3f(0.0f, 100.0f, 0.0f);
    private final Vector3f managedSkyMoonPosition = new Vector3f(0.0f, -100.0f, 0.0f);
    private final Vector3f managedSkyShadowLightPosition = new Vector3f(0.0f, 100.0f, 0.0f);
    private final Vector4f scratchShadowOrigin = new Vector4f();
    private final Vector3d shadowCameraPosition = new Vector3d();
    private final Vector3d worldCameraPosition = new Vector3d();
    @Getter
    private final Vector3d previousWorldCameraPosition = new Vector3d();
    private final Vector3d servedPreviousWorldCameraPosition = new Vector3d();
    private final Vector3d scratchShadowViewportPosition = new Vector3d();
    private final float[] fogColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] clearColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    private boolean fogColorInitialized;

    private @Nullable GlVertexArray fullscreenVertexArray;
    private int fullscreenQuadBuffer;
    private @Nullable ActiniumPostTargets postTargets;
    private @Nullable ActiniumPostTargets preSceneTargets;
    private @Nullable ActiniumShadowTargets shadowTargets;
    private @Nullable ActiniumWorldTargets worldTargets;
    private @Nullable ActiniumWorldProgram skyProgram;
    private @Nullable ActiniumWorldProgram skyTexturedProgram;
    private @Nullable ActiniumWorldProgram cloudsProgram;
    private @Nullable ActiniumWorldProgram particlesProgram;
    private @Nullable ActiniumWorldProgram weatherProgram;
    private @Nullable ActiniumWorldProgram entitiesProgram;
    private @Nullable ActiniumWorldProgram activeWorldProgram;
    private @Nullable ActiniumWorldProgram activeEntityWorldProgram;
    private @Nullable WorldStageGlState worldStageGlState;
    private @Nullable GlProgram<ActiniumShadowShaderInterface> shadowEntityProgram;
    private @Nullable GlProgram<ActiniumShadowShaderInterface> activeShadowEntityProgram;
    private @Nullable GlProgram<ActiniumPostShaderInterface> finalProgram;
    private int[] finalProgramMipmappedBuffers = new int[0];
    private Map<Integer, Boolean> finalProgramExplicitFlips = Collections.emptyMap();
    private @Nullable GlProgram<ActiniumPostShaderInterface> blitProgram;
    private @Nullable GlProgram<ActiniumPostShaderInterface> sceneDepthPackProgram;
    private @Nullable GlProgram<ActiniumPostShaderInterface> firstPersonDepthMergeProgram;
    private @Nullable GlProgram<ActiniumPostShaderInterface> deferredSkyPackProgram;
    private @Nullable GlProgram<ActiniumPostShaderInterface> deferredSkyMergeProgram;
    private @Nullable Integer whiteTexture;
    private @Nullable Integer noiseTexture;
    private @Nullable Integer terrainGaux2Texture;
    private @Nullable Integer terrainGaux1Texture;
    private @Nullable Integer terrainDepthTexture0;
    private @Nullable Integer terrainDepthTexture1;
    private @Nullable Integer postCompositeScratchDepthTexture;
    private int terrainInputTextureWidth = -1;
    private int terrainInputTextureHeight = -1;
    private int postCompositeScratchDepthTextureWidth = -1;
    private int postCompositeScratchDepthTextureHeight = -1;
    private final Map<String, Integer> packTextureCache = new HashMap<>();
    private @Nullable String activePostTextureStage;
    private List<ActiniumPostProgram> preScenePrograms = Collections.emptyList();
    private List<ActiniumPostProgram> scenePrograms = Collections.emptyList();
    private boolean worldTargetsPrepared;
    private boolean prepareProgramsExecutedThisFrame;
    private boolean deferredProgramsExecutedThisFrame;
    private boolean preTranslucentDepthCapturedThisFrame;
    private boolean postPipelineDeferredForFirstPersonThisFrame;
    private int terrainInputsPreparedFrame = Integer.MIN_VALUE;
    private int terrainInputsPreparedFramebufferTexture = -1;
    private int terrainInputsPreparedWidth = -1;
    private int terrainInputsPreparedHeight = -1;
    private int terrainInputsPreparedMask;
    private int previousUniformFrame = Integer.MIN_VALUE;
    private boolean previousUniformInitialized;
    private boolean managedSkyCelestialStateValid;
    private boolean fogColorCapturedFromGlBufferThisFrame;
    private boolean skyColorCapturedThisFrame;
    private boolean centerDepthCapturedThisFrame;
    @Getter
    private float centerDepthSmooth;
    private final int[] boundPostImageUnits = new int[8];
    private int scratchCopyReadFramebuffer;
    private int scratchCopyDrawFramebuffer;
    private final FloatBuffer scratchCapturedMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer shadowTerrainProjectionBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer shadowTerrainModelViewBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer scratchGlMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer scratchFocusDepthBuffer = BufferUtils.createFloatBuffer((FOCUS_DEPTH_SAMPLE_RADIUS * 2 + 1) * (FOCUS_DEPTH_SAMPLE_RADIUS * 2 + 1));
    private final FloatBuffer scratchShadowObjectCoordsBuffer = GLAllocation.createDirectFloatBuffer(3);
    private final IntBuffer scratchShadowViewportBuffer = GLAllocation.createDirectIntBuffer(16);

    private ActiniumRenderPipeline() {
    }

    private void transitionPhase(ActiniumPipelinePhase nextPhase) {
        if (this.currentPhase == nextPhase) {
            return;
        }

        ActiniumPipelinePhase previousPhase = this.currentPhase;
        this.currentPhase = nextPhase;

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Pipeline phase transition {} -> {}", previousPhase, nextPhase);
        }
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
        this.updateShaderCameraPositions();
        this.frameCounter = nextFrameId(this.frameCounter);
        this.worldTargetsPrepared = false;
        this.prepareProgramsExecutedThisFrame = false;
        this.deferredProgramsExecutedThisFrame = false;
        this.preTranslucentDepthCapturedThisFrame = false;
        this.postPipelineDeferredForFirstPersonThisFrame = false;
        this.fogColorCapturedFromGlBufferThisFrame = false;
        this.skyColorCapturedThisFrame = false;
        this.centerDepthCapturedThisFrame = false;
        this.fogColorInitialized = false;
        this.currentStage = ActiniumRenderStage.WORLD;
        this.transitionPhase(ActiniumPipelinePhase.WORLD);
        this.debugLogFogState("beginWorld", "reset");
    }

    public void endWorld() {
        this.debugLogFogState("endWorld", "before-clear-stage");
        this.currentStage = ActiniumRenderStage.NONE;
        this.transitionPhase(ActiniumPipelinePhase.NONE);
    }

    public void finalizeWorldBeforeHand() {
        if (this.currentStage != ActiniumRenderStage.WORLD) {
            return;
        }

        this.captureWorldState();
        this.captureCenterDepthFromMainFramebuffer("finalizeWorldBeforeHand");
        this.debugLogFrameStageSummary("finalizeWorldBeforeHand");
        this.debugLogProjectionSnapshot("finalizeWorldBeforeHand.currentProjection", this.gbufferProjectionMatrix);
        this.prepareDeferredPostForFirstPerson();
        this.endWorld();
    }

    public void finalizeWorldAfterHand(float partialTicks) {
        if (this.postPipelineDeferredForFirstPersonThisFrame) {
            this.debugLogFrameStageSummary("finalizeWorldAfterHand.deferredPost");
            this.renderPostPipeline(partialTicks);
            return;
        }

        if (this.currentStage != ActiniumRenderStage.WORLD) {
            return;
        }

        this.captureWorldState();
        this.captureCenterDepthFromMainFramebuffer("finalizeWorldAfterHand");
        this.debugLogFrameStageSummary("finalizeWorldAfterHand.immediatePost");
        this.debugLogProjectionSnapshot("finalizeWorldAfterHand.currentProjection", this.gbufferProjectionMatrix);
        this.endWorld();

        if (this.hasPostProgram()) {
            this.renderPostPipeline(partialTicks);
        }
    }

    public void beginSky() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.SKY;
        this.transitionPhase(ActiniumPipelinePhase.SKY);
        this.debugLogFogState("beginSky", "stage-switch");
    }

    public void endSky() {
        if (this.currentStage == ActiniumRenderStage.SKY || this.currentStage == ActiniumRenderStage.SKY_TEXTURED) {
            this.debugLogFogState("endSky", "stage-switch");
            this.currentStage = ActiniumRenderStage.WORLD;
            this.transitionPhase(ActiniumPipelinePhase.WORLD);
        }
    }

    public void beginSkyTextured() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.SKY_TEXTURED;
        this.transitionPhase(ActiniumPipelinePhase.SKY_TEXTURED);
        this.debugLogFogState("beginSkyTextured", "stage-switch");
    }

    public void beginManagedSky(float partialTicks) {
        this.syncReloadState();
        this.captureManagedSkyUpPosition();
        this.managedSkyCelestialStateValid = false;
        this.currentStage = ActiniumRenderStage.SKY_TEXTURED;
        this.transitionPhase(ActiniumPipelinePhase.SKY_TEXTURED);
        this.debugLogFogState("beginManagedSky", "stage-switch");
        this.bindWorldStageProgram(partialTicks);
    }

    public void updateManagedSkyTextureState(boolean textured, float partialTicks) {
        if (this.currentStage != ActiniumRenderStage.SKY && this.currentStage != ActiniumRenderStage.SKY_TEXTURED) {
            return;
        }

        ActiniumRenderStage targetStage = textured ? ActiniumRenderStage.SKY_TEXTURED : ActiniumRenderStage.SKY;
        if (this.currentStage == targetStage && this.activeWorldProgram != null) {
            return;
        }

        this.currentStage = targetStage;
        this.bindWorldStageProgram(partialTicks);
    }

    public void refreshManagedSkyProgram(float partialTicks) {
        if (this.currentStage != ActiniumRenderStage.SKY && this.currentStage != ActiniumRenderStage.SKY_TEXTURED) {
            return;
        }

        if (this.activeWorldProgram == null) {
            return;
        }

        this.bindWorldStageProgram(partialTicks);
    }

    public void endManagedSky() {
        this.unbindWorldStageProgram();
        this.managedSkyCelestialStateValid = false;
        this.endSky();
    }

    public void captureManagedSkyPreCelestialState(float partialTicks) {
        this.captureManagedSkyUpPosition();
    }

    public void captureManagedSkyPostCelestialState(float partialTicks) {
        this.captureManagedSkyColor(partialTicks);
        this.captureManagedSkyCelestialState();
        this.refreshManagedSkyProgram(partialTicks);
    }

    public void beginClouds() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.CLOUDS;
        this.transitionPhase(ActiniumPipelinePhase.CLOUDS);
    }

    public void beginEntities() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.ENTITIES;
        this.transitionPhase(ActiniumPipelinePhase.ENTITIES);
    }

    public void beginParticles() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.PARTICLES;
        this.transitionPhase(ActiniumPipelinePhase.PARTICLES);
    }

    public void endEntities() {
        if (this.currentStage == ActiniumRenderStage.ENTITIES) {
            this.currentStage = ActiniumRenderStage.WORLD;
            this.transitionPhase(ActiniumPipelinePhase.WORLD);
        }
    }

    public void endParticles() {
        if (this.currentStage == ActiniumRenderStage.PARTICLES) {
            this.currentStage = ActiniumRenderStage.WORLD;
            this.transitionPhase(ActiniumPipelinePhase.WORLD);
        }
    }

    public void endClouds() {
        if (this.currentStage == ActiniumRenderStage.CLOUDS) {
            this.currentStage = ActiniumRenderStage.WORLD;
            this.transitionPhase(ActiniumPipelinePhase.WORLD);
        }
    }

    public void beginWeather() {
        this.syncReloadState();
        this.currentStage = ActiniumRenderStage.WEATHER;
        this.transitionPhase(ActiniumPipelinePhase.WEATHER);
        this.debugLogFogState("beginWeather", "stage-switch");
    }

    public void endWeather() {
        if (this.currentStage == ActiniumRenderStage.WEATHER) {
            this.debugLogFogState("endWeather", "stage-switch");
            this.currentStage = ActiniumRenderStage.WORLD;
            this.transitionPhase(ActiniumPipelinePhase.WORLD);
        }
    }

    public void beginPost() {
        this.syncReloadState();
        this.postReturnStage = this.currentStage;
        this.postReturnPhase = this.currentPhase;
        this.currentStage = ActiniumRenderStage.POST;
        this.transitionPhase(ActiniumPipelinePhase.POST);
        this.debugLogFogState("beginPost", "stage-switch");
    }

    public void endPost() {
        if (this.currentStage == ActiniumRenderStage.POST) {
            this.debugLogFogState("endPost", "stage-switch");
            this.currentStage = this.postReturnStage;
            this.postReturnStage = ActiniumRenderStage.NONE;
            this.transitionPhase(this.postReturnPhase);
            this.postReturnPhase = ActiniumPipelinePhase.NONE;
        }
    }

    public void captureWorldState() {
        this.syncReloadState();
        captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.gbufferModelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, this.gbufferProjectionMatrix);
        this.gbufferModelViewInverseMatrix.set(this.gbufferModelViewMatrix).invert();
        this.gbufferProjectionInverseMatrix.set(this.gbufferProjectionMatrix).invert();

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog(
                    "Captured world state with preserved fog color [{}, {}, {}] from {} source",
                    this.fogColor[0],
                    this.fogColor[1],
                    this.fogColor[2],
                    this.fogColorCapturedFromGlBufferThisFrame ? "GL" : "fallback"
            );
        }
        this.debugLogFogState("captureWorldState", "matrix-capture");
    }

    private void captureInterpolatedWorldCameraPosition(float partialTicks) {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null) {
            this.worldCameraPosition.zero();
            return;
        }

        double cameraX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double cameraY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
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

    private void prepareDeferredPostForFirstPerson() {
        this.postPipelineDeferredForFirstPersonThisFrame = false;

        if (!ActiniumShaderPackManager.areShadersEnabled() || !this.hasPostProgram()) {
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

        if (!this.preTranslucentDepthCapturedThisFrame) {
            this.postTargets.copyPreTranslucentDepth(mainFramebuffer);
        }

        // Preserve world depth before vanilla clears it for the hand pass.
        this.postTargets.copyCurrentDepth(mainFramebuffer, 0);
        this.postTargets.copyCurrentDepth(mainFramebuffer, 2);
        this.postPipelineDeferredForFirstPersonThisFrame = true;
        this.debugLogFrameStageSummary("prepareDeferredPostForFirstPerson");
        this.debugLogDepthTextureSample("prepareDeferredPostForFirstPerson.depth0", this.postTargets.getDepthTexture(0), this.width, this.height);
        this.debugLogDepthTextureSample("prepareDeferredPostForFirstPerson.depth1", this.postTargets.getDepthTexture(1), this.width, this.height);
        this.debugLogDepthTextureSample("prepareDeferredPostForFirstPerson.depth2", this.postTargets.getDepthTexture(2), this.width, this.height);

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Deferred scene post until after first-person rendering");
        }
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
        boolean deferredPostAfterFirstPerson = this.postPipelineDeferredForFirstPersonThisFrame;

        if (!ENABLE_EXTERNAL_SCENE_PIPELINE && !ENABLE_EXTERNAL_FINAL_PIPELINE) {
            if (deferredPostAfterFirstPerson) {
                this.postPipelineDeferredForFirstPersonThisFrame = false;
            }
            return;
        }

        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            if (deferredPostAfterFirstPerson) {
                this.postPipelineDeferredForFirstPersonThisFrame = false;
            }
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer mainFramebuffer = minecraft.getFramebuffer();

        if (mainFramebuffer == null || mainFramebuffer.framebufferTexture <= 0) {
            if (deferredPostAfterFirstPerson) {
                this.postPipelineDeferredForFirstPersonThisFrame = false;
            }
            return;
        }

        this.width = Math.max(1, mainFramebuffer.framebufferWidth);
        this.height = Math.max(1, mainFramebuffer.framebufferHeight);

        List<ActiniumPostProgram> scenePrograms = this.getScenePrograms();
        GlProgram<ActiniumPostShaderInterface> finalProgram = this.getFinalProgram();

        if (scenePrograms.isEmpty() && finalProgram == null) {
            if (deferredPostAfterFirstPerson) {
                this.postPipelineDeferredForFirstPersonThisFrame = false;
            }
            return;
        }

        this.ensureRuntimeResources();
        ShadowPassGlState previousState = ShadowPassGlState.capture();
        this.beginPost();
        this.debugLogFogState("renderPostPipeline", "entry");
        this.debugLogFrameStageSummary(deferredPostAfterFirstPerson ? "renderPostPipeline.deferred" : "renderPostPipeline.direct");
        this.debugLogProjectionSnapshot("renderPostPipeline.currentProjection", this.gbufferProjectionMatrix);
        this.debugLogProjectionSnapshot("renderPostPipeline.previousProjection", this.getPreviousGbufferProjectionMatrix());
        this.debugLogPipelineOverwriteRisk("scene-post-entry", !scenePrograms.isEmpty() || finalProgram != null);

        try {
            this.debugLogPipelineSnapshot("scene-post.before-copy", mainFramebuffer);
            if (this.postTargets != null) {
                this.postTargets.ensureSize(this.width, this.height);
                if (deferredPostAfterFirstPerson) {
                    this.postTargets.copySceneColors(mainFramebuffer, this.getWorldGaux4TextureForPost());
                    this.postTargets.copyCurrentDepth(mainFramebuffer, 0);
                    this.mergeFirstPersonDepthIntoSceneDepth(this.postTargets, partialTicks);
                    this.debugLogDepthTextureSample("renderPostPipeline.deferred.mergedDepth0", this.postTargets.getDepthTexture(0), this.width, this.height);
                } else {
                    this.postTargets.copySceneTextures(mainFramebuffer, this.getWorldGaux4TextureForPost());
                }
                if (this.hasPreSceneResults()) {
                    int[] preSceneOutputTargets = this.collectPreSceneOutputTargets();
                    this.postTargets.copyTargetsFrom(this.preSceneTargets, preSceneOutputTargets);
                    if (this.shouldEmitVerboseDebugFrame()) {
                        this.debugLog("Merged pre-scene outputs into scene post targets: {}", Arrays.toString(preSceneOutputTargets));
                    }
                }
                this.applyExplicitPreFlips(this.postTargets, "composite_pre");

                if (this.deferredProgramsExecutedThisFrame) {
                    this.mergeDeferredSkyIntoScene(this.postTargets, partialTicks);
                }

                if (!this.centerDepthCapturedThisFrame) {
                    if (!this.preTranslucentDepthCapturedThisFrame) {
                        this.postTargets.copyPreTranslucentDepth(mainFramebuffer);
                    }
                    this.updateCenterDepthSmooth(this.postTargets.getDepthTexture(0));
                }

                this.packSceneDepthIntoColortex1(this.postTargets, partialTicks);
                this.debugLogPreSceneTargets();
            }
            this.debugLogPipelineSnapshot("scene-post.after-copy", mainFramebuffer);
            this.executeScenePrograms(scenePrograms, partialTicks);
            this.debugLogPipelineSnapshot("scene-post.after-scene-programs", mainFramebuffer);
            this.renderFinalPass(mainFramebuffer, finalProgram, partialTicks);
            this.debugLogPipelineSnapshot("scene-post.after-final", mainFramebuffer);
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to execute Actinium shader pack post pipeline; disabling external post programs until the next shader reload", e);
            this.disableExternalProgramsUntilReload();
        } finally {
            if (deferredPostAfterFirstPerson) {
                this.postPipelineDeferredForFirstPersonThisFrame = false;
            }
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
            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog("Skipping prepare pre-scene stage because it already ran this frame");
            }
            return;
        }

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Running prepare pre-scene stage before terrain");
        }
        this.debugLogFogState("renderPreparePipeline", "entry");
        this.debugLogMainFramebufferSnapshot("prepare.pipeline.before");
        this.renderPreparePrograms(partialTicks);
        this.debugLogMainFramebufferSnapshot("prepare.pipeline.after");
        this.prepareProgramsExecutedThisFrame = true;
    }

    public void renderDeferredPipeline(float partialTicks) {
        if (!ENABLE_PRE_SCENE_PIPELINES) {
            return;
        }

        if (this.deferredProgramsExecutedThisFrame) {
            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog("Skipping deferred pre-scene stage because it already ran this frame");
            }
            return;
        }

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Running deferred pre-scene stage before water");
        }
        this.debugLogFogState("renderDeferredPipeline", "entry");
        this.debugLogMainFramebufferSnapshot("deferred.pipeline.before");
        this.renderDeferredPrograms(partialTicks);
        this.debugLogMainFramebufferSnapshot("deferred.pipeline.after");
        this.deferredProgramsExecutedThisFrame = true;
    }

    private void renderPreparePrograms(float partialTicks) {
        this.renderPreCompositePipeline(partialTicks, this.getPreparePrograms(), "prepare", this::preparePreSceneTargetsForPrepare);
        this.syncWorldStageGaux4FromPreSceneTargets("prepare");
    }

    private void renderDeferredPrograms(float partialTicks) {
        this.renderPreCompositePipeline(
                partialTicks,
                this.getDeferredPrograms(),
                "deferred",
                mainFramebuffer -> this.preparePreSceneTargetsForDeferred(mainFramebuffer, false)
        );
        this.syncWorldStageGaux4FromPreSceneTargets("deferred");
    }

    private void renderPreCompositePipeline(float partialTicks,
                                            List<ActiniumPostProgram> programs,
                                            String stageName,
                                            PreSceneTargetInitializer initializer) {
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
            this.debugLogPipelineSnapshot(stageName + ".pipeline.before-init", mainFramebuffer);
            if (this.preSceneTargets != null) {
                initializer.initialize(mainFramebuffer);
                this.applyExplicitPreFlips(this.preSceneTargets, stageName + "_pre");

                this.updateCenterDepthSmooth();
                this.debugLogTextureCenter(stageName + ".entry.colortex1", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
                this.debugLogTextureCenter(stageName + ".entry.gaux4", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
            }
            this.debugLogPipelineSnapshot(stageName + ".pipeline.after-init", mainFramebuffer);

            if (this.shouldExecutePreSceneShaders(stageName) && this.preSceneTargets != null) {
                this.executePostPrograms(programs, this.preSceneTargets, partialTicks);
            } else if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog("Skipping pre-scene shader execution for '{}' while keeping pre-pass buffer initialization active", stageName);
            }
            this.debugLogPipelineSnapshot(stageName + ".pipeline.after-exec", mainFramebuffer);
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to execute Actinium shader pack {} pipeline; disabling external post programs until the next shader reload", stageName, e);
            this.disableExternalProgramsUntilReload();
        } finally {
            previousState.restore(mainFramebuffer, this.width, this.height);
            this.endPost();
        }
    }

    private void preparePreSceneTargetsForPrepare(Framebuffer mainFramebuffer) {
        if (this.preSceneTargets == null) {
            return;
        }

        this.preSceneTargets.ensureSize(this.width, this.height);
        this.preSceneTargets.copySceneTextures(mainFramebuffer, this.getWorldGaux4TextureForPost());
    }

    private void preparePreSceneTargetsForDeferred(Framebuffer mainFramebuffer, boolean preservePrepareResults) {
        if (this.preSceneTargets == null) {
            return;
        }

        this.preSceneTargets.ensureSize(this.width, this.height);
        Integer worldGaux4Texture = this.getWorldGaux4TextureForPost();

        if (preservePrepareResults) {
            this.preSceneTargets.copyPostSceneInputs(mainFramebuffer, worldGaux4Texture);
        } else {
            this.preSceneTargets.copySceneTextures(mainFramebuffer, worldGaux4Texture);
        }

        this.preSceneTargets.copyPreTranslucentDepth(mainFramebuffer);
    }

    private void restoreMainFramebufferState(Framebuffer mainFramebuffer) {
        debugResetGlErrors("restoreMainFramebufferState.entry");
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
            setActiveTextureUnit(unit);
            GlStateManager.bindTexture(0);
        }

        setActiveTextureUnit(0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        debugCheckGlErrors("restoreMainFramebufferState.exit");
    }

    private void restoreScreenRenderState(@Nullable Framebuffer framebuffer, int width, int height) {
        debugResetGlErrors("restoreScreenRenderState.entry");
        this.restoreMainFramebufferState(framebuffer);
        debugCheckGlErrors("restoreScreenRenderState.afterMainFramebuffer");
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
        debugCheckGlErrors("restoreScreenRenderState.exit");
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

    public boolean hasParticleProgram() {
        this.syncReloadState();
        return this.particleProgramAvailable;
    }

    public boolean shouldUseWeatherProgram() {
        this.syncReloadState();
        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return false;
        }

        ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();
        return properties.isWeather() && this.hasWeatherProgram();
    }

    public boolean shouldRenderWeatherParticles() {
        this.syncReloadState();
        if (!ActiniumShaderPackManager.areShadersEnabled()) {
            return true;
        }

        return ActiniumShaderPackManager.getActiveShaderProperties().isWeatherParticles();
    }

    public boolean shouldUseParticleProgram() {
        this.syncReloadState();
        return ActiniumShaderPackManager.areShadersEnabled()
                && this.hasParticleProgram();
    }

    public boolean hasPostProgram() {
        this.syncReloadState();
        return this.postProgramAvailable;
    }

    public boolean shouldSuppressVanillaSkyGeometry() {
        this.syncReloadState();
        return ActiniumShaderPackManager.areShadersEnabled()
                && this.shouldUseExternalWorldPrograms()
                && ENABLE_EXTERNAL_SKY_BASIC_STAGE
                && this.skyProgramAvailable;
    }

    public boolean shouldApplySunPathRotationToVanillaSky() {
        this.syncReloadState();
        return ActiniumShaderPackManager.areShadersEnabled()
                && this.shouldUseExternalWorldPrograms()
                && this.skyProgramAvailable
                && (ENABLE_EXTERNAL_SKY_BASIC_STAGE || ENABLE_EXTERNAL_SKY_TEXTURED_STAGE);
    }

    public boolean shouldSuppressVanillaSkyHorizonGeometry() {
        this.syncReloadState();
        return ActiniumShaderPackManager.areShadersEnabled()
                && this.shouldUseExternalWorldPrograms()
                && ENABLE_EXTERNAL_SKY_BASIC_STAGE
                && this.skyProgramAvailable;
    }

    public int getCloudRenderModeOverride(int currentCloudMode, int renderDistanceChunks) {
        if (!ActiniumShaderPackManager.areShadersEnabled() || renderDistanceChunks < 4) {
            return renderDistanceChunks >= 4 ? currentCloudMode : 0;
        }

        String cloudSetting = ActiniumShaderPackManager.getActiveShaderProperties().getCloudSetting();
        if (cloudSetting == null) {
            return currentCloudMode;
        }

        return switch (cloudSetting.trim().toLowerCase(Locale.ROOT)) {
            case "off" -> 0;
            case "fast" -> 1;
            case "fancy", "on" -> 2;
            default -> currentCloudMode;
        };
    }

    public void renderShaderCoreSkyHorizon() {
        if (!ActiniumShaderPackManager.areShadersEnabled() || this.currentStage != ActiniumRenderStage.SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        float farDistance = minecraft.gameSettings.renderDistanceChunks * 16.0f;
        double xzq = farDistance * 0.9238D;
        double xzp = farDistance * 0.3826D;
        double xzn = -xzp;
        double xzm = -xzq;
        double top = 16.0D;
        double bot = -this.worldCameraPosition.y;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        float[] fogColor = this.getFogColor();
        GlStateManager.color(fogColor[0], fogColor[1], fogColor[2], 1.0f);
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buildHorizonQuad(bufferBuilder, xzn, xzm, xzm, xzn, bot, top);
        buildHorizonQuad(bufferBuilder, xzm, xzn, xzm, xzp, bot, top);
        buildHorizonQuad(bufferBuilder, xzm, xzp, xzn, xzq, bot, top);
        buildHorizonQuad(bufferBuilder, xzn, xzq, xzp, xzq, bot, top);
        buildHorizonQuad(bufferBuilder, xzp, xzq, xzq, xzp, bot, top);
        buildHorizonQuad(bufferBuilder, xzq, xzp, xzq, xzn, bot, top);
        buildHorizonQuad(bufferBuilder, xzq, xzn, xzp, xzm, bot, top);
        buildHorizonQuad(bufferBuilder, xzp, xzm, xzn, xzm, bot, top);
        buildHorizonBottom(bufferBuilder, xzm, xzm, xzm, xzq, xzq, xzq, xzq, xzm, bot);

        tessellator.draw();

        GlStateManager.color(this.managedSkyColor.x, this.managedSkyColor.y, this.managedSkyColor.z, 1.0f);

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog(
                    "Rendered shader-core sky horizon with farDistance {} and cameraY {}",
                    farDistance,
                    this.worldCameraPosition.y
            );
        }
    }

    private static void buildHorizonQuad(BufferBuilder bufferBuilder, double x1, double z1, double x2, double z2, double bot, double top) {
        bufferBuilder.pos(x1, bot, z1).endVertex();
        bufferBuilder.pos(x1, top, z1).endVertex();
        bufferBuilder.pos(x2, top, z2).endVertex();
        bufferBuilder.pos(x2, bot, z2).endVertex();
    }

    private static void buildHorizonBottom(
            BufferBuilder bufferBuilder,
            double x1,
            double z1,
            double x2,
            double z2,
            double x3,
            double z3,
            double x4,
            double z4,
            double bot
    ) {
        bufferBuilder.pos(x1, bot, z1).endVertex();
        bufferBuilder.pos(x2, bot, z2).endVertex();
        bufferBuilder.pos(x3, bot, z3).endVertex();
        bufferBuilder.pos(x4, bot, z4).endVertex();
    }

    public void debugLogSkySegment(String segment) {
        if (!this.shouldEmitVerboseDebugFrame()) {
            return;
        }

        this.debugLog(
                "Sky segment '{}' stage={}, suppressSky={}, suppressHorizon={}, applyVanillaSunPathRotation={}, skyProgram={}, postProgram={}",
                segment,
                this.currentStage,
                this.shouldSuppressVanillaSkyGeometry(),
                this.shouldSuppressVanillaSkyHorizonGeometry(),
                this.shouldApplySunPathRotationToVanillaSky(),
                this.skyProgramAvailable,
                this.postProgramAvailable
        );
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity entity = minecraft.getRenderViewEntity();
        Vec3d worldSkyColor = minecraft.world != null && entity != null
                ? minecraft.world.getSkyColor(entity, minecraft.getRenderPartialTicks())
                : null;
        Vec3d worldFogColor = minecraft.world != null
                ? minecraft.world.getFogColor(minecraft.getRenderPartialTicks())
                : null;
        float[] fogColor = this.getFogColor();
        this.debugLog(
                "Sky colors '{}' activeProgram={} capturedSky=[{}, {}, {}] managedSky=[{}, {}, {}] fog=[{}, {}, {}] clear=[{}, {}, {}] worldSky={} worldFog={}",
                segment,
                this.getActiveWorldProgramName(),
                this.capturedSkyColor.x,
                this.capturedSkyColor.y,
                this.capturedSkyColor.z,
                this.managedSkyColor.x,
                this.managedSkyColor.y,
                this.managedSkyColor.z,
                fogColor[0],
                fogColor[1],
                fogColor[2],
                this.clearColor[0],
                this.clearColor[1],
                this.clearColor[2],
                worldSkyColor,
                worldFogColor
        );
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

    public Matrix4fc getSkyStageProjectionMatrix() {
        return this.skyStageProjectionMatrix;
    }

    public Matrix4fc getSkyStageProjectionInverseMatrix() {
        return this.skyStageProjectionInverseMatrix;
    }

    public Vector3d getWorldCameraPosition() {
        return this.worldCameraPosition;
    }

    public Vector3d getShaderCameraPosition() {
        return this.shaderCameraPosition;
    }

    public Vector3d getPreviousShaderCameraPosition() {
        this.updatePreviousUniformSnapshots();
        return this.previousShaderCameraPosition;
    }

    public Vector3d getShaderCameraPositionUnshifted() {
        return this.shaderCameraPositionUnshifted;
    }

    public Vector3d getPreviousShaderCameraPositionUnshifted() {
        this.updatePreviousUniformSnapshots();
        return this.previousShaderCameraPositionUnshifted;
    }

    public Vector3f getManagedSkyColor() {
        return this.managedSkyColor;
    }

    public Vector3f getCapturedSkyColor() {
        return this.capturedSkyColor;
    }

    public boolean hasCapturedSkyColor() {
        return this.skyColorCapturedThisFrame;
    }

    public float[] getClearColor() {
        return this.clearColor;
    }

    public float[] getFogColor() {
        if (this.fogColorInitialized) {
            this.debugLogFogState("getFogColor", "cached");
            return this.fogColor;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world != null) {
            Vec3d fallbackFogColor = minecraft.world.getFogColor(minecraft.getRenderPartialTicks());
            this.setFogColor((float) fallbackFogColor.x, (float) fallbackFogColor.y, (float) fallbackFogColor.z);
            this.debugLogFogState("getFogColor", "world-fallback");
            return this.fogColor;
        }

        EntityRenderer entityRenderer = minecraft.entityRenderer;
        if (entityRenderer != null) {
            EntityRendererAccessor accessor = (EntityRendererAccessor) (Object) entityRenderer;
            this.setFogColor(accessor.celeritas$getFogColorRed(), accessor.celeritas$getFogColorGreen(), accessor.celeritas$getFogColorBlue());
            this.debugLogFogState("getFogColor", "entity-renderer-fallback");
        }

        this.debugLogFogState("getFogColor", "final");
        return this.fogColor;
    }

    public void captureFogColor(float red, float green, float blue) {
        this.captureFallbackFogColor(red, green, blue, "legacy");
    }

    public void captureGlFogColor(float red, float green, float blue) {
        this.setFogColor(red, green, blue);
        this.fogColorCapturedFromGlBufferThisFrame = true;

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Captured GL fog color [{}, {}, {}] during {}", red, green, blue, this.currentStage);
        }
        this.debugLogFogState("captureGlFogColor", "gl-buffer");
    }

    public void captureFallbackFogColor(float red, float green, float blue, String source) {
        if (this.fogColorCapturedFromGlBufferThisFrame) {
            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog(
                        "Skipped fallback fog color [{}, {}, {}] from {} because GL fog is already authoritative",
                        red,
                        green,
                        blue,
                        source
                );
            }
            return;
        }

        this.setFogColor(red, green, blue);

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Captured fallback fog color [{}, {}, {}] from {}", red, green, blue, source);
        }
        this.debugLogFogState("captureFallbackFogColor", source);
    }

    private void setFogColor(float red, float green, float blue) {
        this.fogColor[0] = red;
        this.fogColor[1] = green;
        this.fogColor[2] = blue;
        this.fogColor[3] = 1.0f;
        this.fogColorInitialized = true;
    }

    public void captureSkyColor(float red, float green, float blue) {
        this.capturedSkyColor.set(red, green, blue);
        this.managedSkyColor.set(red, green, blue);
        this.skyColorCapturedThisFrame = true;
    }

    public void captureClearColor(float red, float green, float blue, float alpha) {
        this.clearColor[0] = red;
        this.clearColor[1] = green;
        this.clearColor[2] = blue;
        this.clearColor[3] = alpha;
        this.debugLogFogState("captureClearColor", "clear-buffer");
    }

    public @Nullable String getActiveWorldProgramName() {
        return this.activeWorldProgram != null ? this.activeWorldProgram.name() : null;
    }

    public Vector3f getManagedSkySunPosition() {
        return this.managedSkySunPosition;
    }

    public Vector3f getManagedSkyUpPosition() {
        return this.managedSkyUpPosition;
    }

    public Vector3f getManagedSkyMoonPosition() {
        return this.managedSkyMoonPosition;
    }

    public Vector3f getManagedSkyShadowLightPosition() {
        return this.managedSkyShadowLightPosition;
    }

    public boolean hasManagedSkyCelestialState() {
        return this.managedSkyCelestialStateValid;
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
        this.debugLogProjectionSnapshot("previousUniform.servedProjection", this.servedPreviousGbufferProjectionMatrix);

        this.previousGbufferModelViewMatrix.set(this.gbufferModelViewMatrix);
        this.previousGbufferProjectionMatrix.set(this.gbufferProjectionMatrix);
        this.previousWorldCameraPosition.set(this.worldCameraPosition);
        this.debugLogProjectionSnapshot("previousUniform.nextProjection", this.previousGbufferProjectionMatrix);
    }

    public Matrix4f getTemporalJitteredProjection(Matrix4fc source, Matrix4f destination) {
        destination.set(source);
        this.applyTemporalJitterToProjection(destination);
        return destination;
    }

    public int getFrameMod() {
        return Math.floorMod(this.frameCounter, 16);
    }

    public float getFrameMod8() {
        return Math.floorMod(this.frameCounter, 8);
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

    public int getCameraPositionIntX() {
        return (int) Math.floor(this.shaderCameraPositionUnshifted.x);
    }

    public int getCameraPositionIntY() {
        return (int) Math.floor(this.shaderCameraPositionUnshifted.y);
    }

    public int getCameraPositionIntZ() {
        return (int) Math.floor(this.shaderCameraPositionUnshifted.z);
    }

    public float getCameraPositionFractX() {
        return (float) (this.shaderCameraPositionUnshifted.x - Math.floor(this.shaderCameraPositionUnshifted.x));
    }

    public float getCameraPositionFractY() {
        return (float) (this.shaderCameraPositionUnshifted.y - Math.floor(this.shaderCameraPositionUnshifted.y));
    }

    public float getCameraPositionFractZ() {
        return (float) (this.shaderCameraPositionUnshifted.z - Math.floor(this.shaderCameraPositionUnshifted.z));
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
        this.captureInterpolatedWorldCameraPosition(partialTicks);
        this.debugLogFogState("renderShadowPass", "entry");

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

        int resolution = chooseShadowResolution(properties);
        this.ensureRuntimeResources();
        this.computeShadowMatrices(partialTicks, properties, resolution);
        ActiniumPipelinePhase previousPhase = this.currentPhase;
        this.renderingShadowPass = true;
        this.transitionPhase(ActiniumPipelinePhase.SHADOW);
        try {
            if (!this.renderShadowTerrainPass(properties, resolution, partialTicks)) {
                if (this.shadowTargets != null) {
                    this.shadowTargets.ensureSize(resolution);
                    this.shadowTargets.clear();
                }
            }
        } finally {
            this.renderingShadowPass = false;
            this.transitionPhase(previousPhase);
        }

        ActiniumInternalShadowRenderingState.update(this.shadowModelViewMatrix, this.shadowProjectionMatrix);
    }

    public Vector3f transformDirection(float x, float y, float z) {
        return this.gbufferModelViewMatrix.transformDirection(x, y, z, this.scratchVector).normalize();
    }

    public float getShaderCoreCelestialAngle(float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.world != null ? minecraft.world.getCelestialAngle(partialTicks) : 0.0f;
    }

    public float getShaderCoreSunAngle(float partialTicks) {
        float celestialAngle = this.getShaderCoreCelestialAngle(partialTicks);
        return celestialAngle < 0.75f ? celestialAngle + 0.25f : celestialAngle - 0.75f;
    }

    public float getShaderCoreShadowAngle(float partialTicks) {
        float shadowAngle = this.getShaderCoreSunAngle(partialTicks);
        return this.isShaderCoreShadowUsingSun(partialTicks) ? shadowAngle : shadowAngle - 0.5f;
    }

    public boolean isShaderCoreShadowUsingSun(float partialTicks) {
        return this.getShaderCoreSunAngle(partialTicks) <= 0.5f;
    }

    public void fillShaderCoreCelestialUniforms(Matrix4fc referenceModelViewMatrix,
                                                float partialTicks,
                                                Vector3f sunPosition,
                                                Vector3f moonPosition,
                                                @Nullable Vector3f shadowLightPosition) {
        float celestialAngle = this.getShaderCoreCelestialAngle(partialTicks);
        float sunPathRotation = ActiniumShaderPackManager.getActiveShaderProperties().getSunPathRotation();

        this.scratchCelestialModelViewMatrix.set(referenceModelViewMatrix)
                .rotateY((float) Math.toRadians(-90.0f))
                .rotateZ((float) Math.toRadians(sunPathRotation))
                .rotateX(celestialAngle * ((float) Math.PI * 2.0f));
        this.scratchCelestialModelViewMatrix.transformDirection(0.0f, 100.0f, 0.0f, sunPosition);
        this.scratchCelestialModelViewMatrix.transformDirection(0.0f, -100.0f, 0.0f, moonPosition);

        if (shadowLightPosition != null) {
            shadowLightPosition.set(this.isShaderCoreShadowUsingSun(partialTicks) ? sunPosition : moonPosition);
        }
    }

    public void fillShaderCoreUpPosition(Vector3f upPosition) {
        this.scratchCelestialModelViewMatrix.set(this.gbufferModelViewMatrix)
                .rotateY((float) Math.toRadians(-90.0f));
        this.scratchCelestialModelViewMatrix.transformDirection(0.0f, 100.0f, 0.0f, upPosition);

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog(
                    "Computed shader-core up position stage={} result=[{}, {}, {}]",
                    this.currentStage,
                    upPosition.x,
                    upPosition.y,
                    upPosition.z
            );
        }
    }

    public void bindTerrainShadowTextures() {
        int white = this.whiteTexture != null ? this.whiteTexture : 0;
        int shadowTex0 = this.getGbuffersTextureOverride("shadowtex0", this.shadowTargets != null ? this.shadowTargets.getDepthTexture(0) : white);
        int shadowTex1 = this.getGbuffersTextureOverride("shadowtex1", this.shadowTargets != null ? this.shadowTargets.getDepthTexture(1) : white);
        int shadowColor0 = this.getGbuffersTextureOverride("shadowcolor0", this.shadowTargets != null ? this.shadowTargets.getColorTexture(0) : white);
        int shadowColor1 = this.getGbuffersTextureOverride("shadowcolor1", this.shadowTargets != null ? this.shadowTargets.getColorTexture(1) : white);
        bindTexture(TERRAIN_SHADOW_TEX0_UNIT, shadowTex0);
        bindTexture(TERRAIN_SHADOW_TEX1_UNIT, shadowTex1);
        bindTexture(TERRAIN_SHADOW_COLOR0_UNIT, shadowColor0);
        bindTexture(POST_SHADOW_COLOR1_UNIT, shadowColor1);
        setActiveTextureUnit(0);
    }

    public void prepareTerrainInputs(boolean needsGaux1, boolean needsGaux2, boolean needsDepthtex0, boolean needsDepthtex1) {
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

        int requiredMask = 0;
        if (needsGaux1) {
            requiredMask |= TERRAIN_INPUT_GAUX1;
        }
        if (needsGaux2) {
            requiredMask |= TERRAIN_INPUT_GAUX2;
        }
        if (needsDepthtex0) {
            requiredMask |= TERRAIN_INPUT_DEPTHTEX0;
        }
        if (needsDepthtex1) {
            requiredMask |= TERRAIN_INPUT_DEPTHTEX1;
        }

        if (requiredMask == 0) {
            return;
        }

        this.width = Math.max(1, framebuffer.framebufferWidth);
        this.height = Math.max(1, framebuffer.framebufferHeight);
        boolean sameSource = this.terrainInputsPreparedFrame == this.frameCounter
                && this.terrainInputsPreparedFramebufferTexture == framebuffer.framebufferTexture
                && this.terrainInputsPreparedWidth == this.width
                && this.terrainInputsPreparedHeight == this.height;

        if (sameSource && (this.terrainInputsPreparedMask & requiredMask) == requiredMask) {
            return;
        }

        this.ensureRuntimeResources();
        debugCheckGlErrors("pipeline.prepareTerrainInputs.ensureRuntimeResources");
        if (this.shouldUseExternalTranslucentTerrainRedirect() && this.hasPostProgram()) {
            this.prepareWorldTargets(framebuffer);
            debugCheckGlErrors("pipeline.prepareTerrainInputs.prepareWorldTargets");
        }
        this.ensureTerrainInputTextures(requiredMask);
        debugCheckGlErrors("pipeline.prepareTerrainInputs.ensureTerrainInputTextures");

        if (!sameSource) {
            this.terrainInputsPreparedMask = 0;
        }

        framebuffer.bindFramebuffer(true);
        int missingMask = requiredMask & ~this.terrainInputsPreparedMask;
        if ((missingMask & TERRAIN_INPUT_GAUX1) != 0) {
            this.copyCurrentFramebufferColorToTexture(this.terrainGaux1Texture);
        }
        if ((missingMask & TERRAIN_INPUT_DEPTHTEX0) != 0) {
            this.copyFramebufferDepthToTexture(this.terrainDepthTexture0);
        }
        if ((missingMask & TERRAIN_INPUT_DEPTHTEX1) != 0) {
            this.copyFramebufferDepthToTexture(this.terrainDepthTexture1);
        }
        this.terrainInputsPreparedFrame = this.frameCounter;
        this.terrainInputsPreparedFramebufferTexture = framebuffer.framebufferTexture;
        this.terrainInputsPreparedWidth = this.width;
        this.terrainInputsPreparedHeight = this.height;
        this.terrainInputsPreparedMask |= requiredMask;
    }

    public void bindTerrainInputTextures() {
        int white = this.whiteTexture != null ? this.whiteTexture : 0;
        int normalsTexture = this.getGbuffersTextureOverride("normals", white);
        int specularTexture = this.getGbuffersTextureOverride("specular", white);
        int gaux1Texture = this.getGbuffersTextureOverride("gaux1", this.terrainGaux1Texture != null ? this.terrainGaux1Texture : white);
        int gaux2Texture = this.getGbuffersTextureOverride("gaux2", this.terrainGaux2Texture != null ? this.terrainGaux2Texture : white);
        int gaux3Texture = this.getGbuffersTextureOverride("gaux3", white);
        int depthtex0Texture = this.terrainDepthTexture0 != null ? this.terrainDepthTexture0 : 0;
        int depthtex1Texture = this.terrainDepthTexture1 != null ? this.terrainDepthTexture1 : 0;
        int noiseTextureId = this.getGbuffersTextureOverride("noisetex", this.noiseTexture);

        bindTexture(2, normalsTexture);
        bindTexture(3, specularTexture);
        bindTexture(TERRAIN_GAUX1_UNIT, gaux1Texture);
        bindTexture(TERRAIN_GAUX2_UNIT, gaux2Texture);
        bindTexture(TERRAIN_GAUX3_UNIT, gaux3Texture);
        bindTexture(TERRAIN_DEPTHTEX0_UNIT, depthtex0Texture);
        bindTexture(TERRAIN_DEPTHTEX1_UNIT, depthtex1Texture);
        bindTexture(TERRAIN_NOISETEX_UNIT, noiseTextureId);
        bindTexture(POST_COLORTEX8_UNIT, this.getGbuffersTextureOverride("colortex8", white));
        bindTexture(POST_COLORTEX9_UNIT, this.getGbuffersTextureOverride("colortex9", white));
        bindTexture(POST_COLORTEX10_UNIT, this.getGbuffersTextureOverride("colortex10", white));
        bindTexture(POST_COLORTEX11_UNIT, this.getGbuffersTextureOverride("colortex11", white));
        bindTexture(POST_COLORTEX12_UNIT, this.getGbuffersTextureOverride("colortex12", white));
        bindTexture(POST_COLORTEX13_UNIT, this.getGbuffersTextureOverride("colortex13", white));
        bindTexture(POST_COLORTEX14_UNIT, this.getGbuffersTextureOverride("colortex14", white));
        bindTexture(POST_COLORTEX15_UNIT, this.getGbuffersTextureOverride("colortex15", white));
        setActiveTextureUnit(0);
    }

    public void unbindTerrainInputTextures() {
        unbindTexture(POST_COLORTEX15_UNIT);
        unbindTexture(POST_COLORTEX14_UNIT);
        unbindTexture(POST_COLORTEX13_UNIT);
        unbindTexture(POST_COLORTEX12_UNIT);
        unbindTexture(POST_COLORTEX11_UNIT);
        unbindTexture(POST_COLORTEX10_UNIT);
        unbindTexture(POST_COLORTEX9_UNIT);
        unbindTexture(POST_COLORTEX8_UNIT);
        unbindTexture(TERRAIN_NOISETEX_UNIT);
        unbindTexture(TERRAIN_DEPTHTEX1_UNIT);
        unbindTexture(TERRAIN_DEPTHTEX0_UNIT);
        unbindTexture(TERRAIN_GAUX3_UNIT);
        unbindTexture(TERRAIN_GAUX2_UNIT);
        unbindTexture(TERRAIN_GAUX1_UNIT);
        unbindTexture(3);
        unbindTexture(2);
    }

    public void unbindTerrainShadowTextures() {
        unbindTexture(POST_SHADOW_COLOR1_UNIT);
        unbindTexture(TERRAIN_SHADOW_COLOR0_UNIT);
        unbindTexture(TERRAIN_SHADOW_TEX1_UNIT);
        unbindTexture(TERRAIN_SHADOW_TEX0_UNIT);
    }

    public void bindTerrainPassFramebuffer(TerrainRenderPass pass) {
        this.syncReloadState();

        if (!this.shouldUseExternalTerrainRedirect(pass)) {
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
                framebuffer.bindFramebuffer(true);
                this.copyCurrentFramebufferColorToTexture(this.worldTargets.getSourceColorTexture());
                debugCheckGlErrors("terrain.primeFramebuffer:" + pass.name());
            }
            int[] drawBuffers = this.resolveTerrainPassDrawBuffers(pass);
            this.worldTargets.bindWriteFramebuffer(framebuffer, drawBuffers, false);
            debugCheckGlErrors("terrain.bindFramebuffer:" + pass.name());
        }
    }

    public void unbindTerrainPassFramebuffer(TerrainRenderPass pass) {
        this.syncReloadState();

        if (!this.shouldUseExternalTerrainRedirect(pass)) {
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

        int[] drawBuffers = this.resolveTerrainPassDrawBuffers(pass);
        this.worldTargets.endWrite(framebuffer, drawBuffers, false);
        this.debugLogTerrainPassState("after-endWrite", pass, framebuffer);
        this.presentTerrainPassResult(framebuffer, drawBuffers);
        this.debugLogTerrainPassState("after-present", pass, framebuffer);
        debugCheckGlErrors("terrain.unbindFramebuffer:" + pass.name());
    }

    public void bindWorldGaux4Texture() {
        int textureId = this.worldTargets != null
                ? this.worldTargets.getSourceGaux4TextureOrDefault(this.whiteTexture)
                : this.whiteTexture != null ? this.whiteTexture : 0;
        textureId = this.getGbuffersTextureOverride("gaux4", textureId);
        bindTexture(WORLD_GAUX4_UNIT, textureId);
        setActiveTextureUnit(0);
    }

    public void unbindWorldGaux4Texture() {
        unbindTexture(WORLD_GAUX4_UNIT);
    }

    public void debugLogTerrainPassState(String stage, TerrainRenderPass pass) {
        if (!this.shouldEmitVerboseDebugFrame() || pass == null || !"translucent".equals(pass.name())) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();
        int mainTexture = framebuffer != null ? framebuffer.framebufferTexture : 0;
        int preSceneGaux1 = this.preSceneTargets != null ? this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX1) : 0;
        int postGaux1 = this.postTargets != null ? this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX1) : 0;
        int worldGaux4 = this.worldTargets != null ? this.worldTargets.getSourceGaux4Texture() : 0;

        this.debugLog(
                "Terrain translucent debug '{}' mainTex={}, terrainGaux1={}, terrainGaux2={}, depthtex0={}, depthtex1={}, preSceneGaux1={}, postGaux1={}, worldGaux4={}, preparedMask={}, preTransDepthCaptured={}, redirectEnabled={}",
                stage,
                mainTexture,
                this.terrainGaux1Texture,
                this.terrainGaux2Texture,
                this.terrainDepthTexture0,
                this.terrainDepthTexture1,
                preSceneGaux1,
                postGaux1,
                worldGaux4,
                this.terrainInputsPreparedMask,
                this.preTranslucentDepthCapturedThisFrame,
                this.shouldUseExternalTerrainRedirect(pass)
        );
        this.debugLogPipelineOverwriteRisk("terrain-translucent-" + stage, true);
        this.debugLogPipelineSnapshot("terrain.translucent." + stage, framebuffer);

        this.debugLogRenderState("terrain.translucent." + stage);

        if (framebuffer != null && framebuffer.framebufferTexture > 0) {
            this.width = Math.max(1, framebuffer.framebufferWidth);
            this.height = Math.max(1, framebuffer.framebufferHeight);
            this.debugLogTextureCenter("terrain.translucent." + stage + ".mainFramebuffer", framebuffer.framebufferTexture);
            this.debugLogTextureSamples("terrain.translucent." + stage + ".mainFramebuffer", framebuffer.framebufferTexture);
        }

        this.debugLogTextureCenter("terrain.translucent." + stage + ".terrainGaux1", this.terrainGaux1Texture != null ? this.terrainGaux1Texture : 0);
        this.debugLogTextureSamples("terrain.translucent." + stage + ".terrainGaux1", this.terrainGaux1Texture != null ? this.terrainGaux1Texture : 0);
        this.debugLogDepthTextureSample("terrain.translucent." + stage + ".depthtex0", this.terrainDepthTexture0 != null ? this.terrainDepthTexture0 : 0, this.width, this.height);
        this.debugLogDepthTextureSample("terrain.translucent." + stage + ".depthtex1", this.terrainDepthTexture1 != null ? this.terrainDepthTexture1 : 0, this.width, this.height);
        this.debugLogTextureCenter("terrain.translucent." + stage + ".preSceneGaux1", preSceneGaux1);
        this.debugLogTextureSamples("terrain.translucent." + stage + ".preSceneGaux1", preSceneGaux1);
        this.debugLogTextureCenter("terrain.translucent." + stage + ".postGaux1", postGaux1);
        this.debugLogTextureSamples("terrain.translucent." + stage + ".postGaux1", postGaux1);
    }

    public void bindWorldStageProgram(float partialTicks) {
        this.syncReloadState();
        this.debugLogFogState("bindWorldStageProgram", "entry");

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

        Matrix4f modelViewMatrix = this.scratchWorldStageModelViewMatrix;
        Matrix4f projectionMatrix = this.scratchWorldStageProjectionMatrix;
        Matrix4f projectionInverseMatrix = this.scratchWorldStageProjectionInverseMatrix;

        captureMatrix(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        projectionInverseMatrix.set(projectionMatrix).invert();

        boolean redirectFramebuffer = this.shouldRedirectWorldStageFramebuffer(program);

        if (this.activeWorldProgram != null) {
            if (this.activeWorldProgram == program) {
                program.program().bind();
                program.program().getInterface().setupState(this, modelViewMatrix, projectionMatrix, projectionInverseMatrix);
                this.bindWorldStageTextures();
                debugCheckGlErrors("world-stage.refreshProgram:" + program.name());
                return;
            }

            if (this.worldTargets != null && this.shouldRedirectWorldStageFramebuffer(this.activeWorldProgram)) {
                if (redirectFramebuffer) {
                    this.worldTargets.transitionWrite(this.activeWorldProgram.drawBuffers(), true);
                } else {
                    this.worldTargets.endWrite(framebuffer, this.activeWorldProgram.drawBuffers(), true);
                }
                if (this.shouldEmitVerboseDebugFrame()) {
                    this.debugLogTextureCenter("world-stage." + this.activeWorldProgram.name() + ".gaux4", this.worldTargets.getSourceGaux4Texture());
                }
            }

            this.unbindWorldStageTextures();
            this.activeWorldProgram.program().unbind();
            this.activeWorldProgram = null;
            debugCheckGlErrors("world-stage.switchProgram");
        }

        if (this.worldStageGlState == null) {
            this.worldStageGlState = WorldStageGlState.capture();
            debugCheckGlErrors("world-stage.capture:" + program.name());
        }
        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Binding world-stage program '{}' for {} with draw buffers {}", program.name(), this.currentStage, Arrays.toString(program.drawBuffers()));
        }
        boolean renderColorTex1ToMain = true;
        if (redirectFramebuffer && this.worldTargets != null) {
            this.worldTargets.bindWriteFramebuffer(framebuffer, program.drawBuffers(), renderColorTex1ToMain);
        }
        debugCheckGlErrors("world-stage.bindFramebuffer:" + program.name());
        this.activeWorldProgram = program;
        program.program().bind();
        debugCheckGlErrors("world-stage.bindProgram:" + program.name());
        program.program().getInterface().setupState(this, modelViewMatrix, projectionMatrix, projectionInverseMatrix);
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
            boolean renderColorTex1ToMain = true;
            if (this.worldTargets != null && this.shouldRedirectWorldStageFramebuffer(this.activeWorldProgram)) {
                this.worldTargets.endWrite(framebuffer, this.activeWorldProgram.drawBuffers(), renderColorTex1ToMain);
                if (this.shouldEmitVerboseDebugFrame()) {
                    this.debugLogTextureCenter("world-stage." + this.activeWorldProgram.name() + ".gaux4", this.worldTargets.getSourceGaux4Texture());
                }
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

    public void refreshEntityUniforms() {
        if (this.activeWorldProgram != null) {
            this.activeWorldProgram.program().getInterface().updateEntityState();
        }

        if (this.activeEntityWorldProgram != null) {
            this.activeEntityWorldProgram.program().getInterface().updateEntityState();
        }

        if (this.activeShadowEntityProgram != null) {
            this.activeShadowEntityProgram.getInterface().updateEntityState();
        }
    }

    public void beginPerEntityWorldProgram(float partialTicks) {
        this.syncReloadState();

        if (!ActiniumShaderPackManager.areShadersEnabled() || this.renderingShadowPass) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.getRenderViewEntity() == null) {
            return;
        }

        ActiniumWorldProgram program = this.getWorldStageProgram(ActiniumWorldStage.ENTITIES);
        if (program == null) {
            return;
        }

        Matrix4f modelViewMatrix = this.scratchWorldStageModelViewMatrix;
        Matrix4f projectionMatrix = this.scratchWorldStageProjectionMatrix;
        Matrix4f projectionInverseMatrix = this.scratchWorldStageProjectionInverseMatrix;

        captureMatrix(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        captureMatrix(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        projectionInverseMatrix.set(projectionMatrix).invert();

        this.activeEntityWorldProgram = program;
        program.program().bind();
        program.program().getInterface().setupState(this, modelViewMatrix, projectionMatrix, projectionInverseMatrix);
        this.bindWorldStageTextures();
        debugCheckGlErrors("world-stage.bindPerEntity:" + program.name());
    }

    public void endPerEntityWorldProgram() {
        if (this.activeEntityWorldProgram == null) {
            return;
        }

        this.unbindWorldStageTextures();
        this.activeEntityWorldProgram.program().unbind();
        debugCheckGlErrors("world-stage.unbindPerEntity:" + this.activeEntityWorldProgram.name());
        this.activeEntityWorldProgram = null;
    }

    private void executePostPrograms(List<ActiniumPostProgram> programs, float partialTicks) {
        if (this.postTargets != null) {
            this.executePostPrograms(programs, this.postTargets, partialTicks);
        }
    }

    private void executePostPrograms(List<ActiniumPostProgram> programs, ActiniumPostTargets targets, float partialTicks) {
        for (ActiniumPostProgram program : programs) {
            this.debugLogPostStageBoundary(program.name(), "before", targets);
            debugResetGlErrors("post." + program.name() + ".preMipmap");
            this.preparePostMipmappedInputs(targets, program.mipmappedBuffers());
            debugCheckGlErrors("post." + program.name() + ".prepareMipmaps");
            targets.bindWriteFramebuffer(program.drawBuffers());
            debugCheckGlErrors("post." + program.name() + ".bindWriteFramebuffer");
            GL11.glViewport(0, 0, this.width, this.height);
            debugCheckGlErrors("post." + program.name() + ".viewport");
            this.renderFullscreenProgram(program.program(), targets, partialTicks, program.name());
            this.applyPostProgramFlips(targets, program);
            this.debugLogPostStage(program.name());
            this.debugLogPostStageBoundary(program.name(), "after", targets);
        }
    }

    private void executeScenePrograms(List<ActiniumPostProgram> scenePrograms, float partialTicks) {
        this.executePostPrograms(scenePrograms, partialTicks);
    }

    private void renderFinalPass(Framebuffer mainFramebuffer, @Nullable GlProgram<ActiniumPostShaderInterface> program, float partialTicks) {
        this.debugLogPipelineSnapshot("final.before", mainFramebuffer);
        this.debugLogRenderState("final.prep.before");
        if (this.postTargets != null) {
            this.debugLogTextureSamples("final.input.colortex1", this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
        }

        debugResetGlErrors("final.prep.entry");
        this.prepareMainFramebufferForFinalPass(mainFramebuffer);
        debugCheckGlErrors("final.prep.mainFramebuffer");
        this.debugLogRenderState("final.prep.after");

        if (program != null) {
            this.preparePostMipmappedInputs(this.finalProgramMipmappedBuffers);
            debugCheckGlErrors("final.prepareMipmaps");
            this.renderFullscreenProgram(program, partialTicks, "final");
            this.applyFinalProgramFlips();
            this.debugLogFramebufferCenter("final.mainFramebuffer", mainFramebuffer.framebufferObject, GL30.GL_COLOR_ATTACHMENT0);
            this.debugLogFramebufferSamples("final.mainFramebuffer", mainFramebuffer.framebufferObject, GL30.GL_COLOR_ATTACHMENT0);
            this.finishMainFramebufferFinalPass();
            debugCheckGlErrors("final.finish");
            this.debugLogPipelineSnapshot("final.after", mainFramebuffer);
            return;
        }

        this.renderBlitFallback(mainFramebuffer, partialTicks);
        this.finishMainFramebufferFinalPass();
        debugCheckGlErrors("final.finish");
        this.debugLogPipelineSnapshot("final.after-blit", mainFramebuffer);
    }

    private void renderBlitFallback(Framebuffer mainFramebuffer, float partialTicks) {
        GlProgram<ActiniumPostShaderInterface> program = this.getBlitProgram();
        this.renderFullscreenProgram(program, partialTicks, null);
    }

    private void executePreSceneProgramsInPostPhase(Framebuffer mainFramebuffer, float partialTicks) {
        if (this.preSceneTargets == null) {
            return;
        }

        List<ActiniumPostProgram> preparePrograms = this.getPreparePrograms();
        List<ActiniumPostProgram> deferredPrograms = this.getDeferredPrograms();

        if (preparePrograms.isEmpty() && deferredPrograms.isEmpty()) {
            return;
        }

        if (!preparePrograms.isEmpty()) {
            this.preparePreSceneTargetsForPrepare(mainFramebuffer);
            this.applyExplicitPreFlips(this.preSceneTargets, "prepare_pre");
            this.debugLogPreSceneStageEntry("prepare");
            this.executePostPrograms(preparePrograms, this.preSceneTargets, partialTicks);
            this.prepareProgramsExecutedThisFrame = true;
        }

        if (!deferredPrograms.isEmpty()) {
            // Match Iris-style stage separation more closely: deferred should
            // operate on the current scene inputs, not on prepare's colortex1
            // result. Reusing prepare's color output here causes sky-prepass
            // data to replace the actual scene, which washes the world out.
            this.preparePreSceneTargetsForDeferred(mainFramebuffer, false);
            this.applyExplicitPreFlips(this.preSceneTargets, "deferred_pre");
            this.debugLogPreSceneStageEntry("deferred");
            this.executePostPrograms(deferredPrograms, this.preSceneTargets, partialTicks);
            this.deferredProgramsExecutedThisFrame = true;
        }
    }

    private boolean hasPreSceneResults() {
        return this.preSceneTargets != null && (this.prepareProgramsExecutedThisFrame || this.deferredProgramsExecutedThisFrame);
    }

    private int[] collectPreSceneOutputTargets() {
        Set<Integer> targets = new LinkedHashSet<>();

        if (this.prepareProgramsExecutedThisFrame) {
            for (ActiniumPostProgram program : this.getPreparePrograms()) {
                for (int drawBuffer : program.drawBuffers()) {
                    if (this.shouldMergePrepareTarget(drawBuffer)) {
                        targets.add(drawBuffer);
                    }
                }
            }
        }

        if (this.deferredProgramsExecutedThisFrame) {
            for (ActiniumPostProgram program : this.getDeferredPrograms()) {
                for (int drawBuffer : program.drawBuffers()) {
                    if (this.shouldMergeDeferredTarget(drawBuffer)) {
                        targets.add(drawBuffer);
                    }
                }
            }
        }

        return targets.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean shouldMergePrepareTarget(int drawBuffer) {
        // Prepare seeds the scene early; colortex1 is the scene bootstrap and
        // is overwritten by the later world/post scene copy.
        return drawBuffer != ActiniumPostTargets.TARGET_COLORTEX1;
    }

    private boolean shouldMergeDeferredTarget(int drawBuffer) {
        // Deferred runs before translucent terrain. Its auxiliary outputs should
        // feed later stages, but its scene-color targets must not replace the
        // fully rendered scene captured after water has already drawn.
        return drawBuffer != ActiniumPostTargets.TARGET_COLORTEX0
                && drawBuffer != ActiniumPostTargets.TARGET_COLORTEX1;
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

    private void debugLogPreSceneStageEntry(String stageName) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || !this.shouldEmitVerboseDebugFrame() || this.preSceneTargets == null) {
            return;
        }

        this.debugLogTextureCenter(stageName + ".entry.colortex1", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
        this.debugLogTextureCenter(stageName + ".entry.gaux4", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
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
        float[] fogColor = this.getFogColor();
        GlStateManager.clearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f);
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
        String stageLabel = textureStage != null ? textureStage : "blit";
        debugResetGlErrors("fullscreen." + stageLabel + ".entry");
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
        debugCheckGlErrors("fullscreen." + stageLabel + ".bindProgram");
        program.getInterface().setupState(this, targets, partialTicks, this.currentFrameDeltaSeconds, this.frameTimeCounterSeconds, this.frameCounter);
        debugCheckGlErrors("fullscreen." + stageLabel + ".setupState");
        this.activePostTextureStage = textureStage;
        this.bindPipelineTextures(targets);
        debugCheckGlErrors("fullscreen." + stageLabel + ".bindTextures");
        this.bindPipelineImageTargets(program.getInterface(), targets, stageLabel);
        debugCheckGlErrors("fullscreen." + stageLabel + ".bindImages");
        this.debugLogRenderState("fullscreen." + stageLabel + ".beforeDraw");

        if (this.fullscreenVertexArray != null) {
            GL30.glBindVertexArray(this.fullscreenVertexArray.handle());
        }
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
        debugCheckGlErrors("fullscreen." + stageLabel + ".draw");

        this.unbindPipelineImageTargets();
        debugCheckGlErrors("fullscreen." + stageLabel + ".unbindImages");
        this.unbindPipelineTextures();
        debugCheckGlErrors("fullscreen." + stageLabel + ".unbindTextures");
        this.activePostTextureStage = null;
        program.unbind();
        debugCheckGlErrors("fullscreen." + stageLabel + ".unbindProgram");

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
    }

    private void packSceneDepthIntoColortex1(ActiniumPostTargets targets, float partialTicks) {
        GlProgram<ActiniumPostShaderInterface> program = this.getSceneDepthPackProgram();
        targets.bindWriteFramebuffer(new int[]{ActiniumPostTargets.TARGET_COLORTEX1});
        GL11.glViewport(0, 0, this.width, this.height);
        this.renderFullscreenProgram(program, targets, partialTicks, "actinium_pack_scene_depth");
        targets.flipTarget(ActiniumPostTargets.TARGET_COLORTEX1);
    }

    private void mergeDeferredSkyIntoScene(ActiniumPostTargets targets, float partialTicks) {
        if (this.preSceneTargets == null) {
            return;
        }

        GlProgram<ActiniumPostShaderInterface> packProgram = this.getDeferredSkyPackProgram();
        targets.bindWriteFramebuffer(new int[]{ActiniumPostTargets.TARGET_COLORTEX8});
        GL11.glViewport(0, 0, this.width, this.height);
        this.renderFullscreenProgram(packProgram, this.preSceneTargets, partialTicks, "actinium_pack_deferred_sky");
        targets.flipTarget(ActiniumPostTargets.TARGET_COLORTEX8);

        GlProgram<ActiniumPostShaderInterface> mergeProgram = this.getDeferredSkyMergeProgram();
        targets.bindWriteFramebuffer(new int[]{ActiniumPostTargets.TARGET_COLORTEX1});
        GL11.glViewport(0, 0, this.width, this.height);
        this.renderFullscreenProgram(mergeProgram, targets, partialTicks, "actinium_merge_deferred_sky");
        targets.flipTarget(ActiniumPostTargets.TARGET_COLORTEX1);
    }

    private void mergeFirstPersonDepthIntoSceneDepth(ActiniumPostTargets targets, float partialTicks) {
        this.ensurePostCompositeScratchDepthTexture();

        if (this.postCompositeScratchDepthTexture == null || this.postCompositeScratchDepthTexture <= 0) {
            return;
        }

        GlProgram<ActiniumPostShaderInterface> program = this.getFirstPersonDepthMergeProgram();
        this.renderDepthOnlyFullscreenProgram(program, targets, partialTicks, this.postCompositeScratchDepthTexture, "actinium_merge_first_person_depth");
        targets.copyDepthTextureToSlot(this.postCompositeScratchDepthTexture, 0);
    }

    private void renderDepthOnlyFullscreenProgram(
            GlProgram<ActiniumPostShaderInterface> program,
            ActiniumPostTargets targets,
            float partialTicks,
            int destinationDepthTexture,
            String stageLabel
    ) {
        this.ensureScratchCopyFramebuffers();
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.scratchCopyDrawFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, destinationDepthTexture, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);

            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Incomplete Actinium depth merge framebuffer: " + status);
            }

            debugResetGlErrors("depth-only." + stageLabel + ".entry");
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.disableFog();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_ALWAYS);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(false, false, false, false);
            GL11.glViewport(0, 0, this.width, this.height);

            program.bind();
            program.getInterface().setupState(this, targets, partialTicks, this.currentFrameDeltaSeconds, this.frameTimeCounterSeconds, this.frameCounter);
            this.activePostTextureStage = stageLabel;
            this.bindPipelineTextures(targets);

            if (this.fullscreenVertexArray != null) {
                GL30.glBindVertexArray(this.fullscreenVertexArray.handle());
            }

            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            GL30.glBindVertexArray(0);
        } finally {
            this.unbindPipelineTextures();
            this.activePostTextureStage = null;
            program.unbind();
            GL11.glColorMask(true, true, true, true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
            GL11.glReadBuffer(previousReadBuffer);
        }
    }

    private void bindPipelineTextures(ActiniumPostTargets targets) {
        if (targets != null) {
            this.bindPipelineTextureAliases(0, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX0), "colortex0", "gcolor");
            this.bindPipelineTextureAliases(1, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1), "colortex1", "gdepth");
            this.bindPipelineTextureAliases(2, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX2), "colortex2", "gnormal");
            this.bindPipelineTextureAliases(3, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX3), "colortex3", "composite");
            this.bindPipelineTextureAliases(POST_SHADOW_TEX0_UNIT, this.shadowTargets != null ? this.shadowTargets.getDepthTexture(0) : this.whiteTexture, "shadowtex0", "watershadow", "shadow");
            this.bindPipelineTextureAliases(POST_SHADOW_TEX1_UNIT, this.shadowTargets != null ? this.shadowTargets.getDepthTexture(1) : this.whiteTexture, "shadowtex1");
            bindTexture(POST_DEPTHTEX0_UNIT, targets.getDepthTexture(0));
            this.bindPipelineTextureAliases(POST_GAUX1_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX1), "gaux1", "colortex4");
            this.bindPipelineTextureAliases(POST_GAUX2_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX2), "gaux2", "colortex5");
            this.bindPipelineTextureAliases(POST_GAUX3_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX3), "gaux3", "colortex6");
            this.bindPipelineTextureAliases(POST_GAUX4_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4), "gaux4", "colortex7");
            bindTexture(POST_DEPTHTEX1_UNIT, targets.getDepthTexture(1));
            bindTexture(POST_DEPTHTEX2_UNIT, targets.getDepthTexture(2));
            this.bindPipelineTextureAliases(POST_SHADOW_COLOR0_UNIT, this.shadowTargets != null ? this.shadowTargets.getColorTexture(0) : this.whiteTexture, "shadowcolor0", "shadowcolor");
            this.bindPipelineTextureAliases(POST_SHADOW_COLOR1_UNIT, this.shadowTargets != null ? this.shadowTargets.getColorTexture(1) : this.whiteTexture, "shadowcolor1");
            this.bindPipelineTextureAliases(POST_NOISETEX_UNIT, this.noiseTexture, "noisetex");
            this.bindPipelineTextureAliases(POST_COLORTEX8_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX8), "colortex8");
            this.bindPipelineTextureAliases(POST_COLORTEX9_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX9), "colortex9");
            this.bindPipelineTextureAliases(POST_COLORTEX10_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX10), "colortex10");
            this.bindPipelineTextureAliases(POST_COLORTEX11_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX11), "colortex11");
            this.bindPipelineTextureAliases(POST_COLORTEX12_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX12), "colortex12");
            this.bindPipelineTextureAliases(POST_COLORTEX13_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX13), "colortex13");
            this.bindPipelineTextureAliases(POST_COLORTEX14_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX14), "colortex14");
            this.bindPipelineTextureAliases(POST_COLORTEX15_UNIT, targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX15), "colortex15");
        }
    }

    private void bindPipelineImageTargets(ActiniumPostShaderInterface shaderInterface, ActiniumPostTargets targets, String stageLabel) {
        if (!LWJGL.isOpenGLVersionSupported(4, 2)) {
            return;
        }

        int boundCount = 0;

        for (int imageIndex = 0; imageIndex <= 5; imageIndex++) {
            if (!shaderInterface.hasColorImage(imageIndex)) {
                continue;
            }

            int targetIndex = switch (imageIndex) {
                case 0 -> ActiniumPostTargets.TARGET_COLORTEX0;
                case 1 -> ActiniumPostTargets.TARGET_COLORTEX1;
                case 2 -> ActiniumPostTargets.TARGET_COLORTEX2;
                case 3 -> ActiniumPostTargets.TARGET_COLORTEX3;
                case 4 -> ActiniumPostTargets.TARGET_GAUX1;
                case 5 -> ActiniumPostTargets.TARGET_GAUX2;
                default -> -1;
            };

            int textureId = targets.getSourceTexture(targetIndex);
            int format = targets.getInternalFormat(targetIndex);
            this.boundPostImageUnits[imageIndex] = textureId;
            LWJGL.glBindImageTexture(imageIndex, textureId, 0, false, 0, GL42.GL_READ_WRITE, format);
            boundCount++;
        }

        if (shaderInterface.hasShadowColorImage(0)) {
            int shadowTexture = this.shadowTargets != null ? this.shadowTargets.getColorTexture(0) : 0;
            this.boundPostImageUnits[6] = shadowTexture;
            LWJGL.glBindImageTexture(6, shadowTexture, 0, false, 0, GL42.GL_READ_WRITE, GL11.GL_RGBA8);
            boundCount++;
        }

        if (shaderInterface.hasShadowColorImage(1)) {
            int shadowTexture = this.shadowTargets != null ? this.shadowTargets.getColorTexture(1) : 0;
            this.boundPostImageUnits[7] = shadowTexture;
            LWJGL.glBindImageTexture(7, shadowTexture, 0, false, 0, GL42.GL_READ_WRITE, GL11.GL_RGBA8);
            boundCount++;
        }

        if (this.shouldEmitVerboseDebugFrame()) {
            if (boundCount > 0) {
                this.debugLog(
                        "Bound {} post image target(s) for '{}' with colorimg0-5/shadowcolorimg0-1 declaration filtering",
                        boundCount,
                        stageLabel
                );
            } else {
                this.debugLog("No post image uniforms declared for '{}'; skipping image bindings", stageLabel);
            }
        }
    }

    private void unbindPipelineImageTargets() {
        if (!LWJGL.isOpenGLVersionSupported(4, 2)) {
            return;
        }

        for (int unit = 0; unit < this.boundPostImageUnits.length; unit++) {
            LWJGL.glBindImageTexture(unit, 0, 0, false, 0, GL42.GL_READ_WRITE, GL11.GL_RGBA8);
            this.boundPostImageUnits[unit] = 0;
        }
    }

    private void bindPipelineTexture(String samplerName, int unit, @Nullable Integer fallbackTexture) {
        bindTexture(unit, this.getPostTextureOverride(samplerName, fallbackTexture));
    }

    private void bindPipelineTextureAliases(int unit, @Nullable Integer fallbackTexture, String... samplerNames) {
        for (String samplerName : samplerNames) {
            int overrideTexture = this.getPostTextureOverride(samplerName, null);
            if (overrideTexture > 0) {
                bindTexture(unit, overrideTexture);
                return;
            }
        }

        bindTexture(unit, fallbackTexture != null ? fallbackTexture : 0);
    }

    private void bindWorldStageTextures() {
        int white = this.whiteTexture != null ? this.whiteTexture : 0;
        int gaux4Texture = this.worldTargets != null
                ? this.worldTargets.getSourceGaux4TextureOrDefault(this.whiteTexture)
                : white;
        int shadowTex0 = this.shadowTargets != null ? this.shadowTargets.getDepthTexture(0) : white;
        int shadowTex1 = this.shadowTargets != null ? this.shadowTargets.getDepthTexture(1) : white;
        int shadowColor0 = this.shadowTargets != null ? this.shadowTargets.getColorTexture(0) : white;
        int shadowColor1 = this.shadowTargets != null ? this.shadowTargets.getColorTexture(1) : white;

        this.bindWorldStageTextureAliases(2, white, "normals");
        this.bindWorldStageTextureAliases(3, white, "specular");
        this.bindWorldStageTextureAliases(TERRAIN_GAUX1_UNIT, white, "gaux1", "colortex4");
        this.bindWorldStageTextureAliases(TERRAIN_GAUX2_UNIT, white, "gaux2", "colortex5");
        this.bindWorldStageTextureAliases(WORLD_GAUX4_UNIT, gaux4Texture, "gaux4", "colortex7");
        this.bindWorldStageTextureAliases(POST_GAUX3_UNIT, white, "gaux3", "colortex6");
        this.bindWorldStageTextureAliases(TERRAIN_NOISETEX_UNIT, this.noiseTexture, "noisetex");
        this.bindWorldStageTextureAliases(TERRAIN_SHADOW_TEX0_UNIT, shadowTex0, "shadowtex0", "watershadow", "shadow");
        this.bindWorldStageTextureAliases(TERRAIN_SHADOW_TEX1_UNIT, shadowTex1, "shadowtex1");
        this.bindWorldStageTextureAliases(TERRAIN_SHADOW_COLOR0_UNIT, shadowColor0, "shadowcolor0", "shadowcolor");
        this.bindWorldStageTextureAliases(POST_SHADOW_COLOR1_UNIT, shadowColor1, "shadowcolor1");
        this.bindWorldStageTextureAliases(POST_COLORTEX8_UNIT, white, "colortex8");
        this.bindWorldStageTextureAliases(POST_COLORTEX9_UNIT, white, "colortex9");
        this.bindWorldStageTextureAliases(POST_COLORTEX10_UNIT, white, "colortex10");
        this.bindWorldStageTextureAliases(POST_COLORTEX11_UNIT, white, "colortex11");
        this.bindWorldStageTextureAliases(POST_COLORTEX12_UNIT, white, "colortex12");
        this.bindWorldStageTextureAliases(POST_COLORTEX13_UNIT, white, "colortex13");
        this.bindWorldStageTextureAliases(POST_COLORTEX14_UNIT, white, "colortex14");
        this.bindWorldStageTextureAliases(POST_COLORTEX15_UNIT, white, "colortex15");
        setActiveTextureUnit(0);
    }

    private void bindWorldStageTextureAliases(int unit, @Nullable Integer fallbackTexture, String... samplerNames) {
        for (String samplerName : samplerNames) {
            int overrideTexture = this.getGbuffersTextureOverride(samplerName, null);
            if (overrideTexture > 0) {
                bindTexture(unit, overrideTexture);
                return;
            }
        }

        bindTexture(unit, fallbackTexture != null ? fallbackTexture : 0);
    }

    private void unbindPipelineTextures() {
        for (int unit = TRACKED_TEXTURE_UNITS - 1; unit >= 0; unit--) {
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
            debugResetGlErrors("post.mipmaps.target" + targetIndex + ".entry");
            this.generateTextureMipmaps(targets.getSourceTexture(targetIndex), false);
            debugCheckGlErrors("post.mipmaps.target" + targetIndex + ".generate");
        }
    }

    private void generateTextureMipmaps(int textureId, boolean integerTexture) {
        if (textureId <= 0) {
            return;
        }

        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        setActiveTextureUnit(0);
        bindTextureDirect(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, integerTexture ? GL11.GL_NEAREST_MIPMAP_NEAREST : GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        bindTextureDirect(previousBinding);
        setActiveTextureEnum(previousActiveTexture);
    }

    private void debugLogPostStage(String programName) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || this.postTargets == null) {
            return;
        }

        int colortex1 = this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1);
        int gaux3 = this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX3);
        int gaux4 = this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4);
        this.debugLogTextureCenter(programName + ".colortex1", colortex1);
        this.debugLogTextureCenter(programName + ".gaux3", gaux3);
        this.debugLogTextureCenter(programName + ".gaux4", gaux4);
        this.debugLogTextureSamples(programName + ".colortex1", colortex1);
    }

    private void debugLogPostStageBoundary(String programName, String boundary, ActiniumPostTargets targets) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || !this.shouldEmitVerboseDebugFrame() || targets == null) {
            return;
        }

        this.debugLog("Post stage '{}' {} draw snapshot", programName, boundary);
        this.debugLogTextureCenter("post." + programName + "." + boundary + ".colortex0", targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX0));
        this.debugLogTextureCenter("post." + programName + "." + boundary + ".colortex1", targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
        this.debugLogTextureCenter("post." + programName + "." + boundary + ".gaux4", targets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
        this.debugLogTextureSamples("post." + programName + "." + boundary + ".colortex1", targets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
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

        if (relativePath == null) {
            for (String alias : getSamplerAliases(samplerName)) {
                relativePath = properties.getStageTexturePath(stageName, alias);
                if (relativePath != null) {
                    break;
                }
            }
        }

        if (relativePath == null && "composite".equals(stageName)) {
            relativePath = properties.getStageTexturePath("final", samplerName);
        }

        if (relativePath == null && "noisetex".equalsIgnoreCase(samplerName)) {
            relativePath = properties.getStageTexturePath("gbuffers", samplerName);
        }

        if (relativePath == null && "noisetex".equalsIgnoreCase(samplerName)) {
            relativePath = properties.getNoiseTexturePath();
        }

        if (relativePath == null) {
            return fallbackTexture != null ? fallbackTexture : 0;
        }

        final String resolvedPath = relativePath;
        Integer textureId = this.packTextureCache.computeIfAbsent(
                stageName + ":" + samplerName + ":" + resolvedPath,
                key -> this.loadPackTexture(stageName, samplerName, resolvedPath)
        );
        return textureId != null && textureId > 0 ? textureId : fallbackTexture != null ? fallbackTexture : 0;
    }

    private static String[] getSamplerAliases(String samplerName) {
        return switch (samplerName) {
            case "gaux1" -> new String[]{"colortex4"};
            case "gaux2" -> new String[]{"colortex5"};
            case "gaux3" -> new String[]{"colortex6"};
            case "gaux4" -> new String[]{"colortex7"};
            case "colortex4" -> new String[]{"gaux1"};
            case "colortex5" -> new String[]{"gaux2"};
            case "colortex6" -> new String[]{"gaux3"};
            case "colortex7" -> new String[]{"gaux4"};
            case "shadow" -> new String[]{"shadowtex0", "watershadow"};
            case "watershadow" -> new String[]{"shadowtex0", "shadow"};
            case "shadowtex0" -> new String[]{"shadow", "watershadow"};
            case "shadowcolor" -> new String[]{"shadowcolor0"};
            case "shadowcolor0" -> new String[]{"shadowcolor"};
            default -> new String[0];
        };
    }

    private int loadPackTexture(String stageName, String samplerName, String relativePath) {
        ActiniumShaderPackResources resources = ActiniumShaderPackManager.getActivePackResources();
        if (resources == null) {
            return 0;
        }

        try (InputStream stream = resources.openResource(relativePath)) {
            if (stream == null) {
                this.debugLog("Shader pack texture '{}' is missing", relativePath);
                return 0;
            }

            return uploadPackTexture(TextureUtil.readBufferedImage(stream), this.shouldRepeatPackTexture(stageName, samplerName, relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader pack texture " + relativePath, e);
        }
    }

    private boolean shouldRepeatPackTexture(String stageName, String samplerName, String relativePath) {
        String normalizedSampler = samplerName.toLowerCase(Locale.ROOT);
        if ("noisetex".equals(normalizedSampler) || "gaux2".equals(normalizedSampler)) {
            return true;
        }

        String normalizedPath = relativePath.toLowerCase(Locale.ROOT);
        return normalizedPath.contains("noise") || normalizedPath.contains("cloud");
    }

    private static int uploadPackTexture(java.awt.image.BufferedImage image, boolean repeat) {
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
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, repeat ? GL11.GL_REPEAT : GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, repeat ? GL11.GL_REPEAT : GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private void updateCenterDepthSmooth() {
        if (this.postTargets == null) {
            return;
        }

        this.updateCenterDepthSmooth(this.postTargets.getDepthTexture(0));
    }

    private void updateCenterDepthSmooth(int depthTexture) {
        if (depthTexture <= 0) {
            return;
        }

        this.updateCenterDepthSmooth(this.readDepthTextureCenter(depthTexture));
    }

    private void updateCenterDepthSmooth(float currentDepth) {
        if (!(currentDepth > 0.0f) || Float.isNaN(currentDepth)) {
            return;
        }

        if (this.centerDepthSmooth == 0.0f) {
            this.centerDepthSmooth = currentDepth;
            return;
        }

        float blend = 0.15f;
        this.centerDepthSmooth += (currentDepth - this.centerDepthSmooth) * blend;
    }

    private void captureCenterDepthFromMainFramebuffer(String label) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer mainFramebuffer = minecraft.getFramebuffer();

        if (mainFramebuffer == null || this.width <= 0 || this.height <= 0) {
            return;
        }

        float focusDepth = this.readFramebufferFocusDepth(mainFramebuffer, this.width, this.height);
        this.updateCenterDepthSmooth(focusDepth);
        this.centerDepthCapturedThisFrame = true;

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog("Focus depth '{}' sampled={}", label, focusDepth);
        }
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

    private float readFramebufferFocusDepth(Framebuffer framebuffer, int width, int height) {
        int sampleDiameter = FOCUS_DEPTH_SAMPLE_RADIUS * 2 + 1;
        int sampleX = Math.max(0, Math.min(width - sampleDiameter, width / 2 - FOCUS_DEPTH_SAMPLE_RADIUS));
        int sampleY = Math.max(0, Math.min(height - sampleDiameter, height / 2 - FOCUS_DEPTH_SAMPLE_RADIUS));
        int sampleCount = sampleDiameter * sampleDiameter;
        FloatBuffer samples = this.scratchFocusDepthBuffer;
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        try {
            framebuffer.bindFramebuffer(true);
            samples.clear();
            GL11.glReadPixels(sampleX, sampleY, sampleDiameter, sampleDiameter, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, samples);

            float centerDepth = 1.0f;
            float closestDepth = 1.0f;

            for (int row = 0; row < sampleDiameter; row++) {
                for (int column = 0; column < sampleDiameter; column++) {
                    float depth = samples.get();

                    if (row == FOCUS_DEPTH_SAMPLE_RADIUS && column == FOCUS_DEPTH_SAMPLE_RADIUS) {
                        centerDepth = depth;
                    }

                    if (depth < closestDepth) {
                        closestDepth = depth;
                    }
                }
            }

            float focusDepth = closestDepth < FOCUS_DEPTH_SKY_THRESHOLD ? closestDepth : centerDepth;

            if (this.shouldEmitVerboseDebugFrame()) {
                this.debugLog(
                        "Focus depth neighborhood center={} closest={} chosen={}",
                        centerDepth,
                        closestDepth,
                        focusDepth
                );
            }

            return focusDepth;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
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

    private void debugLogTextureSamples(String label, int textureId, int width, int height) {
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
            this.debugReadSamplePoints(label, width, height);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    private void debugLogDepthTextureSample(String label, int textureId, int width, int height) {
        if (!this.shouldEmitVerboseDebugFrame() || textureId <= 0 || width <= 0 || height <= 0) {
            return;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int framebuffer = GL30.glGenFramebuffers();
        FloatBuffer pixel = BufferUtils.createFloatBuffer(1);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, textureId, 0);
            GL11.glReadBuffer(GL11.GL_NONE);
            GL11.glReadPixels(Math.max(0, width / 2), Math.max(0, height / 2), 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, pixel);
            this.debugLog("Post debug '{}' center depth={}", label, pixel.get(0));
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
        IntBuffer colorMask = BufferUtils.createIntBuffer(16);
        invokeGlGetInteger(GL11.GL_COLOR_WRITEMASK, colorMask);

        this.debugLog(
                "Render state '{}' fb={}, draw={}, read={}, program={}, vao={}, viewport=[{}, {}, {}, {}], scissorEnabled={}, scissor=[{}, {}, {}, {}], blend={}, depth={}, depthMask={}, alpha={}, cull={}, colorMask=[{}, {}, {}, {}]",
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
                GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                GL11.glIsEnabled(GL11.GL_CULL_FACE),
                colorMask.get(0),
                colorMask.get(1),
                colorMask.get(2),
                colorMask.get(3)
        );
    }

    private void debugLogMainFramebufferSnapshot(String label) {
        if (!this.shouldEmitVerboseDebugFrame()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();
        if (framebuffer == null || framebuffer.framebufferTexture <= 0) {
            return;
        }

        this.width = Math.max(1, framebuffer.framebufferWidth);
        this.height = Math.max(1, framebuffer.framebufferHeight);
        this.debugLogTextureCenter(label + ".mainFramebuffer", framebuffer.framebufferTexture);
        this.debugLogTextureSamples(label + ".mainFramebuffer", framebuffer.framebufferTexture);
    }

    private void debugLogPipelineSnapshot(String label, @Nullable Framebuffer mainFramebuffer) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || !this.shouldEmitVerboseDebugFrame()) {
            return;
        }

        if (mainFramebuffer != null && mainFramebuffer.framebufferTexture > 0) {
            this.width = Math.max(1, mainFramebuffer.framebufferWidth);
            this.height = Math.max(1, mainFramebuffer.framebufferHeight);
        }

        this.debugLog(
                "Pipeline snapshot '{}' flags: terrainRedirect={}, worldTargetsPrepared={}, prepareRan={}, deferredRan={}, preTransDepthCaptured={}, sceneProgramsResolved={}, finalProgramResolved={}",
                label,
                this.shouldUseExternalTranslucentTerrainRedirect(),
                this.worldTargetsPrepared,
                this.prepareProgramsExecutedThisFrame,
                this.deferredProgramsExecutedThisFrame,
                this.preTranslucentDepthCapturedThisFrame,
                this.sceneProgramsResolved,
                this.finalProgramResolved
        );

        if (mainFramebuffer != null && mainFramebuffer.framebufferTexture > 0) {
            this.debugLogTextureCenter(label + ".mainFramebuffer", mainFramebuffer.framebufferTexture);
            this.debugLogTextureSamples(label + ".mainFramebuffer", mainFramebuffer.framebufferTexture);
        }

        if (this.worldTargets != null) {
            this.debugLogTextureCenter(label + ".world.colortex1", this.worldTargets.getSourceColorTexture());
            this.debugLogTextureCenter(label + ".world.gaux4", this.worldTargets.getSourceGaux4Texture());
        }

        if (this.preSceneTargets != null) {
            this.debugLogTextureCenter(label + ".pre.colortex1", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
            this.debugLogTextureCenter(label + ".pre.gaux4", this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
            this.debugLogDepthTextureSample(label + ".pre.depth0", this.preSceneTargets.getDepthTexture(0), this.width, this.height);
            this.debugLogDepthTextureSample(label + ".pre.depth1", this.preSceneTargets.getDepthTexture(1), this.width, this.height);
        }

        if (this.postTargets != null) {
            this.debugLogTextureCenter(label + ".post.colortex1", this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_COLORTEX1));
            this.debugLogTextureCenter(label + ".post.gaux4", this.postTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4));
            this.debugLogDepthTextureSample(label + ".post.depth0", this.postTargets.getDepthTexture(0), this.width, this.height);
            this.debugLogDepthTextureSample(label + ".post.depth1", this.postTargets.getDepthTexture(1), this.width, this.height);
        }
    }

    private void debugLogPipelineOverwriteRisk(String label, boolean postPipelineActive) {
        if (!ActiniumShaderPackManager.isDebugEnabled() || !this.shouldEmitVerboseDebugFrame() || !postPipelineActive) {
            return;
        }

        if (!this.shouldUseExternalTranslucentTerrainRedirect()) {
            this.debugLog(
                    "Pipeline risk '{}': external scene/final post stages are active while terrain redirect is disabled; translucent terrain drawn to the main framebuffer can be overwritten by later fullscreen passes",
                    label
            );
        }
    }

    private boolean shouldUseExternalTerrainRedirect(TerrainRenderPass pass) {
        if (pass == null) {
            return false;
        }

        if (ENABLE_EXTERNAL_TERRAIN_REDIRECT) {
            return true;
        }

        return this.shouldUseExternalTranslucentTerrainRedirect() && pass.isReverseOrder();
    }

    private boolean shouldUseExternalTranslucentTerrainRedirect() {
        return ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT;
    }

    private boolean shouldEmitVerboseDebugFrame() {
        return ActiniumShaderPackManager.isDebugEnabled();
    }

    private void debugLogFrameStageSummary(String label) {
        if (!ActiniumShaderPackManager.isDebugEnabled()) {
            return;
        }

        this.debugLog(
                "{} frame={} stage={} shadersEnabled={} shadowProgram={} skyProgram={} particleProgram={} weatherProgram={} postProgram={} deferredPost={} taaActive={} taaOffset=[{}, {}] centerDepthSmooth={} viewport={}x{} camera=[{}, {}, {}]",
                label,
                this.frameCounter,
                this.currentStage,
                ActiniumShaderPackManager.areShadersEnabled(),
                this.shadowProgramAvailable,
                this.skyProgramAvailable,
                this.particleProgramAvailable,
                this.weatherProgramAvailable,
                this.postProgramAvailable,
                this.postPipelineDeferredForFirstPersonThisFrame,
                this.isTemporalAntiAliasingActive(),
                this.getTaaOffsetX(),
                this.getTaaOffsetY(),
                this.centerDepthSmooth,
                this.width,
                this.height,
                this.worldCameraPosition.x,
                this.worldCameraPosition.y,
                this.worldCameraPosition.z
        );
    }

    private void debugLogProjectionSnapshot(String label, Matrix4fc matrix) {
        if (!ActiniumShaderPackManager.isDebugEnabled()) {
            return;
        }

        this.debugLog(
                "{} m00={} m11={} m20={} m21={} m22={} m23={} m32={} m33={}",
                label,
                matrix.m00(),
                matrix.m11(),
                matrix.m20(),
                matrix.m21(),
                matrix.m22(),
                matrix.m23(),
                matrix.m32(),
                matrix.m33()
        );
    }

    private void unbindWorldStageTextures() {
        for (int unit : new int[]{
                POST_COLORTEX15_UNIT,
                POST_COLORTEX14_UNIT,
                POST_COLORTEX13_UNIT,
                POST_COLORTEX12_UNIT,
                POST_COLORTEX11_UNIT,
                POST_COLORTEX10_UNIT,
                POST_COLORTEX9_UNIT,
                POST_COLORTEX8_UNIT,
                POST_SHADOW_COLOR1_UNIT,
                TERRAIN_SHADOW_COLOR0_UNIT,
                TERRAIN_SHADOW_TEX1_UNIT,
                TERRAIN_SHADOW_TEX0_UNIT,
                TERRAIN_NOISETEX_UNIT,
                POST_GAUX3_UNIT,
                WORLD_GAUX4_UNIT,
                TERRAIN_GAUX2_UNIT,
                TERRAIN_GAUX1_UNIT,
                3,
                2
        }) {
            unbindTexture(unit);
        }
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
            ProgramMetadata metadata = ProgramMetadata.parse(this.resolveShaderForMetadata(fragmentSource));
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
        ProgramMetadata metadata = ProgramMetadata.parse(this.resolveShaderForMetadata(fragmentSource));
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

    private GlProgram<ActiniumPostShaderInterface> getSceneDepthPackProgram() {
        if (this.sceneDepthPackProgram != null) {
            return this.sceneDepthPackProgram;
        }

        String fragmentSource = String.join("\n",
                "#version 330 core",
                "uniform sampler2D colortex1;",
                "uniform sampler2D depthtex0;",
                "in vec2 texcoord;",
                "out vec4 fragColor0;",
                "void main() {",
                "    vec4 sceneColor = texture(colortex1, texcoord);",
                "    float sceneDepth = texture(depthtex0, texcoord).r;",
                "    fragColor0 = vec4(sceneColor.rgb, sceneDepth);",
                "}",
                ""
        );

        this.sceneDepthPackProgram = this.createProgram("actinium_internal_pack_scene_depth", defaultVertexSource(), fragmentSource, new int[]{0});
        return this.sceneDepthPackProgram;
    }

    private GlProgram<ActiniumPostShaderInterface> getFirstPersonDepthMergeProgram() {
        if (this.firstPersonDepthMergeProgram != null) {
            return this.firstPersonDepthMergeProgram;
        }

        String fragmentSource = String.join("\n",
                "#version 330 core",
                "uniform sampler2D depthtex0;",
                "uniform sampler2D depthtex2;",
                "in vec2 texcoord;",
                "void main() {",
                "    float handDepth = texture(depthtex0, texcoord).r;",
                "    float worldDepth = texture(depthtex2, texcoord).r;",
                "    gl_FragDepth = min(handDepth, worldDepth);",
                "}",
                ""
        );

        this.firstPersonDepthMergeProgram = this.createProgram("actinium_internal_merge_first_person_depth", defaultVertexSource(), fragmentSource, new int[0]);
        return this.firstPersonDepthMergeProgram;
    }

    private GlProgram<ActiniumPostShaderInterface> getDeferredSkyPackProgram() {
        if (this.deferredSkyPackProgram != null) {
            return this.deferredSkyPackProgram;
        }

        String fragmentSource = String.join("\n",
                "#version 330 core",
                "uniform sampler2D colortex1;",
                "uniform sampler2D depthtex0;",
                "in vec2 texcoord;",
                "out vec4 fragColor0;",
                "void main() {",
                "    vec3 deferredScene = texture(colortex1, texcoord).rgb;",
                "    float sceneDepth = texture(depthtex0, texcoord).r;",
                "    float skyMask = step(" + DEFERRED_SKY_DEPTH_THRESHOLD + ", sceneDepth);",
                "    fragColor0 = vec4(deferredScene, skyMask);",
                "}",
                ""
        );

        this.deferredSkyPackProgram = this.createProgram("actinium_internal_pack_deferred_sky", defaultVertexSource(), fragmentSource, new int[]{0});
        return this.deferredSkyPackProgram;
    }

    private GlProgram<ActiniumPostShaderInterface> getDeferredSkyMergeProgram() {
        if (this.deferredSkyMergeProgram != null) {
            return this.deferredSkyMergeProgram;
        }

        String fragmentSource = String.join("\n",
                "#version 330 core",
                "uniform sampler2D colortex1;",
                "uniform sampler2D colortex8;",
                "in vec2 texcoord;",
                "out vec4 fragColor0;",
                "void main() {",
                "    vec4 currentScene = texture(colortex1, texcoord);",
                "    vec4 deferredSky = texture(colortex8, texcoord);",
                "    vec3 mergedColor = mix(currentScene.rgb, deferredSky.rgb, deferredSky.a);",
                "    fragColor0 = vec4(mergedColor, currentScene.a);",
                "}",
                ""
        );

        this.deferredSkyMergeProgram = this.createProgram("actinium_internal_merge_deferred_sky", defaultVertexSource(), fragmentSource, new int[]{0});
        return this.deferredSkyMergeProgram;
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

    private String resolveShaderForMetadata(String source) {
        return ShaderParser.parseShader(source, this::resolveShaderSource, ShaderConstants.EMPTY);
    }

    private boolean shouldUseExternalScenePrograms() {
        return ENABLE_EXTERNAL_SCENE_PIPELINE
                && ActiniumShaderPackManager.areShadersEnabled()
                && ActiniumShaderPackManager.getActivePackResources() != null;
    }

    private boolean shouldUseExternalWorldPrograms() {
        return ActiniumShaderPackManager.areShadersEnabled()
                && ActiniumShaderPackManager.getActivePackResources() != null;
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
                ActiniumPostTargets.ColorFormat.RGBA16F,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8,
                ActiniumPostTargets.ColorFormat.RGBA8
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

        if (this.isShaderOptionEnabled("DOF")) {
            formats[ActiniumPostTargets.TARGET_COLORTEX1] = ActiniumPostTargets.ColorFormat.RGBA16F;
            formats[ActiniumPostTargets.TARGET_COLORTEX3] = ActiniumPostTargets.ColorFormat.RGBA16F;

            if (ActiniumShaderPackManager.isDebugEnabled()) {
                this.debugLog("Forced DOF post target formats: colortex1=RGBA16F, colortex3=RGBA16F");
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
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
                new ActiniumPostTargets.TargetSettings(true, 0.0f, 0.0f, 0.0f, 0.0f),
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

    private boolean isShaderOptionEnabled(String optionName) {
        String value = ActiniumShaderPackManager.getEffectiveOptionValue(optionName);

        if (value == null) {
            return false;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "on", "yes" -> true;
            default -> false;
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
        this.transitionPhase(ActiniumPipelinePhase.NONE);
        this.shadowProgramAvailable = hasAnyStageProgram("shadow");
        this.skyProgramAvailable = hasAnyStageProgram(SKY_PROGRAMS);
        this.particleProgramAvailable = hasAnyStageProgram(PARTICLE_PROGRAMS);
        this.weatherProgramAvailable = hasAnyStageProgram(WEATHER_PROGRAMS);
        this.postProgramAvailable = this.hasUsablePostProgram();
        this.loggedCapabilities = false;
        this.debugLog(
                "Observed shader reload version {}: shadow={}, sky={}, particles={}, weather={}, post={}",
                reloadVersion,
                this.shadowProgramAvailable,
                this.skyProgramAvailable,
                this.particleProgramAvailable,
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

        if (this.particleProgramAvailable) {
            capabilities.add("particles");
        }

        if (this.weatherProgramAvailable) {
            capabilities.add("weather");
        }

        if (this.postProgramAvailable) {
            capabilities.add("post");
        }

        if (capabilities.isEmpty()) {
            ActiniumShaders.logger().info("Active shader pack does not expose shadow/sky/particles/weather/post programs yet");
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

        if (this.sceneDepthPackProgram != null) {
            this.sceneDepthPackProgram.delete();
            this.sceneDepthPackProgram = null;
        }

        if (this.firstPersonDepthMergeProgram != null) {
            this.firstPersonDepthMergeProgram.delete();
            this.firstPersonDepthMergeProgram = null;
        }

        if (this.deferredSkyPackProgram != null) {
            this.deferredSkyPackProgram.delete();
            this.deferredSkyPackProgram = null;
        }

        if (this.deferredSkyMergeProgram != null) {
            this.deferredSkyMergeProgram.delete();
            this.deferredSkyMergeProgram = null;
        }

        if (this.shadowEntityProgram != null) {
            this.shadowEntityProgram.delete();
            this.shadowEntityProgram = null;
        }

        this.finalProgramResolved = false;
        this.shadowEntityProgramResolved = false;
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
        this.postPipelineDeferredForFirstPersonThisFrame = false;
        this.terrainInputsPreparedFrame = Integer.MIN_VALUE;
        this.terrainInputsPreparedFramebufferTexture = -1;
        this.terrainInputsPreparedWidth = -1;
        this.terrainInputsPreparedHeight = -1;
        this.terrainInputsPreparedMask = 0;
        this.worldCameraPosition.zero();
        this.previousWorldCameraPosition.zero();
        this.servedPreviousWorldCameraPosition.zero();
        this.previousGbufferModelViewMatrix.identity();
        this.previousGbufferProjectionMatrix.identity();
        this.servedPreviousGbufferModelViewMatrix.identity();
        this.servedPreviousGbufferProjectionMatrix.identity();
        this.shaderCameraPosition.zero();
        this.previousShaderCameraPosition.zero();
        this.shaderCameraPositionUnshifted.zero();
        this.previousShaderCameraPositionUnshifted.zero();
        this.shaderCameraShift.zero();
        this.shaderCameraPositionInitialized = false;
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

        if (this.postCompositeScratchDepthTexture != null) {
            GL11.glDeleteTextures(this.postCompositeScratchDepthTexture);
            this.postCompositeScratchDepthTexture = null;
        }

        if (this.scratchCopyReadFramebuffer != 0) {
            GL30.glDeleteFramebuffers(this.scratchCopyReadFramebuffer);
            this.scratchCopyReadFramebuffer = 0;
        }

        if (this.scratchCopyDrawFramebuffer != 0) {
            GL30.glDeleteFramebuffers(this.scratchCopyDrawFramebuffer);
            this.scratchCopyDrawFramebuffer = 0;
        }

        this.terrainInputTextureWidth = -1;
        this.terrainInputTextureHeight = -1;
        this.postCompositeScratchDepthTextureWidth = -1;
        this.postCompositeScratchDepthTextureHeight = -1;

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

        if (this.entitiesProgram != null) {
            this.entitiesProgram.program().delete();
            this.entitiesProgram = null;
        }

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

        if (this.particlesProgram != null) {
            this.particlesProgram.program().delete();
            this.particlesProgram = null;
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
        this.transitionPhase(ActiniumPipelinePhase.NONE);
        this.worldTargetsPrepared = false;

        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();
        int framebufferWidth = framebuffer != null ? Math.max(1, framebuffer.framebufferWidth) : Math.max(1, this.width);
        int framebufferHeight = framebuffer != null ? Math.max(1, framebuffer.framebufferHeight) : Math.max(1, this.height);
        this.restoreScreenRenderState(framebuffer, framebufferWidth, framebufferHeight);
        debugCheckGlErrors("pipeline.resetVanillaRenderState");
    }

    public void prepareFirstPersonRenderState() {
        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer framebuffer = minecraft.getFramebuffer();
        int framebufferWidth = framebuffer != null ? Math.max(1, framebuffer.framebufferWidth) : Math.max(1, this.width);
        int framebufferHeight = framebuffer != null ? Math.max(1, framebuffer.framebufferHeight) : Math.max(1, this.height);

        this.restoreMainFramebufferState(framebuffer);
        GlStateManager.viewport(0, 0, framebufferWidth, framebufferHeight);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableFog();
        GlStateManager.enableCull();
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);
        this.debugLogFrameStageSummary("prepareFirstPersonRenderState");
    }

    private void releaseActiveWorldStageState() {
        if (this.activeWorldProgram != null) {
            this.unbindWorldStageTextures();
            this.activeWorldProgram.program().unbind();
            this.activeWorldProgram = null;
            debugCheckGlErrors("world-stage.releaseProgram");
        }

        if (this.activeEntityWorldProgram != null) {
            this.unbindWorldStageTextures();
            this.activeEntityWorldProgram.program().unbind();
            this.activeEntityWorldProgram = null;
            debugCheckGlErrors("world-stage.releasePerEntityProgram");
        }

        this.activeShadowEntityProgram = null;

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
            case ENTITIES -> ENABLE_EXTERNAL_ENTITIES_STAGE ? this.getWorldStageProgram(ActiniumWorldStage.ENTITIES) : null;
            case SKY -> ENABLE_EXTERNAL_SKY_BASIC_STAGE ? this.getWorldStageProgram(ActiniumWorldStage.SKY) : null;
            case SKY_TEXTURED -> ENABLE_EXTERNAL_SKY_TEXTURED_STAGE ? this.getWorldStageProgram(ActiniumWorldStage.SKY_TEXTURED) : null;
            case CLOUDS -> ENABLE_EXTERNAL_CLOUDS_STAGE ? this.getWorldStageProgram(ActiniumWorldStage.CLOUDS) : null;
            case PARTICLES -> this.getWorldStageProgram(ActiniumWorldStage.PARTICLES);
            case WEATHER -> this.getWorldStageProgram(ActiniumWorldStage.WEATHER);
            default -> null;
        };
    }

    private @Nullable ActiniumWorldProgram getWorldStageProgram(ActiniumWorldStage stage) {
        return switch (stage) {
            case ENTITIES -> {
                if (this.entitiesProgram == null) {
                    this.entitiesProgram = this.createWorldStageProgram(stage);
                }
                yield this.entitiesProgram;
            }
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
            case PARTICLES -> {
                if (this.particlesProgram == null) {
                    this.particlesProgram = this.createWorldStageProgram(stage);
                    this.particlesProgram = this.tryCreateFallbackWorldStageProgram(stage, this.particlesProgram);
                }
                yield this.particlesProgram;
            }
            case WEATHER -> {
                if (this.weatherProgram == null) {
                    this.weatherProgram = this.createWorldStageProgram(stage);
                    this.weatherProgram = this.tryCreateFallbackWorldStageProgram(stage, this.weatherProgram);
                }
                yield this.weatherProgram;
            }
        };
    }

    private @Nullable ActiniumWorldProgram createWorldStageProgram(ActiniumWorldStage stage) {
        return this.createWorldStageProgram(stage, stage.programName());
    }

    private @Nullable ActiniumWorldProgram createWorldStageProgram(ActiniumWorldStage stage, String programName) {
        String vertexSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.VERTEX);
        String fragmentSource = ActiniumShaderPackManager.getProgramSource(programName, ShaderType.FRAGMENT);

        if (vertexSource == null || fragmentSource == null) {
            return null;
        }

        String resolvedVertexSource = ShaderParser.parseShader(vertexSource, this::resolveShaderSource, ShaderConstants.EMPTY);
        String resolvedFragmentSource = ShaderParser.parseShader(fragmentSource, this::resolveShaderSource, ShaderConstants.EMPTY);
        int[] drawBuffers = this.resolveWorldStageDrawBuffers(stage, resolvedFragmentSource);
        if (ActiniumShaderPackManager.isDebugEnabled()
                && (stage == ActiniumWorldStage.SKY || stage == ActiniumWorldStage.SKY_TEXTURED)) {
            this.debugLog(
                    "Sky shader source hints '{}' vertex[upVector={}, upPosition={}, gbufferModelView[1]={}] fragment[upVector={}, upPosition={}, gbufferModelView[1]={}]",
                    programName,
                    resolvedVertexSource.contains("upVector"),
                    resolvedVertexSource.contains("upPosition"),
                    resolvedVertexSource.contains("gbufferModelView[1]"),
                    resolvedFragmentSource.contains("upVector"),
                    resolvedFragmentSource.contains("upPosition"),
                    resolvedFragmentSource.contains("gbufferModelView[1]")
            );
        }
        List<GlShader> shaders = new ArrayList<>(2);
        shaders.add(new GlShader(ShaderType.VERTEX, "actinium:world/" + programName + "." + ShaderType.VERTEX.fileExtension, resolvedVertexSource));
        shaders.add(new GlShader(ShaderType.FRAGMENT, "actinium:world/" + programName + "." + ShaderType.FRAGMENT.fileExtension, resolvedFragmentSource));

        try {
            GlProgram.Builder builder = GlProgram.builder("actinium:world/" + programName);
            shaders.forEach(builder::attachShader);
            this.debugLog("Compiled world-stage program '{}' with draw buffers {}", programName, Arrays.toString(drawBuffers));
            return new ActiniumWorldProgram(programName, builder.link(ActiniumWorldShaderInterface::new), drawBuffers);
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to compile external shader pack world-stage program '{}'", programName, e);
            return null;
        } finally {
            shaders.forEach(GlShader::delete);
        }
    }

    private @Nullable ActiniumWorldProgram tryCreateFallbackWorldStageProgram(ActiniumWorldStage stage, @Nullable ActiniumWorldProgram currentProgram) {
        if (currentProgram != null) {
            return currentProgram;
        }

        for (String fallbackProgramName : stage.fallbackProgramNames()) {
            this.debugLog("Falling back world-stage program '{}' to '{}'", stage.programName(), fallbackProgramName);
            ActiniumWorldProgram fallbackProgram = this.createWorldStageProgram(stage, fallbackProgramName);
            if (fallbackProgram != null) {
                return fallbackProgram;
            }
        }

        return null;
    }

    private boolean shouldRedirectWorldStageFramebuffer(@Nullable ActiniumWorldProgram program) {
        return program != null
                && program != this.entitiesProgram
                && program != this.particlesProgram
                && program != this.weatherProgram;
    }

    private void ensureTerrainInputTextures(int requiredMask) {
        if (this.width <= 0 || this.height <= 0) {
            this.debugLog("Skipping terrain input allocation because framebuffer size is invalid: {}x{}", this.width, this.height);
            return;
        }

        if (this.terrainInputTextureWidth != this.width || this.terrainInputTextureHeight != this.height) {
            if (this.terrainInputTextureWidth > 0 && this.terrainInputTextureHeight > 0) {
                this.debugLog("Resizing terrain input textures from {}x{} to {}x{}",
                        this.terrainInputTextureWidth, this.terrainInputTextureHeight, this.width, this.height);
            }
            this.deleteTerrainInputTextures();
            this.terrainInputTextureWidth = this.width;
            this.terrainInputTextureHeight = this.height;
        }

        if ((requiredMask & TERRAIN_INPUT_GAUX1) != 0 && this.terrainGaux1Texture == null) {
            this.terrainGaux1Texture = createColorTexture(this.width, this.height);
            this.debugLog("Allocated terrain gaux1 texture {} with size {}x{}", this.terrainGaux1Texture, this.width, this.height);
        }

        if ((requiredMask & TERRAIN_INPUT_GAUX2) != 0 && this.terrainGaux2Texture == null) {
            this.terrainGaux2Texture = createColorTexture(this.width, this.height);
            this.debugLog("Allocated terrain gaux2 texture {} with size {}x{}", this.terrainGaux2Texture, this.width, this.height);
        }

        if ((requiredMask & TERRAIN_INPUT_DEPTHTEX0) != 0 && this.terrainDepthTexture0 == null) {
            this.terrainDepthTexture0 = createDepthTexture(this.width, this.height);
            this.debugLog("Allocated terrain depthtex0 texture {} with size {}x{}", this.terrainDepthTexture0, this.width, this.height);
        }

        if ((requiredMask & TERRAIN_INPUT_DEPTHTEX1) != 0 && this.terrainDepthTexture1 == null) {
            this.terrainDepthTexture1 = createDepthTexture(this.width, this.height);
            this.debugLog("Allocated terrain depthtex1 texture {} with size {}x{}", this.terrainDepthTexture1, this.width, this.height);
        }
    }

    private void deleteTerrainInputTextures() {
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
    }

    private void prepareWorldTargets(Framebuffer framebuffer) {
        if (this.worldTargets == null || this.width <= 0 || this.height <= 0) {
            return;
        }

        clearGlErrorsSilently();
        this.worldTargets.ensureSize(this.width, this.height);
        debugCheckGlErrors("pipeline.prepareWorldTargets.ensureSize");

        if (!this.worldTargetsPrepared) {
            float[] fogColor = this.getFogColor();
            float clearRed = fogColor[0];
            float clearGreen = fogColor[1];
            float clearBlue = fogColor[2];

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
        this.debugLogTextureCenter("terrain." + pass.name() + "." + stage + ".gaux4", this.worldTargets.getSourceGaux4Texture());
        this.debugLogTextureCenter("terrain." + pass.name() + "." + stage + ".mainFramebuffer", framebuffer.framebufferTexture);
    }

    private int[] resolveTerrainPassDrawBuffers(TerrainRenderPass pass) {
        return new int[]{ActiniumPostTargets.TARGET_COLORTEX1, ActiniumPostTargets.TARGET_GAUX4};
    }

    private @Nullable String getActivePackShaderSource() {
        String normalizedPath = "src/writebuffers.glsl".startsWith("/") ? "src/writebuffers.glsl".substring(1) : "src/writebuffers.glsl";
        return ActiniumShaderPackManager.getShaderSource("actinium:" + normalizedPath);
    }

    private @Nullable Integer getWorldGaux4TextureForPost() {
        if (this.worldTargets == null) {
            return null;
        }

        int textureId = this.worldTargets.getSourceGaux4Texture();
        return textureId > 0 ? textureId : null;
    }

    private void syncWorldStageGaux4FromPreSceneTargets(String stageName) {
        if (this.worldTargets == null || this.preSceneTargets == null) {
            return;
        }

        int sourceTexture = this.preSceneTargets.getSourceTexture(ActiniumPostTargets.TARGET_GAUX4);
        int destinationTexture = this.worldTargets.getSourceGaux4Texture();
        if (sourceTexture <= 0 || destinationTexture <= 0 || sourceTexture == destinationTexture) {
            return;
        }

        this.copyTexture(sourceTexture, destinationTexture);
        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLogTextureCenter("world-stage." + stageName + ".gaux4", this.worldTargets.getSourceGaux4Texture());
        }
    }

    private void captureManagedSkyColor(float partialTicks) {
        if (this.skyColorCapturedThisFrame) {
            this.managedSkyColor.set(this.capturedSkyColor);
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Entity entity = minecraft.getRenderViewEntity();

        if (minecraft.world == null || entity == null) {
            return;
        }

        Vec3d skyColor = minecraft.world.getSkyColor(entity, partialTicks);
        this.captureSkyColor((float) skyColor.x, (float) skyColor.y, (float) skyColor.z);
    }

    private void captureManagedSkyUpPosition() {
        captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.scratchManagedSkyMatrix);
        this.scratchManagedSkyMatrix.transformDirection(0.0f, 100.0f, 0.0f, this.managedSkyUpPosition);

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog(
                    "Captured managed sky up position stage={} result=[{}, {}, {}]",
                    this.currentStage,
                    this.managedSkyUpPosition.x,
                    this.managedSkyUpPosition.y,
                    this.managedSkyUpPosition.z
            );
        }
    }

    private void captureManagedSkyCelestialState() {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft.world == null) {
            return;
        }

        captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.scratchManagedSkyMatrix);
        this.scratchManagedSkyMatrix.transformDirection(0.0f, 100.0f, 0.0f, this.managedSkySunPosition);
        this.scratchManagedSkyMatrix.transformDirection(0.0f, -100.0f, 0.0f, this.managedSkyMoonPosition);

        Vector3f shadowLight = this.isShaderCoreShadowUsingSun(minecraft.getRenderPartialTicks())
                ? this.managedSkySunPosition
                : this.managedSkyMoonPosition;
        this.managedSkyShadowLightPosition.set(shadowLight);
        this.managedSkyCelestialStateValid = true;

        if (this.shouldEmitVerboseDebugFrame()) {
            this.debugLog(
                    "Captured managed sky celestial state stage={} sun=[{}, {}, {}] moon=[{}, {}, {}] shadow=[{}, {}, {}]",
                    this.currentStage,
                    this.managedSkySunPosition.x,
                    this.managedSkySunPosition.y,
                    this.managedSkySunPosition.z,
                    this.managedSkyMoonPosition.x,
                    this.managedSkyMoonPosition.y,
                    this.managedSkyMoonPosition.z,
                    this.managedSkyShadowLightPosition.x,
                    this.managedSkyShadowLightPosition.y,
                    this.managedSkyShadowLightPosition.z
            );
        }
    }

    private void copyTexture(int sourceTexture, @Nullable Integer destinationTexture) {
        if (destinationTexture == null || sourceTexture <= 0) {
            return;
        }

        this.ensureScratchCopyFramebuffers();
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);

        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.scratchCopyReadFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sourceTexture, 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.scratchCopyDrawFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, destinationTexture, 0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL11.glReadBuffer(previousReadBuffer);
            GL11.glDrawBuffer(previousDrawBuffer);
        }
    }

    private void copyFramebufferDepthToTexture(@Nullable Integer textureId) {
        if (textureId == null) {
            return;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, this.width, this.height);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void copyCurrentFramebufferColorToTexture(@Nullable Integer textureId) {
        if (textureId == null) {
            return;
        }

        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try {
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, this.width, this.height);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL11.glReadBuffer(previousReadBuffer);
        }
    }

    private void ensurePostCompositeScratchDepthTexture() {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        if (this.postCompositeScratchDepthTexture != null
                && (this.postCompositeScratchDepthTextureWidth != this.width
                || this.postCompositeScratchDepthTextureHeight != this.height)) {
            GL11.glDeleteTextures(this.postCompositeScratchDepthTexture);
            this.postCompositeScratchDepthTexture = null;
            this.postCompositeScratchDepthTextureWidth = -1;
            this.postCompositeScratchDepthTextureHeight = -1;
        }

        if (this.postCompositeScratchDepthTexture == null) {
            this.postCompositeScratchDepthTexture = createDepthTexture(this.width, this.height);
            this.postCompositeScratchDepthTextureWidth = this.width;
            this.postCompositeScratchDepthTextureHeight = this.height;
        }
    }

    private void ensureScratchCopyFramebuffers() {
        if (this.scratchCopyReadFramebuffer == 0) {
            this.scratchCopyReadFramebuffer = GL30.glGenFramebuffers();
        }

        if (this.scratchCopyDrawFramebuffer == 0) {
            this.scratchCopyDrawFramebuffer = GL30.glGenFramebuffers();
        }
    }

    private void debugLog(String message, Object... args) {
        if (!ActiniumShaderPackManager.isDebugEnabled()) {
            return;
        }

        ActiniumShaders.logger().info("[DEBUG] " + message, args);
    }

    public void debugLogFogState(String label, String source) {
        if (!this.shouldEmitVerboseDebugFrame()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityRenderer entityRenderer = minecraft.entityRenderer;
        float entityFogRed = Float.NaN;
        float entityFogGreen = Float.NaN;
        float entityFogBlue = Float.NaN;
        if (entityRenderer != null) {
            EntityRendererAccessor accessor = (EntityRendererAccessor) (Object) entityRenderer;
            entityFogRed = accessor.celeritas$getFogColorRed();
            entityFogGreen = accessor.celeritas$getFogColorGreen();
            entityFogBlue = accessor.celeritas$getFogColorBlue();
        }

        Vec3d worldFog = minecraft.world != null ? minecraft.world.getFogColor(minecraft.getRenderPartialTicks()) : null;
        this.debugLog(
                "Fog state '{}' source={} stage={} initialized={} capturedGL={} fog=[{}, {}, {}] clear=[{}, {}, {}] entity=[{}, {}, {}] worldFog={} entityRendererPresent={}",
                label,
                source,
                this.currentStage,
                this.fogColorInitialized,
                this.fogColorCapturedFromGlBufferThisFrame,
                this.fogColor[0],
                this.fogColor[1],
                this.fogColor[2],
                this.clearColor[0],
                this.clearColor[1],
                this.clearColor[2],
                entityFogRed,
                entityFogGreen,
                entityFogBlue,
                worldFog,
                entityRenderer != null
        );
    }

    private static int nextFrameId(int current) {
        return (current + 1) % 720720;
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

    private void updateShaderCameraPositions() {
        if (!this.shaderCameraPositionInitialized) {
            this.shaderCameraPosition.set(this.worldCameraPosition);
            this.previousShaderCameraPosition.set(this.worldCameraPosition);
            this.shaderCameraPositionUnshifted.set(this.worldCameraPosition);
            this.previousShaderCameraPositionUnshifted.set(this.worldCameraPosition);
            this.shaderCameraPositionInitialized = true;
        } else {
            this.previousShaderCameraPosition.set(this.shaderCameraPosition);
            this.previousShaderCameraPositionUnshifted.set(this.shaderCameraPositionUnshifted);
            this.shaderCameraPosition.set(this.worldCameraPosition).add(this.shaderCameraShift);
            this.shaderCameraPositionUnshifted.set(this.worldCameraPosition);
        }

        double dX = getCameraShift(this.shaderCameraPosition.x, this.previousShaderCameraPosition.x);
        double dZ = getCameraShift(this.shaderCameraPosition.z, this.previousShaderCameraPosition.z);

        if (dX != 0.0D || dZ != 0.0D) {
            applyShaderCameraShift(dX, dZ);
        }
    }

    private static double getCameraShift(double value, double prevValue) {
        if (Math.abs(value) > 30000.0D || Math.abs(value - prevValue) > 1000.0D) {
            return -(value - (value % 30000.0D));
        }

        return 0.0D;
    }

    private void applyShaderCameraShift(double dX, double dZ) {
        this.shaderCameraShift.x += dX;
        this.shaderCameraPosition.x += dX;
        this.previousShaderCameraPosition.x += dX;

        this.shaderCameraShift.z += dZ;
        this.shaderCameraPosition.z += dZ;
        this.previousShaderCameraPosition.z += dZ;
    }

    private void captureMatrix(int matrixType, Matrix4f destination) {
        this.scratchCapturedMatrixBuffer.clear();
        invokeGlGetFloat(matrixType, this.scratchCapturedMatrixBuffer);
        destination.set(this.scratchCapturedMatrixBuffer);
    }

    private void computeShadowMatrices(float partialTicks, ActiniumShaderProperties properties, int resolution) {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft.world == null || minecraft.getRenderViewEntity() == null) {
            this.shadowCameraPosition.zero();
            this.shadowModelViewMatrix.identity();
            this.shadowProjectionMatrix.identity();
            this.shadowModelViewInverseMatrix.identity();
            this.shadowProjectionInverseMatrix.identity();
            return;
        }

        this.shadowCameraPosition.set(this.worldCameraPosition);

        float celestialAngle = this.getShaderCoreCelestialAngle(partialTicks);
        float sunAngle = celestialAngle < 0.75f ? celestialAngle + 0.25f : celestialAngle - 0.75f;
        float angle = celestialAngle * -360.0f;
        float intervalSize = Math.max(0.0f, properties.getShadowIntervalSize());
        float angleInterval = 0.0f;
        float shadowDistance = Math.max(16.0f, Math.min(
                properties.getShadowDistance(),
                minecraft.gameSettings.renderDistanceChunks * 16.0f
        ));
        float nearPlane = properties.getShadowNearPlane();
        float farPlane = Math.max(nearPlane + 1.0f, properties.getShadowFarPlane());

        this.shadowModelViewMatrix.identity()
                .translate(0.0f, 0.0f, -100.0f)
                .rotateX((float) Math.toRadians(90.0f));

        if (sunAngle <= 0.5f) {
            this.shadowModelViewMatrix.rotateZ((float) Math.toRadians(angle - angleInterval));
        } else {
            this.shadowModelViewMatrix.rotateZ((float) Math.toRadians(angle + 180.0f - angleInterval));
        }

        this.shadowModelViewMatrix.rotateX((float) Math.toRadians(properties.getSunPathRotation()));

        if (intervalSize > 0.0f) {
            float halfInterval = intervalSize * 0.5f;
            float offsetX = (float) this.shadowCameraPosition.x % intervalSize - halfInterval;
            float offsetY = (float) this.shadowCameraPosition.y % intervalSize - halfInterval;
            float offsetZ = (float) this.shadowCameraPosition.z % intervalSize - halfInterval;
            this.shadowModelViewMatrix.translate(offsetX, offsetY, offsetZ);
        }

        this.shadowProjectionMatrix.identity().ortho(-shadowDistance, shadowDistance, -shadowDistance, shadowDistance, nearPlane, farPlane);
        this.stabilizeShadowProjection(resolution);
        this.shadowModelViewInverseMatrix.set(this.shadowModelViewMatrix).invert();
        this.shadowProjectionInverseMatrix.set(this.shadowProjectionMatrix).invert();
    }

    private void stabilizeShadowProjection(int resolution) {
        if (resolution <= 0) {
            return;
        }

        this.scratchShadowStabilizationMatrix.set(this.shadowProjectionMatrix).mul(this.shadowModelViewMatrix);
        this.scratchShadowOrigin.set(0.0f, 0.0f, 0.0f, 1.0f);
        this.scratchShadowStabilizationMatrix.transform(this.scratchShadowOrigin);

        float texelSize = 2.0f / resolution;
        float snappedX = Math.round(this.scratchShadowOrigin.x / texelSize) * texelSize;
        float snappedY = Math.round(this.scratchShadowOrigin.y / texelSize) * texelSize;
        float offsetX = snappedX - this.scratchShadowOrigin.x;
        float offsetY = snappedY - this.scratchShadowOrigin.y;

        this.shadowProjectionMatrix.m30(this.shadowProjectionMatrix.m30() + offsetX);
        this.shadowProjectionMatrix.m31(this.shadowProjectionMatrix.m31() + offsetY);
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
            this.shadowTargets.configureSampling(properties.isShadowHardwareFiltering());
        }
        FloatBuffer previousProjection = ActiveRenderInfoAccessor.getProjectionMatrix();
        FloatBuffer previousModelView = ActiveRenderInfoAccessor.getModelViewMatrix();
        Minecraft minecraft = Minecraft.getMinecraft();
        ShadowPassGlState glState = ShadowPassGlState.capture();
        int shadowFrame = nextFrameId(this.shadowVisibilityFrameCounter);
        this.shadowVisibilityFrameCounter = shadowFrame;

        try {
            ActiveRenderInfoAccessor.setProjectionMatrix(writeMatrix(this.shadowProjectionMatrix, this.shadowTerrainProjectionBuffer));
            ActiveRenderInfoAccessor.setModelViewMatrix(writeMatrix(this.shadowModelViewMatrix, this.shadowTerrainModelViewBuffer));
            ActiniumInternalShadowRenderingState.begin(
                    this.shadowModelViewMatrix,
                    this.shadowProjectionMatrix,
                    properties.isShadowEntities(),
                    properties.isShadowPlayer(),
                    properties.isShadowBlockEntities()
            );
            int[] shadowDrawBuffers = this.resolveShadowDrawBuffers();
            this.shadowTargets.beginWrite(shadowDrawBuffers);
            if (ActiniumShaderPackManager.isDebugEnabled() && this.shouldEmitVerboseDebugFrame()) {
                int framebufferStatus = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
                this.debugLog(
                        "Shadow terrain framebuffer status={} ({}) resolution={} camera=({}, {}, {}) drawBuffers={}",
                        framebufferStatusName(framebufferStatus),
                        framebufferStatus,
                        resolution,
                        this.shadowCameraPosition.x,
                        this.shadowCameraPosition.y,
                        this.shadowCameraPosition.z,
                        Arrays.toString(shadowDrawBuffers)
                );
            }
            RenderDevice.enterManagedCode();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.bindTexture(minecraft.getTextureMapBlocks().getGlTextureId());
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            minecraft.entityRenderer.enableLightmap();

            renderer.setupTerrain(
                    this.createShadowViewport(partialTicks),
                    this.createShadowCameraState(partialTicks),
                    shadowFrame,
                    minecraft.player != null && minecraft.player.isSpectator(),
                    false
            );

            if (ActiniumShaderPackManager.isDebugEnabled() && this.shouldEmitVerboseDebugFrame()) {
                this.debugLog(
                        "Shadow terrain setup visibleChunks={} shadowFrame={} shadowDistance={} interval={} near={} far={}",
                        renderer.getVisibleChunkCount(),
                        shadowFrame,
                        properties.getShadowDistance(),
                        properties.getShadowIntervalSize(),
                        properties.getShadowNearPlane(),
                        properties.getShadowFarPlane()
                );
            }

            if (properties.isShadowTerrain()) {
                renderer.drawChunkLayer(BlockRenderLayer.SOLID, this.shadowCameraPosition.x, this.shadowCameraPosition.y, this.shadowCameraPosition.z);
                renderer.drawChunkLayer(BlockRenderLayer.CUTOUT_MIPPED, this.shadowCameraPosition.x, this.shadowCameraPosition.y, this.shadowCameraPosition.z);
                renderer.drawChunkLayer(BlockRenderLayer.CUTOUT, this.shadowCameraPosition.x, this.shadowCameraPosition.y, this.shadowCameraPosition.z);
            }

            try {
                this.renderShadowEntityPass(resolution, partialTicks);
            } catch (RuntimeException e) {
                ActiniumShaders.logger().warn("Failed to render Actinium shadow entity pass; keeping terrain shadow map", e);
            }

            if (this.shadowTargets != null) {
                this.shadowTargets.copyDepthPrimaryToSecondary();
            }

            if (properties.isShadowTranslucent()) {
                renderer.drawChunkLayer(BlockRenderLayer.TRANSLUCENT, this.shadowCameraPosition.x, this.shadowCameraPosition.y, this.shadowCameraPosition.z);
            }

            if (ActiniumShaderPackManager.isDebugEnabled() && this.shouldEmitVerboseDebugFrame() && this.shadowTargets != null) {
                this.debugLogShadowTargetState("shadow.terrain", resolution);
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

    private Viewport createShadowViewport(float partialTicks) {
        float shadowDistance = this.getShadowTerrainRenderDistance();
        this.scratchShadowViewportPosition.set(this.shadowCameraPosition.x, this.shadowCameraPosition.y, this.shadowCameraPosition.z);
        Vector3f sunPosition = new Vector3f();
        Vector3f moonPosition = new Vector3f();
        Vector3f shadowLightVector = new Vector3f();
        this.fillShaderCoreCelestialUniforms(this.gbufferModelViewMatrix, partialTicks, sunPosition, moonPosition, shadowLightVector);
        return new Viewport(new ShadowViewportFrustum(
                this.shadowProjectionMatrix,
                this.shadowModelViewMatrix,
                this.gbufferProjectionMatrix,
                this.gbufferModelViewMatrix,
                shadowLightVector,
                shadowDistance
        ), this.scratchShadowViewportPosition);
    }

    private SimpleWorldRenderer.CameraState createShadowCameraState(float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity renderViewEntity = minecraft.getRenderViewEntity();
        return new SimpleWorldRenderer.CameraState(
                this.shadowCameraPosition.x,
                this.shadowCameraPosition.y,
                this.shadowCameraPosition.z,
                renderViewEntity.rotationPitch,
                renderViewEntity.rotationYaw,
                ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff()
        );
    }

    private void renderShadowEntityPass(int resolution, float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity renderViewEntity = minecraft.getRenderViewEntity();
        ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();

        if (minecraft.world == null || renderViewEntity == null || !properties.isShadowEnabled()) {
            return;
        }

        boolean renderEntityShadows = properties.isShadowEntities();
        boolean renderPlayerShadow = properties.isShadowPlayer();

        if (!renderEntityShadows && !renderPlayerShadow) {
            return;
        }

        ICamera camera = this.createShadowEntityCamera(properties);
        double entityX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        double entityY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        double entityZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
        camera.setPosition(entityX, entityY, entityZ);

        RenderManager renderManager = minecraft.getRenderManager();
        int previousRenderPass = MinecraftForgeClient.getRenderPass();
        boolean previousRenderShadow = renderManager.isRenderShadow();
        int previousThirdPersonView = minecraft.gameSettings.thirdPersonView;
        FloatBuffer previousProjection = ActiveRenderInfoAccessor.getProjectionMatrix();
        FloatBuffer previousModelView = ActiveRenderInfoAccessor.getModelViewMatrix();
        FloatBuffer previousObjectCoords = ActiveRenderInfoAccessor.getObjectCoords();
        IntBuffer previousViewport = ActiveRenderInfoAccessor.getViewportBuffer();

        try {
            ForgeHooksClient.setRenderPass(0);
            if (renderPlayerShadow) {
                minecraft.gameSettings.thirdPersonView = 1;
            }
            renderManager.cacheActiveRenderInfo(
                    minecraft.world,
                    minecraft.fontRenderer,
                    renderViewEntity,
                    minecraft.pointedEntity,
                    minecraft.gameSettings,
                    partialTicks
            );
            renderManager.setRenderPosition(entityX, entityY, entityZ);
            renderManager.setRenderShadow(false);
            GL20.glUseProgram(0);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadMatrixf(this.toGlMatrixBuffer(this.shadowProjectionMatrix));
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadMatrixf(this.toGlMatrixBuffer(this.shadowModelViewMatrix));
            GL11.glViewport(0, 0, resolution, resolution);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            GlStateManager.disableBlend();
            GlStateManager.disableFog();
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(2.0f, 8.0f);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableCull();
            GlProgram<ActiniumShadowShaderInterface> shadowProgram = this.getShadowEntityProgram();
            if (shadowProgram != null) {
                shadowProgram.bind();
                shadowProgram.getInterface().setupState(
                        this.shadowModelViewMatrix,
                        this.shadowModelViewInverseMatrix,
                        this.shadowProjectionMatrix,
                        this.shadowProjectionInverseMatrix
                );
                this.activeShadowEntityProgram = shadowProgram;
            } else {
                this.activeShadowEntityProgram = null;
            }
            this.shadowTerrainModelViewBuffer.clear();
            this.shadowTerrainProjectionBuffer.clear();
            this.scratchShadowObjectCoordsBuffer.clear();
            this.scratchShadowViewportBuffer.clear();
            ActiveRenderInfoAccessor.setModelViewMatrix(this.shadowTerrainModelViewBuffer);
            ActiveRenderInfoAccessor.setProjectionMatrix(this.shadowTerrainProjectionBuffer);
            ActiveRenderInfoAccessor.setObjectCoords(this.scratchShadowObjectCoordsBuffer);
            ActiveRenderInfoAccessor.setViewportBuffer(this.scratchShadowViewportBuffer);
            ActiveRenderInfo.updateRenderInfo(renderViewEntity, minecraft.gameSettings.thirdPersonView == 2);
            if (renderEntityShadows || renderPlayerShadow) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                minecraft.renderGlobal.renderEntities(renderViewEntity, camera, partialTicks);
                GL11.glPopMatrix();
            }

            if (ActiniumShaderPackManager.isDebugEnabled() && this.shouldEmitVerboseDebugFrame() && this.shadowTargets != null) {
                this.debugLogShadowTargetState("shadow.entities", resolution);
            }
        } finally {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(0.0f, 0.0f);
            ActiveRenderInfoAccessor.setProjectionMatrix(previousProjection);
            ActiveRenderInfoAccessor.setModelViewMatrix(previousModelView);
            ActiveRenderInfoAccessor.setObjectCoords(previousObjectCoords);
            ActiveRenderInfoAccessor.setViewportBuffer(previousViewport);
            if (this.activeShadowEntityProgram != null) {
                this.activeShadowEntityProgram.unbind();
                this.activeShadowEntityProgram = null;
            }
            minecraft.gameSettings.thirdPersonView = previousThirdPersonView;
            renderManager.setRenderShadow(previousRenderShadow);
            ForgeHooksClient.setRenderPass(previousRenderPass);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }

    private @Nullable GlProgram<ActiniumShadowShaderInterface> getShadowEntityProgram() {
        if (this.shadowEntityProgramResolved) {
            return this.shadowEntityProgram;
        }

        this.shadowEntityProgramResolved = true;
        String vertexSource = ActiniumShaderPackManager.getProgramSource("shadow", ShaderType.VERTEX);
        String fragmentSource = ActiniumShaderPackManager.getProgramSource("shadow", ShaderType.FRAGMENT);

        if (vertexSource == null || fragmentSource == null) {
            return null;
        }

        String resolvedVertexSource = ShaderParser.parseShader(vertexSource, this::resolveShaderSource, ShaderConstants.EMPTY);
        String resolvedFragmentSource = ShaderParser.parseShader(fragmentSource, this::resolveShaderSource, ShaderConstants.EMPTY);
        List<GlShader> shaders = new ArrayList<>(2);
        shaders.add(new GlShader(ShaderType.VERTEX, "actinium:shadow/entity." + ShaderType.VERTEX.fileExtension, resolvedVertexSource));
        shaders.add(new GlShader(ShaderType.FRAGMENT, "actinium:shadow/entity." + ShaderType.FRAGMENT.fileExtension, resolvedFragmentSource));

        try {
            GlProgram.Builder builder = GlProgram.builder("actinium:shadow/entity");
            shaders.forEach(builder::attachShader);
            int[] shadowDrawBuffers = this.resolveShadowDrawBuffers();
            for (int i = 0; i < shadowDrawBuffers.length; i++) {
                builder.bindFragmentData("fragColor" + i, i);
            }
            this.shadowEntityProgram = builder.link(ActiniumShadowShaderInterface::new);
            this.debugLog("Compiled external shader pack entity shadow program");
            return this.shadowEntityProgram;
        } catch (RuntimeException e) {
            ActiniumShaders.logger().warn("Failed to compile external shader pack entity shadow program", e);
            return null;
        } finally {
            shaders.forEach(GlShader::delete);
        }
    }

    private void debugLogShadowTargetState(String label, int resolution) {
        if (!this.shouldEmitVerboseDebugFrame() || this.shadowTargets == null || resolution <= 0) {
            return;
        }

        this.debugLogTextureSamples(label + ".color0", this.shadowTargets.getColorTexture(0), resolution, resolution);
        this.debugLogTextureSamples(label + ".color1", this.shadowTargets.getColorTexture(1), resolution, resolution);
        this.debugLogDepthTextureSample(label + ".depth0", this.shadowTargets.getDepthTexture(0), resolution, resolution);
        this.debugLogDepthTextureSample(label + ".depth1", this.shadowTargets.getDepthTexture(1), resolution, resolution);
    }

    private int[] resolveShadowDrawBuffers() {
        String fragmentSource = ActiniumShaderPackManager.getProgramSource("shadow", ShaderType.FRAGMENT);

        if (fragmentSource == null) {
            return new int[]{0};
        }

        ProgramMetadata metadata = ProgramMetadata.parse(this.resolveShaderForMetadata(fragmentSource));
        int[] drawBuffers = metadata.drawBuffers();

        if (ActiniumShaderPackManager.isDebugEnabled()) {
            this.debugLog(
                    "Resolved shadow program metadata: drawBuffers={}, mipmappedBuffers={}",
                    Arrays.toString(drawBuffers),
                    Arrays.toString(metadata.mipmappedBuffers())
            );
        }

        if (drawBuffers.length == 0) {
            return new int[]{0};
        }

        return drawBuffers;
    }

    private float getShadowTerrainRenderDistance() {
        ActiniumShaderProperties properties = ActiniumShaderPackManager.getActiveShaderProperties();
        Minecraft minecraft = Minecraft.getMinecraft();
        float shadowDistance = Math.max(16.0f, Math.min(
                properties.getShadowDistance(),
                minecraft.gameSettings.renderDistanceChunks * 16.0f
        ));
        float renderMultiplier = properties.getShadowDistanceRenderMul();
        if (renderMultiplier >= 0.0f) {
            shadowDistance *= renderMultiplier;
        }
        return Math.max(16.0f, shadowDistance);
    }

    public int getShadowTerrainRenderDistanceChunks() {
        return Math.max(1, (int) (this.getShadowTerrainRenderDistance() / 16.0f));
    }

    private ICamera createShadowEntityCamera(ActiniumShaderProperties properties) {
        float entityShadowDistanceMul = properties.getEntityShadowDistanceMul();
        if (entityShadowDistanceMul > 0.0f && entityShadowDistanceMul != 1.0f) {
            return new ShadowDistanceCamera(this.getShadowTerrainRenderDistance() * entityShadowDistanceMul);
        }

        return new ShadowEntityCamera(this.shadowProjectionMatrix, this.shadowModelViewMatrix);
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

    private static FloatBuffer writeMatrix(Matrix4fc matrix, FloatBuffer buffer) {
        buffer.clear();
        matrix.get(buffer);
        return buffer;
    }

    private FloatBuffer toGlMatrixBuffer(Matrix4fc matrix) {
        return writeMatrix(matrix, this.scratchGlMatrixBuffer);
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

    private static String framebufferStatusName(int status) {
        return switch (status) {
            case GL30.GL_FRAMEBUFFER_COMPLETE -> "GL_FRAMEBUFFER_COMPLETE";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
            case GL30.GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
            default -> "UNKNOWN_FRAMEBUFFER_STATUS";
        };
    }

    private static final class ShadowViewportFrustum implements Frustum {
        private static final int[][] NEIGHBORING_PLANES = {
                {2, 3, 4, 5},
                {2, 3, 4, 5},
                {0, 1, 4, 5},
                {0, 1, 4, 5},
                {0, 1, 2, 3},
                {0, 1, 2, 3}
        };
        private static final int MAX_CLIPPING_PLANES = 16;
        private final FrustumIntersection frustum = new FrustumIntersection();
        private final Matrix4f frustumMatrix = new Matrix4f();
        private final Vector4f[] clippingPlanes = new Vector4f[MAX_CLIPPING_PLANES];
        private final float maxDistance;
        private int clippingPlaneCount;

        private ShadowViewportFrustum(Matrix4fc projection,
                                      Matrix4fc modelView,
                                      Matrix4fc playerProjection,
                                      Matrix4fc playerModelView,
                                      Vector3f shadowLightVector,
                                      float shadowDistance) {
            this.maxDistance = Math.max(32.0f, shadowDistance + 32.0f);
            this.frustumMatrix.set(projection).mul(modelView);
            this.frustum.set(this.frustumMatrix, true);
            this.buildShadowCasterPlanes(playerProjection, playerModelView, shadowLightVector);
        }

        @Override
        public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            if (maxX < -this.maxDistance || minX > this.maxDistance
                    || maxY < -this.maxDistance || minY > this.maxDistance
                    || maxZ < -this.maxDistance || minZ > this.maxDistance) {
                return false;
            }

            if (!this.testShadowCasterPlanes(minX, minY, minZ, maxX, maxY, maxZ)) {
                return false;
            }

            return this.frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private void buildShadowCasterPlanes(Matrix4fc playerProjection, Matrix4fc playerModelView, Vector3f shadowLightVector) {
            if (shadowLightVector.lengthSquared() <= 1.0e-6f) {
                return;
            }

            Vector3f normalizedShadowLight = new Vector3f(shadowLightVector).normalize();
            Vector4f[] basePlanes = this.createBaseClippingPlanes(playerProjection, playerModelView);
            boolean[] backPlanes = new boolean[basePlanes.length];

            for (int planeIndex = 0; planeIndex < basePlanes.length; planeIndex++) {
                Vector4f plane = basePlanes[planeIndex];
                float dot = plane.x() * normalizedShadowLight.x() + plane.y() * normalizedShadowLight.y() + plane.z() * normalizedShadowLight.z();
                boolean isBackPlane = dot > 0.0f;
                backPlanes[planeIndex] = isBackPlane;

                if (isBackPlane || dot == 0.0f) {
                    this.addPlane(new Vector4f(plane));
                }
            }

            for (int planeIndex = 0; planeIndex < basePlanes.length; planeIndex++) {
                if (!backPlanes[planeIndex]) {
                    continue;
                }

                Vector4f plane = basePlanes[planeIndex];
                for (int neighbor : NEIGHBORING_PLANES[planeIndex]) {
                    if (!backPlanes[neighbor]) {
                        this.addEdgePlane(plane, basePlanes[neighbor], normalizedShadowLight);
                    }
                }
            }
        }

        private Vector4f[] createBaseClippingPlanes(Matrix4fc playerProjection, Matrix4fc playerModelView) {
            Matrix4f transform = new Matrix4f(playerProjection).mul(playerModelView).transpose();
            return new Vector4f[]{
                    this.transformPlane(transform, -1.0f, 0.0f, 0.0f),
                    this.transformPlane(transform, 1.0f, 0.0f, 0.0f),
                    this.transformPlane(transform, 0.0f, -1.0f, 0.0f),
                    this.transformPlane(transform, 0.0f, 1.0f, 0.0f),
                    this.transformPlane(transform, 0.0f, 0.0f, -1.0f),
                    this.transformPlane(transform, 0.0f, 0.0f, 1.0f)
            };
        }

        private Vector4f transformPlane(Matrix4f transform, float x, float y, float z) {
            return new Vector4f(x, y, z, 1.0f).mul(transform).normalize();
        }

        private void addPlane(Vector4f plane) {
            if (this.clippingPlaneCount >= this.clippingPlanes.length) {
                return;
            }

            this.clippingPlanes[this.clippingPlaneCount++] = plane;
        }

        private void addEdgePlane(Vector4f backPlane, Vector4f frontPlane, Vector3f shadowLightVector) {
            Vector3f backNormal = new Vector3f(backPlane.x(), backPlane.y(), backPlane.z());
            Vector3f frontNormal = new Vector3f(frontPlane.x(), frontPlane.y(), frontPlane.z());
            Vector3f intersection = backNormal.cross(frontNormal, new Vector3f());

            if (intersection.lengthSquared() <= 1.0e-6f) {
                return;
            }

            Vector3f edgePlaneNormal = intersection.cross(shadowLightVector, new Vector3f());

            if (edgePlaneNormal.lengthSquared() <= 1.0e-6f) {
                return;
            }

            Vector3f ixb = intersection.cross(backNormal, new Vector3f()).mul(-frontPlane.w());
            Vector3f fxi = frontNormal.cross(intersection, new Vector3f()).mul(-backPlane.w());
            Vector3f point = ixb.add(fxi).mul(1.0f / intersection.lengthSquared());
            float w = -edgePlaneNormal.dot(point);

            this.addPlane(new Vector4f(edgePlaneNormal, w));
        }

        private boolean testShadowCasterPlanes(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            for (int i = 0; i < this.clippingPlaneCount; i++) {
                Vector4f plane = this.clippingPlanes[i];
                float outsideX = plane.x() < 0.0f ? minX : maxX;
                float outsideY = plane.y() < 0.0f ? minY : maxY;
                float outsideZ = plane.z() < 0.0f ? minZ : maxZ;

                if (Math.fma(plane.x(), outsideX, Math.fma(plane.y(), outsideY, plane.z() * outsideZ)) < -plane.w()) {
                    return false;
                }
            }

            return true;
        }
    }

    private static void bindTexture(int unit, int textureId) {
        setActiveTextureUnit(unit);
        if (unit < GL_STATE_MANAGER_TEXTURE_UNITS) {
            GlStateManager.bindTexture(textureId);
        } else {
            bindTextureDirect(textureId);
        }
    }

    private static void unbindTexture(int unit) {
        setActiveTextureUnit(unit);
        if (unit < GL_STATE_MANAGER_TEXTURE_UNITS) {
            GlStateManager.bindTexture(0);
        } else {
            bindTextureDirect(0);
        }
        setActiveTextureUnit(0);
    }

    private static void setActiveTextureUnit(int unit) {
        setActiveTextureEnum(OpenGlHelper.defaultTexUnit + unit);
    }

    private static void setActiveTextureEnum(int textureEnum) {
        int unit = textureEnum - OpenGlHelper.defaultTexUnit;
        if (unit >= 0 && unit < GL_STATE_MANAGER_TEXTURE_UNITS) {
            GlStateManager.setActiveTexture(textureEnum);
        } else {
            GL13.glActiveTexture(textureEnum);
        }
    }

    private static void bindTextureDirect(int textureId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    private static void syncGlStateManagerToCapturedState(int activeTexture,
                                                          int[] textureBindings2d,
                                                          boolean blendEnabled,
                                                          boolean cullEnabled,
                                                          boolean depthEnabled,
                                                          boolean alphaEnabled,
                                                          boolean fogEnabled,
                                                          boolean texture2dEnabled,
                                                          boolean depthMask,
                                                          boolean[] colorMask,
                                                          float[] currentColor,
                                                          int alphaFunc,
                                                          float alphaRef,
                                                          int shadeModel,
                                                          int depthFunc,
                                                          int blendSrcRgb,
                                                          int blendDstRgb,
                                                          int blendSrcAlpha,
                                                          int blendDstAlpha,
                                                          int viewportX,
                                                          int viewportY,
                                                          int viewportWidth,
                                                          int viewportHeight,
                                                          int matrixMode) {
        for (int unit = 0; unit < Math.min(GL_STATE_MANAGER_TEXTURE_UNITS, textureBindings2d.length); unit++) {
            setActiveTextureUnit(unit);
            GlStateManager.bindTexture(textureBindings2d[unit]);
        }

        setActiveTextureEnum(activeTexture);
        if (activeTexture - OpenGlHelper.defaultTexUnit >= 0 && activeTexture - OpenGlHelper.defaultTexUnit < GL_STATE_MANAGER_TEXTURE_UNITS) {
            if (texture2dEnabled) {
                GlStateManager.enableTexture2D();
            } else {
                GlStateManager.disableTexture2D();
            }
        }

        if (blendEnabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }

        if (cullEnabled) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }

        if (depthEnabled) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }

        if (alphaEnabled) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }

        if (fogEnabled) {
            GlStateManager.enableFog();
        } else {
            GlStateManager.disableFog();
        }

        GlStateManager.depthMask(depthMask);
        GlStateManager.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        GlStateManager.color(currentColor[0], currentColor[1], currentColor[2], currentColor[3]);
        GlStateManager.alphaFunc(alphaFunc, alphaRef);
        GlStateManager.shadeModel(shadeModel);
        GlStateManager.depthFunc(depthFunc);
        GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        GlStateManager.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
        GlStateManager.matrixMode(matrixMode);
        setActiveTextureUnit(0);
    }

    private static final class ShadowDistanceCamera implements ICamera {
        private final double maxDistance;
        private double minAllowedX;
        private double maxAllowedX;
        private double minAllowedY;
        private double maxAllowedY;
        private double minAllowedZ;
        private double maxAllowedZ;

        private ShadowDistanceCamera(double maxDistance) {
            this.maxDistance = maxDistance;
        }

        @Override
        public boolean isBoundingBoxInFrustum(AxisAlignedBB box) {
            return !(box.maxX < this.minAllowedX || box.minX > this.maxAllowedX
                    || box.maxY < this.minAllowedY || box.minY > this.maxAllowedY
                    || box.maxZ < this.minAllowedZ || box.minZ > this.maxAllowedZ);
        }

        @Override
        public void setPosition(double x, double y, double z) {
            this.minAllowedX = x - this.maxDistance;
            this.maxAllowedX = x + this.maxDistance;
            this.minAllowedY = y - this.maxDistance;
            this.maxAllowedY = y + this.maxDistance;
            this.minAllowedZ = z - this.maxDistance;
            this.maxAllowedZ = z + this.maxDistance;
        }
    }

    private static final class ShadowEntityCamera implements ICamera {
        private final FrustumIntersection frustum = new FrustumIntersection();
        private final Matrix4f frustumMatrix = new Matrix4f();
        private double x;
        private double y;
        private double z;

        private ShadowEntityCamera(Matrix4fc projection, Matrix4fc modelView) {
            this.frustumMatrix.set(projection).mul(modelView);
            this.frustum.set(this.frustumMatrix, true);
        }

        @Override
        public boolean isBoundingBoxInFrustum(AxisAlignedBB box) {
            return this.isBoxInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }

        public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            if (Double.isInfinite(minX) || Double.isInfinite(minY) || Double.isInfinite(minZ)
                    || Double.isInfinite(maxX) || Double.isInfinite(maxY) || Double.isInfinite(maxZ)) {
                return true;
            }

            return this.frustum.testAab(
                    (float) (minX - this.x),
                    (float) (minY - this.y),
                    (float) (minZ - this.z),
                    (float) (maxX - this.x),
                    (float) (maxY - this.y),
                    (float) (maxZ - this.z)
            );
        }

        @Override
        public void setPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class ShadowPassGlState {
        private static final IntBuffer SCRATCH_COLOR_MASK_BUFFER = BufferUtils.createIntBuffer(16);
        private static final FloatBuffer SCRATCH_ALPHA_REF_BUFFER = BufferUtils.createFloatBuffer(16);
        private static final FloatBuffer SCRATCH_LINE_WIDTH_BUFFER = BufferUtils.createFloatBuffer(16);
        private static final FloatBuffer SCRATCH_CURRENT_COLOR_BUFFER = BufferUtils.createFloatBuffer(16);
        private static final IntBuffer SCRATCH_POLYGON_MODE_BUFFER = BufferUtils.createIntBuffer(16);
        private static final IntBuffer SCRATCH_SCISSOR_BOX_BUFFER = BufferUtils.createIntBuffer(16);

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

            SCRATCH_COLOR_MASK_BUFFER.clear();
            invokeGlGetInteger(GL11.GL_COLOR_WRITEMASK, SCRATCH_COLOR_MASK_BUFFER);
            SCRATCH_ALPHA_REF_BUFFER.clear();
            invokeGlGetFloat(GL11.GL_ALPHA_TEST_REF, SCRATCH_ALPHA_REF_BUFFER);
            SCRATCH_LINE_WIDTH_BUFFER.clear();
            invokeGlGetFloat(GL11.GL_LINE_WIDTH, SCRATCH_LINE_WIDTH_BUFFER);
            SCRATCH_CURRENT_COLOR_BUFFER.clear();
            invokeGlGetFloat(GL11.GL_CURRENT_COLOR, SCRATCH_CURRENT_COLOR_BUFFER);
            SCRATCH_POLYGON_MODE_BUFFER.clear();
            invokeGlGetInteger(GL11.GL_POLYGON_MODE, SCRATCH_POLYGON_MODE_BUFFER);
            SCRATCH_SCISSOR_BOX_BUFFER.clear();
            invokeGlGetInteger(GL11.GL_SCISSOR_BOX, SCRATCH_SCISSOR_BOX_BUFFER);
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
                    SCRATCH_POLYGON_MODE_BUFFER.get(0),
                    SCRATCH_POLYGON_MODE_BUFFER.get(1),
                    SCRATCH_LINE_WIDTH_BUFFER.get(0),
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
                            SCRATCH_SCISSOR_BOX_BUFFER.get(0),
                            SCRATCH_SCISSOR_BOX_BUFFER.get(1),
                            SCRATCH_SCISSOR_BOX_BUFFER.get(2),
                            SCRATCH_SCISSOR_BOX_BUFFER.get(3)
                    },
                    new boolean[]{
                            SCRATCH_COLOR_MASK_BUFFER.get(0) != 0,
                            SCRATCH_COLOR_MASK_BUFFER.get(1) != 0,
                            SCRATCH_COLOR_MASK_BUFFER.get(2) != 0,
                            SCRATCH_COLOR_MASK_BUFFER.get(3) != 0
                    },
                    new float[]{
                            SCRATCH_CURRENT_COLOR_BUFFER.get(0),
                            SCRATCH_CURRENT_COLOR_BUFFER.get(1),
                            SCRATCH_CURRENT_COLOR_BUFFER.get(2),
                            SCRATCH_CURRENT_COLOR_BUFFER.get(3)
                    },
                    GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),
                    SCRATCH_ALPHA_REF_BUFFER.get(0),
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
                setActiveTextureUnit(unit);
                if (unit < GL_STATE_MANAGER_TEXTURE_UNITS) {
                    GlStateManager.bindTexture(this.textureBindings2d[unit]);
                } else {
                    bindTextureDirect(this.textureBindings2d[unit]);
                }
            }

            setActiveTextureEnum(this.activeTexture);
            if (this.activeTexture - OpenGlHelper.defaultTexUnit < GL_STATE_MANAGER_TEXTURE_UNITS) {
                GlStateManager.bindTexture(this.textureBinding2d);
            } else {
                bindTextureDirect(this.textureBinding2d);
            }
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
            setActiveTextureUnit(0);
            syncGlStateManagerToCapturedState(
                    this.activeTexture,
                    this.textureBindings2d,
                    this.blendEnabled,
                    this.cullEnabled,
                    this.depthEnabled,
                    this.alphaEnabled,
                    this.fogEnabled,
                    this.texture2dEnabled,
                    this.depthMask,
                    this.colorMask,
                    this.currentColor,
                    this.alphaFunc,
                    this.alphaRef,
                    this.shadeModel,
                    this.depthFunc,
                    this.blendSrcRgb,
                    this.blendDstRgb,
                    this.blendSrcAlpha,
                    this.blendDstAlpha,
                    0,
                    0,
                    Math.max(1, fallbackWidth),
                    Math.max(1, fallbackHeight),
                    this.matrixMode
            );
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
        private static final IntBuffer SCRATCH_COLOR_MASK_BUFFER = BufferUtils.createIntBuffer(16);
        private static final FloatBuffer SCRATCH_ALPHA_REF_BUFFER = BufferUtils.createFloatBuffer(16);
        private static final FloatBuffer SCRATCH_CURRENT_COLOR_BUFFER = BufferUtils.createFloatBuffer(16);
        private static final IntBuffer SCRATCH_VIEWPORT_BUFFER = BufferUtils.createIntBuffer(16);

        private final int framebufferBinding;
        private final int drawBuffer;
        private final int readBuffer;
        private final int activeTexture;
        private final int[] textureBindings2d;
        private final int currentProgram;
        private final int matrixMode;
        private final int shadeModel;
        private final int depthFunc;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final boolean blendEnabled;
        private final boolean cullEnabled;
        private final boolean depthEnabled;
        private final boolean alphaEnabled;
        private final boolean fogEnabled;
        private final boolean texture2dEnabled;
        private final boolean depthMask;
        private final boolean[] colorMask;
        private final float[] currentColor;
        private final int alphaFunc;
        private final float alphaRef;
        private final int[] viewport;

        private WorldStageGlState(int framebufferBinding,
                                  int drawBuffer,
                                  int readBuffer,
                                  int activeTexture,
                                  int[] textureBindings2d,
                                  int currentProgram,
                                  int matrixMode,
                                  int shadeModel,
                                  int depthFunc,
                                  int blendSrcRgb,
                                  int blendDstRgb,
                                  int blendSrcAlpha,
                                  int blendDstAlpha,
                                  boolean blendEnabled,
                                  boolean cullEnabled,
                                  boolean depthEnabled,
                                  boolean alphaEnabled,
                                  boolean fogEnabled,
                                  boolean texture2dEnabled,
                                  boolean depthMask,
                                  boolean[] colorMask,
                                  float[] currentColor,
                                  int alphaFunc,
                                  float alphaRef,
                                  int[] viewport) {
            this.framebufferBinding = framebufferBinding;
            this.drawBuffer = drawBuffer;
            this.readBuffer = readBuffer;
            this.activeTexture = activeTexture;
            this.textureBindings2d = textureBindings2d;
            this.currentProgram = currentProgram;
            this.matrixMode = matrixMode;
            this.shadeModel = shadeModel;
            this.depthFunc = depthFunc;
            this.blendSrcRgb = blendSrcRgb;
            this.blendDstRgb = blendDstRgb;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.blendEnabled = blendEnabled;
            this.cullEnabled = cullEnabled;
            this.depthEnabled = depthEnabled;
            this.alphaEnabled = alphaEnabled;
            this.fogEnabled = fogEnabled;
            this.texture2dEnabled = texture2dEnabled;
            this.depthMask = depthMask;
            this.colorMask = colorMask;
            this.currentColor = currentColor;
            this.alphaFunc = alphaFunc;
            this.alphaRef = alphaRef;
            this.viewport = viewport;
        }

        public static WorldStageGlState capture() {
            SCRATCH_COLOR_MASK_BUFFER.clear();
            invokeGlGetInteger(GL11.GL_COLOR_WRITEMASK, SCRATCH_COLOR_MASK_BUFFER);
            SCRATCH_ALPHA_REF_BUFFER.clear();
            invokeGlGetFloat(GL11.GL_ALPHA_TEST_REF, SCRATCH_ALPHA_REF_BUFFER);
            SCRATCH_CURRENT_COLOR_BUFFER.clear();
            invokeGlGetFloat(GL11.GL_CURRENT_COLOR, SCRATCH_CURRENT_COLOR_BUFFER);
            SCRATCH_VIEWPORT_BUFFER.clear();
            invokeGlGetInteger(GL11.GL_VIEWPORT, SCRATCH_VIEWPORT_BUFFER);
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
                    GL11.glGetInteger(GL11.GL_MATRIX_MODE),
                    GL11.glGetInteger(GL11.GL_SHADE_MODEL),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                    GL11.glIsEnabled(GL11.GL_FOG),
                    GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    new boolean[]{
                            SCRATCH_COLOR_MASK_BUFFER.get(0) != 0,
                            SCRATCH_COLOR_MASK_BUFFER.get(1) != 0,
                            SCRATCH_COLOR_MASK_BUFFER.get(2) != 0,
                            SCRATCH_COLOR_MASK_BUFFER.get(3) != 0
                    },
                    new float[]{
                            SCRATCH_CURRENT_COLOR_BUFFER.get(0),
                            SCRATCH_CURRENT_COLOR_BUFFER.get(1),
                            SCRATCH_CURRENT_COLOR_BUFFER.get(2),
                            SCRATCH_CURRENT_COLOR_BUFFER.get(3)
                    },
                    GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),
                    SCRATCH_ALPHA_REF_BUFFER.get(0),
                    new int[]{
                            SCRATCH_VIEWPORT_BUFFER.get(0),
                            SCRATCH_VIEWPORT_BUFFER.get(1),
                            SCRATCH_VIEWPORT_BUFFER.get(2),
                            SCRATCH_VIEWPORT_BUFFER.get(3)
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
                setActiveTextureUnit(unit);
                if (unit < GL_STATE_MANAGER_TEXTURE_UNITS) {
                    GlStateManager.bindTexture(this.textureBindings2d[unit]);
                } else {
                    bindTextureDirect(this.textureBindings2d[unit]);
                }
            }

            setActiveTextureEnum(this.activeTexture);
            GL11.glMatrixMode(this.matrixMode);
            GL11.glShadeModel(this.shadeModel);
            GL11.glDepthFunc(this.depthFunc);
            GL14.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
            setEnabled(GL11.GL_BLEND, this.blendEnabled);
            setEnabled(GL11.GL_CULL_FACE, this.cullEnabled);
            setEnabled(GL11.GL_DEPTH_TEST, this.depthEnabled);
            setEnabled(GL11.GL_ALPHA_TEST, this.alphaEnabled);
            setEnabled(GL11.GL_FOG, this.fogEnabled);
            setEnabled(GL11.GL_TEXTURE_2D, this.texture2dEnabled);
            GL11.glDepthMask(this.depthMask);
            GL11.glColorMask(this.colorMask[0], this.colorMask[1], this.colorMask[2], this.colorMask[3]);
            GL11.glColor4f(this.currentColor[0], this.currentColor[1], this.currentColor[2], this.currentColor[3]);
            GL11.glAlphaFunc(this.alphaFunc, this.alphaRef);
            GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
            syncGlStateManagerToCapturedState(
                    this.activeTexture,
                    this.textureBindings2d,
                    this.blendEnabled,
                    this.cullEnabled,
                    this.depthEnabled,
                    this.alphaEnabled,
                    this.fogEnabled,
                    this.texture2dEnabled,
                    this.depthMask,
                    this.colorMask,
                    this.currentColor,
                    this.alphaFunc,
                    this.alphaRef,
                    this.shadeModel,
                    this.depthFunc,
                    this.blendSrcRgb,
                    this.blendDstRgb,
                    this.blendSrcAlpha,
                    this.blendDstAlpha,
                    this.viewport[0],
                    this.viewport[1],
                    this.viewport[2],
                    this.viewport[3],
                    this.matrixMode
            );
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }

    @FunctionalInterface
    private interface PreSceneTargetInitializer {
        void initialize(Framebuffer mainFramebuffer);
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
                        ConditionalFrame frame = stack.getLast();
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
                        ConditionalFrame frame = stack.getLast();
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
                        ConditionalFrame frame = stack.removeLast();
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

            if ("shadowcolor0".equals(bufferName) || "shadowcolor".equals(bufferName)) {
                return POST_SHADOW_COLOR0_UNIT;
            }

            if ("shadowcolor1".equals(bufferName)) {
                return POST_SHADOW_COLOR1_UNIT;
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

            if (trimmed.startsWith("defined")) {
                String definedTarget = trimmed.substring("defined".length()).trim();

                if (!definedTarget.isEmpty()) {
                    definedTarget = trimOuterParentheses(definedTarget);

                    if (!definedTarget.isEmpty() && isIdentifier(definedTarget)) {
                        return defines.containsKey(definedTarget);
                    }
                }
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

        private static boolean isIdentifier(String value) {
            if (value.isEmpty()) {
                return false;
            }

            char first = value.charAt(0);
            if (!(Character.isLetter(first) || first == '_')) {
                return false;
            }

            for (int i = 1; i < value.length(); i++) {
                char c = value.charAt(i);
                if (!(Character.isLetterOrDigit(c) || c == '_')) {
                    return false;
                }
            }

            return true;
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
