package com.gtnewhorizons.angelica.sdlgpu;

import com.gtnewhorizons.angelica.config.SystemProperties;
import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import com.gtnewhorizons.angelica.sdlgpu.device.Device;
import com.gtnewhorizons.angelica.sdlgpu.shader.ShaderManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.sdl.SDLLog;
import org.lwjglx.Sys;
import org.taumc.glsl.grammar.GLSLParser;

import java.util.function.Consumer;

/**
 * Gate owning the SDL GPU device lifecycle and availability probing.
 *
 * <p>The window takeover path is currently removed: Cleanroom's lwjglxx always creates a GLFW
 * window with a GL context, which a GPU device cannot claim ({@code VK_ERROR_NATIVE_WINDOW_IN_USE_KHR}),
 * and the interim workaround (building a {@code GLFW_NO_API} window and mirroring lwjglxx's
 * {@code Display} state with hand-installed GLFW callbacks) was reverted. The backend is therefore
 * not registered in the {@code RenderBackend} service file. Re-wiring the backend requires a window
 * source Cleanroom actually controls (a native SDL window) whose handle can be wrapped and claimed
 * through {@link Device#claimWindow} before the backend presents.</p>
 */
public final class SDLGPUGate {

    private static final Logger LOG = LogManager.getLogger("Angelica/SDLGPU");

    private SDLGPUGate() {}

    private static Device device;

    private static volatile Boolean deviceReady;

    public static synchronized Device device() {
        if (device == null) device = new Device();
        return device;
    }

    private static final boolean SDL_GPU_AVAILABLE = SDLGPUGate.class.getClassLoader().getResource("org/lwjgl/sdl/SDLGPU.class") != null;

    public static boolean isSDLGPUAvailable() {
        return SDL_GPU_AVAILABLE;
    }

    public static boolean isDeviceReady() {
        final Boolean ready = deviceReady;
        return ready != null ? ready : probe();
    }

    private static synchronized boolean probe() {
        if (deviceReady != null) return deviceReady;
        if (!SystemProperties.USE_SDL_GPU || !isSDLGPUAvailable()) {
            deviceReady = false;
            return false;
        }
        boolean ok;
        try {
            Sys.initialize();
            // Backend probing happens during class initialization on the main thread.
            ok = device().createDevice();
        } catch (Throwable t) {
            LOG.error("SDL GPU device probe failed", t);
            ok = false;
        }
        if (!ok) resetSdlLogging();
        deviceReady = ok;
        return ok;
    }

    public static boolean isActive() {
        if (!isSDLGPUAvailable()) return false;
        final RenderBackend rb = BackendManager.RENDER_BACKEND;
        return rb instanceof SDLGPURenderBackend;
    }

    public static void ensureDrawableInstalled() {
        if (!isActive()) return;
        SDLGPUDisplayBridge.ensureDrawableInstalled();
    }

    public static void prewarmSpirv(String transformedSource, int glShaderType) {
        if (!isActive()) return;
        ShaderManager.prewarmSpirv(transformedSource, glShaderType);
    }

    public static void prewarmSpirv(String transformedSource, GLSLParser.Translation_unitContext bodyTree, int headerLen, int glShaderType) {
        if (!isActive()) return;
        ShaderManager.prewarmSpirv(transformedSource, bodyTree, headerLen, glShaderType);
    }

    public static void clearShaderPrewarmCache() {
        if (!isActive()) return;
        ShaderManager.clearPrewarmCache();
    }

    public static Consumer<Object> sdlGpuPreSwapchainInvalidatingCallback() {
        return change -> BackendManager.RENDER_BACKEND.onPreSwapchainInvalidatingChange(change);
    }

    private static void resetSdlLogging() {
        SDLLog.SDL_ResetLogPriorities();
    }
}
