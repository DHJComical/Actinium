package com.dhj.actinium.compat.scannable;

import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Adapter that lets Actinium stand in as the "shader mod" side of Scannable's OptiFine
 * integration ({@code li.cil.scannable.integration.optifine.ProxyOptiFine}).
 *
 * <p>Motivation (issue #94): Scannable's default scan-wave path ({@code injectDepthTexture=true},
 * mode {@code INJECT}) swaps the main framebuffer's {@code GL_DEPTH_ATTACHMENT} over to a private
 * depth texture and later restores it with {@code glFramebufferRenderbuffer(..., depthBuffer)}.
 * {@code FramebufferIrisMixin} replaces the vanilla depth renderbuffer with a shared depth
 * texture ({@code Framebuffer.depthBuffer} stays 0), so Scannable's restore detaches the depth
 * attachment entirely: terrain rendering afterwards has no depth buffer and loses
 * self-occlusion, which reads as see-through ground. With a shader pack active the same code
 * also runs while an Iris render target is bound, churning the gbuffer depth attachment and
 * drawing the additive scan quad into the pipeline's targets (white screen with ghosting).</p>
 *
 * <p>Scannable ships a dedicated path for external shader mods (mode {@code OPTIFINE}): it copies
 * the shader mod's depth texture into a private FBO right after world rendering and draws the
 * scan wave during the overlay phase. That path never touches the main framebuffer's attachments,
 * which is exactly what Actinium needs. {@code ProxyOptiFine} resolves OptiFine reflectively and
 * reports "no shader mod" when it is absent, so the Scannable mixin reroutes its two queries
 * ({@code isShaderPackLoaded} / {@code getDepthTexture}) here; Actinium's shared main-framebuffer
 * depth texture is the exact object Iris renders gbuffer depth into
 * ({@code DeferredWorldRenderingPipeline} constructs {@code RenderTargets} from it), so it carries
 * the world depth both with and without a shader pack.</p>
 *
 * <p>Assumption: Actinium's Cleanroom environment does not coexist with OptiFine, so when
 * {@code ProxyOptiFine} reports an OptiFine-managed depth texture we defer to it unchanged.</p>
 */
public final class ScannableShaderCompat {

    private static final Logger LOGGER = LogManager.getLogger("ActiniumScannableCompat");

    /**
     * Verbose tracing for field diagnosis (issue #94 reproduction): enabled with
     * {@code -Dactinium.debug.scannable=true}. Per-call probes are only logged while this is on;
     * the takeover notice is always logged once.
     */
    private static final boolean DEBUG = Boolean.getBoolean("actinium.debug.scannable");

    private static boolean takeoverLogged;
    private static boolean copyFixLogged;

    private ScannableShaderCompat() {
    }

    /**
     * Whether Actinium can take over Scannable's shader-mod probes.
     *
     * <p>True exactly when the shared main-framebuffer depth texture exists; that texture is both
     * the depth attachment Actinium installs and the object Iris renders gbuffer depth into, so
     * its presence is the precondition for answering Scannable's queries.</p>
     */
    public static boolean canProvideDepthTexture() {
        return mainDepthTextureId() > 0;
    }

    /**
     * Id of the shared main-framebuffer depth texture, or 0 when it does not exist yet.
     *
     * <p>{@code iris$getDepthTextureId()} reports -1 before the main framebuffer is built; the
     * OptiFine query contract this feeds treats 0 as "no depth texture" (Scannable then skips
     * rendering the wave), so the sentinel is mapped onto that value. The id is read live on
     * every call because framebuffer resizes recreate the texture.</p>
     */
    public static int mainDepthTextureId() {
        final int id = ((IRenderTargetExt) Minecraft.getMinecraft().getFramebuffer()).iris$getDepthTextureId();
        if (DEBUG) {
            LOGGER.info("scannable depth probe: main framebuffer depth texture id={}", id);
        }
        return id > 0 ? id : 0;
    }

    /** Logs the takeover once so reproduction runs can confirm the compat path is engaged. */
    public static void logTakeover() {
        if (takeoverLogged) {
            return;
        }
        takeoverLogged = true;
        LOGGER.info(
            "Scannable compat: Actinium now answers its shader-mod probes (depth texture {}); "
                + "scan wave uses Scannable's overlay path instead of depth-attachment injection",
            mainDepthTextureId());
    }

    /**
     * Logs the depth-copy texture format fix once; traces the corrected pixel type so a
     * reproduction run can confirm the wave samples valid depth data.
     */
    public static void logCopyTextureFix() {
        if (copyFixLogged) {
            return;
        }
        copyFixLogged = true;
        LOGGER.info(
            "Scannable compat: fixed depth-copy texture pixel type {} -> {} (GL_R32F requires "
                + "GL_FLOAT; the original GL_UNSIGNED_BYTE allocation yields no storage)",
            5121, 5126);
    }
}
