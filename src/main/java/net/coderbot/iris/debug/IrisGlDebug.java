package net.coderbot.iris.debug;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.coderbot.iris.rendertarget.RenderTarget;
import net.coderbot.iris.rendertarget.RenderTargets;
import org.taumc.celeritas.CeleritasVintage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class IrisGlDebug {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumGLDebug");
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(4);
    private static final Set<String> LOGGED_CELERITAS_PROGRAMS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_CELERITAS_STATES = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_FULLSCREEN_PROGRAMS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_FULLSCREEN_STATES = ConcurrentHashMap.newKeySet();
	private static final Map<String, Integer> FRAMEBUFFER_SAMPLE_COUNTS = new ConcurrentHashMap<>();
	private static final Map<String, Integer> TEXTURE_SAMPLE_COUNTS = new ConcurrentHashMap<>();
	private static final Map<String, Integer> MATERIAL_SAMPLE_COUNTS = new ConcurrentHashMap<>();
	private static final Map<String, Integer> ENTITY_PHASE_SAMPLE_COUNTS = new ConcurrentHashMap<>();
	private static final Map<String, Integer> SAMPLER_INIT_COUNTS = new ConcurrentHashMap<>();
	private static long lastShadowEntityLogTime;
    private static String lastStage = "startup";
    private static String framebufferSamplePhase;
    private static long lastLogTime;
    private static int debugFramebuffer;

    private IrisGlDebug() {
    }

    public static boolean isEnabled() {
        try {
            return CeleritasVintage.options().debug.enableActiniumGlDebug;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

	public static void logDebugInfo(String message, Object... params) {
		if (shouldCaptureGlState()) {
			LOGGER.info(message, params);
		}
	}

	private static boolean shouldCaptureGlState() {
		if (!isEnabled() || !Iris.isWorldReadyForShaderpackLoad()) {
			return false;
		}

		return true;
	}

	public static void logSamplerInitialization(int program, String mode, String name, int location, int assignedUnit) {
		if (!shouldCaptureGlState()) {
            return;
        }

		String label = "sampler-init:" + program + ":" + mode + ":" + name + ":" + assignedUnit;
		int count = SAMPLER_INIT_COUNTS.merge(label, 1, Integer::sum);
		if (count > 4) {
			return;
		}

		logDebugInfo(
			"sampler-init program={} mode={} name={} count={} location={} unit={}",
			program,
			mode,
			name,
			count,
			location,
			assignedUnit
		);
	}

	public static void logSamplerIntercept(int program, String mode, int requestedUnit, boolean override, String... names) {
		if (!shouldCaptureGlState()) {
            return;
        }

		String joinedNames = String.join(",", names);
		String label = "sampler-intercept:" + program + ":" + mode + ":" + requestedUnit + ":" + override + ":" + joinedNames;
		int count = SAMPLER_INIT_COUNTS.merge(label, 1, Integer::sum);
		if (count > 4) {
			return;
		}

		logDebugInfo(
			"sampler-intercept program={} mode={} requestedUnit={} override={} count={} names=[{}]",
			program,
			mode,
			requestedUnit,
			override,
			count,
			joinedNames
		);
	}

	public static void logPipelineInputs(String stage, String phase, String availability, boolean shadow, boolean mainBound, boolean fullscreen, boolean postChain) {
		if (!shouldCaptureGlState()) {
            return;
        }

		String label = "pipeline-inputs:" + stage + ":" + phase + ":" + availability + ":" + shadow + ":" + mainBound + ":" + fullscreen + ":" + postChain;
		int count = SAMPLER_INIT_COUNTS.merge(label, 1, Integer::sum);
		if (count > 8) {
			return;
		}

		logDebugInfo(
			"pipeline-inputs stage={} phase={} availability={} count={} shadow={} mainBound={} fullscreen={} postChain={}",
			stage,
			phase,
			availability,
			count,
			shadow,
			mainBound,
			fullscreen,
			postChain
		);
	}

	public static void logProgramSamplerState(String stage, int program, String availability, String phase) {
		if (!shouldCaptureGlState()) {
            return;
        }

		String label = "program-sampler-state:" + stage + ":" + program + ":" + availability + ":" + phase;
		int count = SAMPLER_INIT_COUNTS.merge(label, 1, Integer::sum);
		if (count > 4) {
			return;
		}

		logDebugInfo(
			"program-sampler-state stage={} program={} phase={} availability={} count={} values[tex={}, lightmap={}, gaux4={}, shadowtex0={}, shadowtex1={}, shadowcolor0={}]",
			stage,
			program,
			phase,
			availability,
			count,
			getSamplerUniform(program, "tex"),
			getSamplerUniform(program, "lightmap"),
			getSamplerUniform(program, "gaux4"),
			getSamplerUniform(program, "shadowtex0"),
			getSamplerUniform(program, "shadowtex1"),
			getSamplerUniform(program, "shadowcolor0")
		);
	}

    public static void markStage(String stage) {
        lastStage = stage;
    }

    public static void beginFramebufferSamplePhase(String phase) {
        framebufferSamplePhase = phase;
    }

    public static void endFramebufferSamplePhase() {
        framebufferSamplePhase = null;
    }

    public static void logMinecraftGlError(String message, int error) {
        if (error == 0) {
            return;
        }

        if (!shouldCaptureGlState()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastLogTime < 1000L) {
            return;
        }
        lastLogTime = now;

        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer main = mc.getFramebuffer();
        int mainFbo = main != null ? main.framebufferObject : -1;
        int mainTex = main != null ? main.framebufferTexture : -1;
        int mainWidth = main != null ? main.framebufferWidth : -1;
        int mainHeight = main != null ? main.framebufferHeight : -1;

        LOGGER.error(
            "message={} error={} lastStage={} fb={} readFb={} drawFb={} drawBuffer={} readBuffer={} program={} vao={} viewport=[{},{},{},{}] mainFbo={} mainTex={} mainSize={}x{} display={}x{}",
            message,
            error,
            lastStage,
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
            GL11.glGetInteger(GL11.GL_READ_BUFFER),
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3),
            mainFbo,
            mainTex,
            mainWidth,
            mainHeight,
            mc.displayWidth,
            mc.displayHeight
        );
    }

    public static void check(String stage) {
        if (!shouldCaptureGlState()) {
            return;
        }

        markStage(stage);
        int error = GL11.glGetError();
        if (error != 0) {
            logGlState(stage, error);
        }
    }

    public static void logCeleritasProgram(String passName, int program, Collection<GlVertexAttribute> attributes) {
        if (!shouldCaptureGlState()) {
            return;
        }

        if (!LOGGED_CELERITAS_PROGRAMS.add(passName + ":" + program)) {
            return;
        }

        String attributeLocations = attributes.stream()
            .map(attribute -> attribute.getName() + "=" + GL20.glGetAttribLocation(program, attribute.getName()))
            .collect(Collectors.joining(", "));

        logDebugInfo(
            "celeritas-program pass={} program={} attrs=[{}] uniforms=[tex={}, lightmap={}, colortex0={}, gaux4={}, shadowtex0={}, shadowtex1={}, shadowcolor0={}, iris_ModelViewMatrix={}, iris_ProjectionMatrix={}, iris_TextureMatrix={}, iris_LightmapTextureMatrix={}, iris_NormalMatrix={}, u_RegionOffset={}, frameMod={}, frameCounter={}, cameraPosition={}, previousCameraPosition={}, sunPosition={}, moonPosition={}, viewWidth={}, viewHeight={}, pixelSizeX={}, pixelSizeY={}, worldTime={}, gbufferModelView={}, gbufferProjection={}, gbufferPreviousModelView={}, gbufferPreviousProjection={}]",
            passName,
            program,
            attributeLocations,
            GL20.glGetUniformLocation(program, "tex"),
            GL20.glGetUniformLocation(program, "lightmap"),
            GL20.glGetUniformLocation(program, "colortex0"),
            GL20.glGetUniformLocation(program, "gaux4"),
            GL20.glGetUniformLocation(program, "shadowtex0"),
            GL20.glGetUniformLocation(program, "shadowtex1"),
            GL20.glGetUniformLocation(program, "shadowcolor0"),
            GL20.glGetUniformLocation(program, "iris_ModelViewMatrix"),
            GL20.glGetUniformLocation(program, "iris_ProjectionMatrix"),
            GL20.glGetUniformLocation(program, "iris_TextureMatrix"),
            GL20.glGetUniformLocation(program, "iris_LightmapTextureMatrix"),
            GL20.glGetUniformLocation(program, "iris_NormalMatrix"),
            GL20.glGetUniformLocation(program, "u_RegionOffset"),
            GL20.glGetUniformLocation(program, "frameMod"),
            GL20.glGetUniformLocation(program, "frameCounter"),
            GL20.glGetUniformLocation(program, "cameraPosition"),
            GL20.glGetUniformLocation(program, "previousCameraPosition"),
            GL20.glGetUniformLocation(program, "sunPosition"),
            GL20.glGetUniformLocation(program, "moonPosition"),
            GL20.glGetUniformLocation(program, "viewWidth"),
            GL20.glGetUniformLocation(program, "viewHeight"),
            GL20.glGetUniformLocation(program, "pixelSizeX"),
            GL20.glGetUniformLocation(program, "pixelSizeY"),
            GL20.glGetUniformLocation(program, "worldTime"),
            GL20.glGetUniformLocation(program, "gbufferModelView"),
            GL20.glGetUniformLocation(program, "gbufferProjection"),
            GL20.glGetUniformLocation(program, "gbufferPreviousModelView"),
            GL20.glGetUniformLocation(program, "gbufferPreviousProjection")
        );
    }

    public static void logCeleritasTerrainState(String passName, int program) {
        if (!shouldCaptureGlState()) {
            return;
        }

        if (!LOGGED_CELERITAS_STATES.add(passName + ":" + program)) {
            return;
        }

        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) - GL13.GL_TEXTURE0;
        int texSampler = getSamplerUniform(program, "tex");
        int lightmapSampler = getSamplerUniform(program, "lightmap");
        int shadow0Sampler = getSamplerUniform(program, "shadowtex0");
        int shadow1Sampler = getSamplerUniform(program, "shadowtex1");
        int shadowColorSampler = getSamplerUniform(program, "shadowcolor0");
        int texture0 = getBoundTexture2D(0);
        int texture1 = getBoundTexture2D(1);
        int texture2 = getBoundTexture2D(2);
        int texture3 = getBoundTexture2D(3);
        int frameMod = getIntUniform(program, "frameMod");
        int frameCounter = getIntUniform(program, "frameCounter");
        float viewWidth = getFloatUniform(program, "viewWidth");
        float viewHeight = getFloatUniform(program, "viewHeight");
        float pixelSizeX = getFloatUniform(program, "pixelSizeX");
        float pixelSizeY = getFloatUniform(program, "pixelSizeY");
        float alphaTest = getFloatUniform(program, "iris_currentAlphaTest");
        String textureMatrix = getMatrixUniform(program, "iris_TextureMatrix");
        String lightmapTextureMatrix = getMatrixUniform(program, "iris_LightmapTextureMatrix");

        logDebugInfo(
            "celeritas-state pass={} program={} currentProgram={} fb={} readFb={} drawFb={} drawBuffer={} readBuffer={} viewport=[{},{},{},{}] activeTex={} samplers[tex={}, lightmap={}, shadow0={}, shadow1={}, shadowColor0={}] textures[0={}, 1={}, 2={}, 3={}] values[frameMod={}, frameCounter={}, viewWidth={}, viewHeight={}, pixelSizeX={}, pixelSizeY={}, alphaTest={}] matrices[tex={}, lightmap={}]",
            passName,
            program,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
            GL11.glGetInteger(GL11.GL_READ_BUFFER),
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3),
            activeTexture,
            texSampler,
            lightmapSampler,
            shadow0Sampler,
            shadow1Sampler,
            shadowColorSampler,
            texture0,
            texture1,
            texture2,
            texture3,
            frameMod,
            frameCounter,
            viewWidth,
            viewHeight,
            pixelSizeX,
            pixelSizeY,
            alphaTest,
            textureMatrix,
            lightmapTextureMatrix
        );

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + activeTexture);
    }

    public static void logTerrainMaterialSample(String source, int blockId, int renderType, int lightValue, int localX, int localY, int localZ, float u, float v) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = source + ":" + blockId + ":" + renderType;
        int count = MATERIAL_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "terrain-material source={} count={} blockId={} renderType={} lightValue={} local=[{},{},{}] uv=[{},{}]",
            source,
            count,
            blockId,
            renderType,
            lightValue,
            localX,
            localY,
            localZ,
            u,
            v
        );
    }

    public static void logShadowEntityState(String stage, double cameraX, double cameraY, double cameraZ, double renderPosX, double renderPosY, double renderPosZ, double viewerPosX, double viewerPosY, double viewerPosZ, int entityCount) {
        if (!shouldCaptureGlState()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastShadowEntityLogTime < 1000L) {
            return;
        }
        lastShadowEntityLogTime = now;

        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        logDebugInfo(
            "shadow-entity-state stage={} camera=[{},{},{}] renderPos=[{},{},{}] viewerPos=[{},{},{}] entities={} modelviewDepth={} projectionDepth={} fb={} viewport=[{},{},{},{}]",
            stage,
            cameraX,
            cameraY,
            cameraZ,
            renderPosX,
            renderPosY,
            renderPosZ,
            viewerPosX,
            viewerPosY,
            viewerPosZ,
            entityCount,
            GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH),
            GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3)
        );
    }

    public static void logShadowEntityDraw(String entityType, double x, double y, double z, float yaw, float partialTicks) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = entityType + ":" + GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 6) {
            return;
        }

        logDebugInfo(
            "shadow-entity-draw entity={} count={} relative=[{},{},{}] yaw={} partialTicks={} program={} fb={} modelview={} projection={}",
            entityType,
            count,
            x,
            y,
            z,
            yaw,
            partialTicks,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            getMatrixModeMatrix(GL11.GL_MODELVIEW_MATRIX),
            getMatrixModeMatrix(GL11.GL_PROJECTION_MATRIX)
        );
    }

    public static void logEntityCullSample(String reason, String entityType, int pass) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "entity-cull:" + reason + ":" + entityType + ":" + pass;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 4) {
            return;
        }

        logDebugInfo(
            "entity-cull reason={} entity={} pass={} count={} program={} fb={} viewport=[{},{},{},{}]",
            reason,
            entityType,
            pass,
            count,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logEntityLoopSummary(String stage, int pass, int gathered, int rendered, int shadowSkipped, int frustumSkipped, int celeritasSkipped, int blockSkipped, boolean irisEntities, String phase) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "entity-loop:" + stage + ":" + pass + ":" + gathered + ":" + rendered + ":" + shadowSkipped + ":" + frustumSkipped + ":" + celeritasSkipped + ":" + blockSkipped;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 6) {
            return;
        }

        logDebugInfo(
            "entity-loop stage={} pass={} count={} gathered={} rendered={} skipped[shadow={}, frustum={}, celeritas={}, block={}] irisEntities={} phase={} program={} fb={} viewport=[{},{},{},{}]",
            stage,
            pass,
            count,
            gathered,
            rendered,
            shadowSkipped,
            frustumSkipped,
            celeritasSkipped,
            blockSkipped,
            irisEntities,
            phase,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logEntityPhase(String entityType, String previousPhase, boolean beganEntityPhase, String uniformModelView, String uniformProjection) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = entityType + ":" + previousPhase + ":" + beganEntityPhase;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "entity-phase entity={} count={} previousPhase={} beganEntityPhase={} program={} fb={} viewport=[{},{},{},{}] uniforms[modelView={}, projection={}]",
            entityType,
            count,
            previousPhase,
            beganEntityPhase,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight(),
            uniformModelView,
            uniformProjection
        );
    }

    public static void logEntityRenderCall(String stage, String entityType, String rendererType, String phase, int entityId) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "entity-render:" + stage + ":" + entityType + ":" + rendererType;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "entity-render stage={} entity={} renderer={} phase={} entityId={} count={} program={} fb={} viewport=[{},{},{},{}]",
            stage,
            entityType,
            rendererType,
            phase,
            entityId,
            count,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logWorldPassState(String stage, String phase, String subject) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "world-pass-state:" + stage + ":" + phase + ":" + subject;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        final var blend = GLStateManager.getBlendState();
        final var alpha = GLStateManager.getAlphaState();
        final var depth = GLStateManager.getDepthState();
        final var alphaTest = GLStateManager.getAlphaTest();
        final var colorMask = GLStateManager.getColorMask();

        logDebugInfo(
            "world-pass-state stage={} subject={} phase={} count={} program={} fb={} viewport=[{},{},{},{}] blend[enabled={}, srcRgb={}, dstRgb={}, srcAlpha={}, dstAlpha={}] alphaTest[enabled={}, func={}, ref={}] depth[enabled={}, func={}, mask={}] colorMask=[{},{},{},{}] cullEnabled={}",
            stage,
            subject,
            phase,
            count,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight(),
            blend.isEnabled(),
            blend.getSrcRgb(),
            blend.getDstRgb(),
            blend.getSrcAlpha(),
            blend.getDstAlpha(),
            alphaTest.isEnabled(),
            alpha.getFunction(),
            alpha.getReference(),
            depth.isEnabled(),
            depth.getFunc(),
            GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
            colorMask.red,
            colorMask.green,
            colorMask.blue,
            colorMask.alpha,
            GL11.glIsEnabled(GL11.GL_CULL_FACE)
        );
    }

    public static void logActiveTextureBindings(String stage, String phase, String subject) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "texture-bindings:" + stage + ":" + phase + ":" + subject;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) - GL13.GL_TEXTURE0;
        int texUnit = getSamplerUniform(program, "tex");
        int lightmapUnit = getSamplerUniform(program, "lightmap");

        logDebugInfo(
            "texture-bindings stage={} subject={} phase={} count={} program={} activeTex={} samplers[tex={}, lightmap={}] textures[0={}, 1={}, 2={}, 3={}]",
            stage,
            subject,
            phase,
            count,
            program,
            activeTexture,
            texUnit,
            lightmapUnit,
            getBoundTexture2D(0),
            getBoundTexture2D(1),
            getBoundTexture2D(2),
            getBoundTexture2D(3)
        );
    }

    public static void logPipelineSkip(String stage, String phase, boolean shadow, boolean mainBound, boolean renderingWorld, boolean fullscreen, boolean postChain) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "pipeline-skip:" + stage + ":" + phase + ":" + shadow + ":" + mainBound + ":" + renderingWorld + ":" + fullscreen + ":" + postChain;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "pipeline-skip stage={} count={} phase={} shadow={} mainBound={} world={} fullscreen={} postChain={} currentProgram={} fb={} viewport=[{},{},{},{}]",
            stage,
            count,
            phase,
            shadow,
            mainBound,
            renderingWorld,
            fullscreen,
            postChain,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logPassBind(String stage, String phase, int previousProgram, int nextProgram) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "pass-bind:" + stage + ":" + phase + ":" + previousProgram + ":" + nextProgram;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "pass-bind stage={} count={} phase={} previousProgram={} nextProgram={} currentProgram={} fb={} viewport=[{},{},{},{}]",
            stage,
            count,
            phase,
            previousProgram,
            nextProgram,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logPhaseChange(String stage, String previousPhase, String nextPhase, boolean shadow, boolean mainBound, boolean renderingWorld, boolean fullscreen, boolean postChain, String inputs) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "phase-change:" + stage + ":" + previousPhase + ":" + nextPhase + ":" + shadow + ":" + mainBound + ":" + fullscreen + ":" + postChain + ":" + inputs;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "phase-change stage={} count={} previousPhase={} nextPhase={} inputs={} shadow={} mainBound={} world={} fullscreen={} postChain={} currentProgram={} fb={} viewport=[{},{},{},{}]",
            stage,
            count,
            previousPhase,
            nextPhase,
            inputs,
            shadow,
            mainBound,
            renderingWorld,
            fullscreen,
            postChain,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logProgramOverrideDecision(String stage, String phase, int oldProgram, int newProgram, int activePassProgram, boolean shouldOverrideShaders, boolean renderingLevel, boolean ownedProgram, boolean unlockedDepthColor, boolean invokedOverride) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "program-override:" + stage + ":" + phase + ":" + oldProgram + ":" + newProgram + ":" + activePassProgram + ":" + shouldOverrideShaders + ":" + renderingLevel + ":" + ownedProgram + ":" + unlockedDepthColor + ":" + invokedOverride;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 12) {
            return;
        }

        logDebugInfo(
            "program-override stage={} count={} phase={} oldProgram={} newProgram={} activePassProgram={} shouldOverride={} renderingLevel={} ownedProgram={} unlockedDepthColor={} invokedOverride={} currentProgram={} fb={} viewport=[{},{},{},{}]",
            stage,
            count,
            phase,
            oldProgram,
            newProgram,
            activePassProgram,
            shouldOverrideShaders,
            renderingLevel,
            ownedProgram,
            unlockedDepthColor,
            invokedOverride,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logModProgramOverride(String stage, String phase, String inputs, boolean shadow, boolean mainBound, boolean renderingWorld, boolean fullscreen, boolean postChain, int previousProgram, int activePassProgram) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "mod-program-override:" + stage + ":" + phase + ":" + previousProgram + ":" + activePassProgram + ":" + shadow + ":" + mainBound + ":" + fullscreen + ":" + postChain + ":" + inputs;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        logDebugInfo(
            "mod-program-override stage={} count={} phase={} inputs={} previousProgram={} activePassProgram={} shadow={} mainBound={} world={} fullscreen={} postChain={} currentProgram={} fb={} viewport=[{},{},{},{}]",
            stage,
            count,
            phase,
            inputs,
            previousProgram,
            activePassProgram,
            shadow,
            mainBound,
            renderingWorld,
            fullscreen,
            postChain,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight()
        );
    }

    public static void logCurrentFramebufferAttachments(String stage, String phase, int maxAttachments) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = "fb-attachments:" + stage + ":" + phase;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        StringBuilder attachments = new StringBuilder();
        for (int i = 0; i < maxAttachments; i++) {
            int objectType = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0 + i,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
            );
            int objectName = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0 + i,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
            );
            attachments.append(" att").append(i)
                .append("[type=").append(objectType)
                .append(", tex=").append(objectName)
                .append("]");
        }

        int depthType = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_ATTACHMENT,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );
        int depthName = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_ATTACHMENT,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        );

        logDebugInfo(
            "fb-attachments stage={} phase={} count={} fb={} drawBuffer={} readBuffer={}{} depth[type={}, tex={}]",
            stage,
            phase,
            count,
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
            GL11.glGetInteger(GL11.GL_READ_BUFFER),
            attachments,
            depthType,
            depthName
        );
    }

    public static void logPipelineMatch(String stage, String phase, String condition, boolean shadow, boolean mainBound, boolean renderingWorld, boolean fullscreen, boolean postChain, int program) {
        if (!shouldCaptureGlState()) {
            return;
        }

        String label = stage + ":" + phase + ":" + condition + ":" + shadow + ":" + mainBound + ":" + program;
        int count = ENTITY_PHASE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 8) {
            return;
        }

        final var blend = GLStateManager.getBlendState();

        logDebugInfo(
            "pipeline-match stage={} count={} phase={} condition={} shadow={} mainBound={} world={} fullscreen={} postChain={} program={} currentProgram={} fb={} viewport=[{},{},{},{}] blendEnabled={} blend=[{},{},{},{}]",
            stage,
            count,
            phase,
            condition,
            shadow,
            mainBound,
            renderingWorld,
            fullscreen,
            postChain,
            program,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            currentViewportX(),
            currentViewportY(),
            currentViewportWidth(),
            currentViewportHeight(),
            GLStateManager.getBlendMode().isEnabled(),
            blend.getSrcRgb(),
            blend.getDstRgb(),
            blend.getSrcAlpha(),
            blend.getDstAlpha()
        );
    }

    private static int currentViewportX() {
        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);
        return VIEWPORT.get(0);
    }

    private static int currentViewportY() {
        return VIEWPORT.get(1);
    }

    private static int currentViewportWidth() {
        return VIEWPORT.get(2);
    }

    private static int currentViewportHeight() {
        return VIEWPORT.get(3);
    }

    public static void logFullscreenProgram(String stageName, String sourceName, int program, int[] drawBuffers) {
        if (!shouldCaptureGlState()) {
            return;
        }

        if (!LOGGED_FULLSCREEN_PROGRAMS.add(stageName + ":" + sourceName + ":" + program)) {
            return;
        }

        logDebugInfo(
            "fullscreen-program stage={} source={} program={} drawBuffers={} uniforms=[colortex1={}, colortex3={}, depthtex1={}, frameMod={}, cameraPosition={}, previousCameraPosition={}, gbufferProjection={}, gbufferProjectionInverse={}, gbufferModelViewInverse={}, gbufferPreviousModelView={}, gbufferPreviousProjection={}, pixelSizeX={}, pixelSizeY={}]",
            stageName,
            sourceName,
            program,
            java.util.Arrays.toString(drawBuffers),
            GL20.glGetUniformLocation(program, "colortex1"),
            GL20.glGetUniformLocation(program, "colortex3"),
            GL20.glGetUniformLocation(program, "depthtex1"),
            GL20.glGetUniformLocation(program, "frameMod"),
            GL20.glGetUniformLocation(program, "cameraPosition"),
            GL20.glGetUniformLocation(program, "previousCameraPosition"),
            GL20.glGetUniformLocation(program, "gbufferProjection"),
            GL20.glGetUniformLocation(program, "gbufferProjectionInverse"),
            GL20.glGetUniformLocation(program, "gbufferModelViewInverse"),
            GL20.glGetUniformLocation(program, "gbufferPreviousModelView"),
            GL20.glGetUniformLocation(program, "gbufferPreviousProjection"),
            GL20.glGetUniformLocation(program, "pixelSizeX"),
            GL20.glGetUniformLocation(program, "pixelSizeY")
        );
    }

    public static void logFullscreenPassState(String stageName, String sourceName, int program, int[] drawBuffers, java.util.Set<Integer> readsFromAlt, RenderTargets renderTargets) {
        if (!shouldCaptureGlState()) {
            return;
        }

        if (!LOGGED_FULLSCREEN_STATES.add(stageName + ":" + sourceName + ":" + program)) {
            return;
        }

        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        RenderTarget history = renderTargets.getRenderTargetCount() > 3 ? renderTargets.get(3) : null;
        int historyRead = history == null ? -1 : (readsFromAlt.contains(3) ? history.getAltTexture() : history.getMainTexture());
        int historyWrite = history == null ? -1 : (readsFromAlt.contains(3) ? history.getMainTexture() : history.getAltTexture());

        logDebugInfo(
            "fullscreen-state stage={} source={} program={} currentProgram={} drawBuffers={} readsFromAlt={} fb={} drawBuffer={} viewport=[{},{},{},{}] colortex3[main={}, alt={}, sampled={}, written={}] samplers[colortex1={}, colortex3={}, depthtex1={}] values[frameMod={}, pixelSizeX={}, pixelSizeY={}]",
            stageName,
            sourceName,
            program,
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            java.util.Arrays.toString(drawBuffers),
            readsFromAlt,
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3),
            history == null ? -1 : history.getMainTexture(),
            history == null ? -1 : history.getAltTexture(),
            historyRead,
            historyWrite,
            getSamplerUniform(program, "colortex1"),
            getSamplerUniform(program, "colortex3"),
            getSamplerUniform(program, "depthtex1"),
            getIntUniform(program, "frameMod"),
            getFloatUniform(program, "pixelSizeX"),
            getFloatUniform(program, "pixelSizeY")
        );
    }

    public static void logFullscreenSamplerSamples(String stageName, String sourceName, int program) {
        if (!shouldCaptureGlState()) {
            return;
        }

        if (framebufferSamplePhase == null) {
            return;
        }

        String label = framebufferSamplePhase + ":" + stageName + ":" + sourceName + ":" + program;
        int count = TEXTURE_SAMPLE_COUNTS.merge(label, 1, Integer::sum);
        if (count > 4) {
            return;
        }

        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        StringBuilder samples = new StringBuilder();
        appendSamplerTexture(samples, program, "colortex0");
        appendSamplerTexture(samples, program, "colortex1");
        appendSamplerTexture(samples, program, "colortex2");
        appendSamplerTexture(samples, program, "colortex3");
        appendSamplerTexture(samples, program, "gcolor");
        appendSamplerTexture(samples, program, "gaux1");
        appendSamplerTexture(samples, program, "gaux2");
        appendSamplerTexture(samples, program, "gaux3");
        appendSamplerTexture(samples, program, "gaux4");
        appendSamplerBinding(samples, program, "depthtex0");
        appendSamplerBinding(samples, program, "depthtex1");
        appendSamplerBinding(samples, program, "depthtex2");

        logDebugInfo(
            "fullscreen-sampler-sample label={} count={} fb={} readFb={} drawFb={} viewport=[{},{},{},{}]{}",
            label,
            count,
            previousFramebuffer,
            previousReadFramebuffer,
            previousDrawFramebuffer,
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3),
            samples
        );

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
        GL11.glReadBuffer(previousReadBuffer);
        GL13.glActiveTexture(previousActiveTexture);
    }

    public static void logCurrentFramebufferSamples(String label, int localColorAttachments) {
        if (!shouldCaptureGlState()) {
            return;
        }

        if (framebufferSamplePhase == null) {
            return;
        }

        String phasedLabel = framebufferSamplePhase + ":" + label;
        int count = FRAMEBUFFER_SAMPLE_COUNTS.merge(phasedLabel, 1, Integer::sum);
        if (count > 4 || localColorAttachments <= 0) {
            return;
        }

        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);
        int width = Math.max(1, VIEWPORT.get(2));
        int height = Math.max(1, VIEWPORT.get(3));
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        StringBuilder samples = new StringBuilder();
        for (int attachment = 0; attachment < localColorAttachments; attachment++) {
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0 + attachment);
            samples.append(" att").append(attachment).append("[");
            appendPixel(samples, width / 2, height / 2);
            samples.append(" ");
            appendPixel(samples, width / 4, height / 4);
            samples.append(" ");
            appendPixel(samples, (width * 3) / 4, height / 4);
            samples.append(" ");
            appendPixel(samples, width / 4, (height * 3) / 4);
            samples.append(" ");
            appendPixel(samples, (width * 3) / 4, (height * 3) / 4);
            samples.append("]");
        }

        logDebugInfo(
            "framebuffer-sample label={} count={} fb={} readFb={} drawFb={} viewport=[{},{},{},{}]{}",
            phasedLabel,
            count,
            previousFramebuffer,
            previousReadFramebuffer,
            previousDrawFramebuffer,
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3),
            samples
        );

        GL11.glReadBuffer(previousReadBuffer);
    }

    private static int getSamplerUniform(int program, String name) {
        if (program <= 0) {
            return -1;
        }

        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) {
            return -1;
        }

        return GL20.glGetUniformi(program, location);
    }

    private static int getIntUniform(int program, String name) {
        if (program <= 0) {
            return -1;
        }

        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) {
            return -1;
        }

        return GL20.glGetUniformi(program, location);
    }

    private static float getFloatUniform(int program, String name) {
        if (program <= 0) {
            return -1.0F;
        }

        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) {
            return -1.0F;
        }

        return GL20.glGetUniformf(program, location);
    }

    private static String getMatrixUniform(int program, String name) {
        if (program <= 0) {
            return "missing";
        }

        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) {
            return "missing";
        }

        FloatBuffer values = BufferUtils.createFloatBuffer(16);
        GL20.glGetUniformfv(program, location, values);
        return String.format(
            java.util.Locale.ROOT,
            "[%.6f,%.6f,%.6f,%.6f|%.6f,%.6f,%.6f,%.6f|%.6f,%.6f,%.6f,%.6f|%.6f,%.6f,%.6f,%.6f]",
            values.get(0),
            values.get(1),
            values.get(2),
            values.get(3),
            values.get(4),
            values.get(5),
            values.get(6),
            values.get(7),
            values.get(8),
            values.get(9),
            values.get(10),
            values.get(11),
            values.get(12),
            values.get(13),
            values.get(14),
            values.get(15)
        );
    }

    private static String getMatrixModeMatrix(int pname) {
        FloatBuffer values = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloatv(pname, values);
        return String.format(
            java.util.Locale.ROOT,
            "[%.4f,%.4f,%.4f,%.4f|%.4f,%.4f,%.4f,%.4f|%.4f,%.4f,%.4f,%.4f|%.4f,%.4f,%.4f,%.4f]",
            values.get(0),
            values.get(1),
            values.get(2),
            values.get(3),
            values.get(4),
            values.get(5),
            values.get(6),
            values.get(7),
            values.get(8),
            values.get(9),
            values.get(10),
            values.get(11),
            values.get(12),
            values.get(13),
            values.get(14),
            values.get(15)
        );
    }

    private static int getBoundTexture2D(int unit) {
        int previous = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(previous);
        return texture;
    }

    private static void appendSamplerTexture(StringBuilder builder, int program, String samplerName) {
        int unit = getSamplerUniform(program, samplerName);
        if (unit < 0) {
            return;
        }

        int texture = getBoundTexture2D(unit);
        builder.append(" ").append(samplerName).append("[unit=").append(unit).append(",tex=").append(texture);

        if (texture > 0) {
            appendTexturePixels(builder, texture);
        }

        builder.append("]");
    }

    private static void appendSamplerBinding(StringBuilder builder, int program, String samplerName) {
        int unit = getSamplerUniform(program, samplerName);
        if (unit < 0) {
            return;
        }

        builder.append(" ").append(samplerName)
            .append("[unit=").append(unit)
            .append(",tex=").append(getBoundTexture2D(unit))
            .append("]");
    }

    private static void appendTexturePixels(StringBuilder builder, int texture) {
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int previousTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (width <= 0 || height <= 0) {
            builder.append(",size=").append(width).append("x").append(height);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture0);
            GL13.glActiveTexture(previousActiveTexture);
            return;
        }

        if (debugFramebuffer == 0) {
            debugFramebuffer = GL30.glGenFramebuffers();
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, debugFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        builder.append(",size=").append(width).append("x").append(height);
        if (status == GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            builder.append(",pixels[");
            appendPixel(builder, width / 2, height / 2);
            builder.append(" ");
            appendPixel(builder, width / 4, height / 4);
            builder.append(" ");
            appendPixel(builder, (width * 3) / 4, height / 4);
            builder.append(" ");
            appendPixel(builder, width / 4, (height * 3) / 4);
            builder.append(" ");
            appendPixel(builder, (width * 3) / 4, (height * 3) / 4);
            builder.append("]");
        } else {
            builder.append(",fboStatus=").append(status);
        }

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture0);
        GL13.glActiveTexture(previousActiveTexture);
    }

    private static void appendPixel(StringBuilder builder, int x, int y) {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(Math.max(0, x), Math.max(0, y), 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        builder.append("(")
            .append(Byte.toUnsignedInt(pixel.get(0))).append(",")
            .append(Byte.toUnsignedInt(pixel.get(1))).append(",")
            .append(Byte.toUnsignedInt(pixel.get(2))).append(",")
            .append(Byte.toUnsignedInt(pixel.get(3))).append(")");
    }

    private static void logGlState(String stage, int error) {
        VIEWPORT.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer main = mc.getFramebuffer();
        int mainFbo = main != null ? main.framebufferObject : -1;
        int mainTex = main != null ? main.framebufferTexture : -1;
        int mainWidth = main != null ? main.framebufferWidth : -1;
        int mainHeight = main != null ? main.framebufferHeight : -1;

        LOGGER.error(
            "checkpoint={} error={} fb={} readFb={} drawFb={} drawBuffer={} readBuffer={} program={} vao={} viewport=[{},{},{},{}] mainFbo={} mainTex={} mainSize={}x{} display={}x{}",
            stage,
            error,
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL11.GL_DRAW_BUFFER),
            GL11.glGetInteger(GL11.GL_READ_BUFFER),
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
            GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
            VIEWPORT.get(0),
            VIEWPORT.get(1),
            VIEWPORT.get(2),
            VIEWPORT.get(3),
            mainFbo,
            mainTex,
            mainWidth,
            mainHeight,
            mc.displayWidth,
            mc.displayHeight
        );
    }
}
