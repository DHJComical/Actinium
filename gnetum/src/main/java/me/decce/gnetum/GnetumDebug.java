package me.decce.gnetum;

import me.decce.gnetum.gl.FramebufferTracker;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import com.dhj.actinium.runtime.ActiniumRuntime;

public final class GnetumDebug {
    private static final Logger LOGGER = LogManager.getLogger("GnetumHudDebug");
    private static final String HUD_DEBUG_PROPERTY = "gnetum.hudDebug";
    private static final String FRAME_LIMIT_PROPERTY = "gnetum.hudDebugFrameLimit";
    private static final int DEFAULT_FRAME_LIMIT = 240;

    private static int overlayFrame;
    private static int loggedFrames;
    private static int lastLoggedFrame = -1;
    private static int lastUncountedFrameLog = -1;
    private static boolean currentFrameAllowed;
    private static boolean wasEnabled;
    private static boolean announcedLimit;

    private GnetumDebug() {
    }

    public static boolean isEnabled() {
        String override = System.getProperty(HUD_DEBUG_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return ActiniumRuntime.options().debug.enableGnetumHudDebug;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean shouldLog() {
        if (!isEnabled()) {
            if (wasEnabled) {
                wasEnabled = false;
                announcedLimit = false;
                loggedFrames = 0;
                lastLoggedFrame = -1;
                lastUncountedFrameLog = -1;
                currentFrameAllowed = false;
            }
            return false;
        }

        if (!wasEnabled) {
            wasEnabled = true;
            announcedLimit = false;
            loggedFrames = 0;
            lastLoggedFrame = -1;
            lastUncountedFrameLog = -1;
            LOGGER.info("HUD debug enabled. Runtime: {}", describeRuntime());
        }

        if (currentFrameAllowed) {
            return true;
        }

        int limit = getFrameLimit();
        if (limit > 0 && loggedFrames >= limit && lastLoggedFrame != overlayFrame) {
            if (!announcedLimit) {
                announcedLimit = true;
                LOGGER.info("HUD debug frame limit reached after {} logged frames. Set -D{}=0 to disable the limit.", loggedFrames, FRAME_LIMIT_PROPERTY);
            }
            return false;
        }
        announcedLimit = false;
        return false;
    }

    public static int beginOverlayFrame() {
        return beginOverlayFrame(true);
    }

    public static int beginOverlayFrame(boolean countFrame) {
        overlayFrame++;
        currentFrameAllowed = false;

        if (!isEnabled()) {
            shouldLog();
            return overlayFrame;
        }

        if (!countFrame) {
            if (lastUncountedFrameLog < 0 || overlayFrame - lastUncountedFrameLog >= 60) {
                currentFrameAllowed = true;
                lastUncountedFrameLog = overlayFrame;
                log("frame-begin uncounted=true mcFrame={} player={} world={}", Minecraft.getDebugFPS(), hasPlayer(), hasWorld());
            }
            return overlayFrame;
        }

        int limit = getFrameLimit();
        if (limit > 0 && loggedFrames >= limit) {
            shouldLog();
            return overlayFrame;
        }

        if (lastLoggedFrame != overlayFrame) {
            currentFrameAllowed = true;
            lastLoggedFrame = overlayFrame;
            loggedFrames++;
            log("frame-begin mcFrame={} player={} world={}", Minecraft.getDebugFPS(), hasPlayer(), hasWorld());
        }
        return overlayFrame;
    }

    public static int frame() {
        return overlayFrame;
    }

    public static void log(String message, Object... params) {
        if (shouldLog()) {
            LOGGER.info("[frame {}] " + message, prependFrame(params));
        }
    }

    public static void logAlways(String message, Object... params) {
        LOGGER.info("[frame {}] " + message, prependFrame(params));
    }

    public static void logGlState(String label) {
        if (!shouldLog()) {
            return;
        }

        LOGGER.info(
                "[frame {}] gl-state {} boundFbo={} trackerFbo={} tex2d={} blend={} alpha={} depth={} blendFunc=({},{},{},{}) matrixMode={} error={}",
                overlayFrame,
                label,
                getInteger(GL30Compat.FRAMEBUFFER_BINDING),
                FramebufferTracker.getCurrentlyBoundFbo(),
                GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                getInteger(GL14.GL_BLEND_SRC_RGB),
                getInteger(GL14.GL_BLEND_DST_RGB),
                getInteger(GL14.GL_BLEND_SRC_ALPHA),
                getInteger(GL14.GL_BLEND_DST_ALPHA),
                getInteger(GL11.GL_MATRIX_MODE),
                glError()
        );
    }

    public static String describeRuntime() {
        try {
            var mc = Minecraft.getMinecraft();
            return "enabled=" + Gnetum.config.isEnabled()
                    + ",wanted=" + Gnetum.config.enabled.get()
                    + ",rendering=" + Gnetum.rendering
                    + ",renderingCanceled=" + Gnetum.renderingCanceled
                    + ",currentPass=" + Gnetum.passManager.current
                    + ",numberOfPasses=" + Gnetum.config.numberOfPasses
                    + ",maxFps=" + Gnetum.config.maxFps
                    + ",display=" + mc.displayWidth + "x" + mc.displayHeight
                    + ",guiScale=" + mc.gameSettings.guiScale
                    + ",fullscreen=" + mc.gameSettings.fullScreen;
        } catch (RuntimeException | LinkageError e) {
            return "unavailable:" + e.getClass().getSimpleName();
        }
    }

    private static Object[] prependFrame(Object[] params) {
        Object[] output = new Object[params.length + 1];
        output[0] = overlayFrame;
        System.arraycopy(params, 0, output, 1, params.length);
        return output;
    }

    private static int getFrameLimit() {
        return Integer.getInteger(FRAME_LIMIT_PROPERTY, DEFAULT_FRAME_LIMIT);
    }

    private static boolean hasPlayer() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.player != null;
    }

    private static boolean hasWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.world != null;
    }

    private static int getInteger(int pname) {
        try {
            return GL11.glGetInteger(pname);
        } catch (RuntimeException | LinkageError ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static int glError() {
        try {
            return GL11.glGetError();
        } catch (RuntimeException | LinkageError ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static final class GL30Compat {
        private static final int FRAMEBUFFER_BINDING = 0x8CA6;
    }
}
