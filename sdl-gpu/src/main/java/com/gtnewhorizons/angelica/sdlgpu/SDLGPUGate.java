package com.gtnewhorizons.angelica.sdlgpu;

import com.gtnewhorizons.angelica.config.SystemProperties;
import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import com.gtnewhorizons.angelica.sdlgpu.device.Device;
import com.gtnewhorizons.angelica.sdlgpu.shader.ShaderManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.sdl.SDLLog;
import org.lwjgl.system.Platform;
import org.lwjglx.Sys;
import org.lwjglx.input.Mouse;
import org.lwjglx.opengl.ContextAttribs;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.PixelFormat;
import org.taumc.glsl.grammar.GLSLParser;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Gate for taking over the game window with the SDL GPU backend.
 *
 * <p>Unlike the upstream lwjgl3ify environment (which exposes DisplayEvents and can suppress GL
 * context creation), Cleanroom's lwjglxx always creates a GLFW window with a GL context. The
 * bridge therefore lets lwjglxx create the window normally and then claims it for SDL GPU:
 * the GLFW window handle is wrapped via {@code SDL_CreateWindowFrom} (using the platform handle
 * obtained from GLFW) and claimed with {@code SDL_ClaimWindowForGPUDevice}. The unused GL context
 * stays attached to the window but is never made current again once the SDL backend is engaged.</p>
 *
 * <p>TODO(sdl-gpu): macOS needs the Metal layer property on the wrapped window before claim;
 * currently only Windows and Linux are wired up.</p>
 */
public final class SDLGPUGate {

    private static final Logger LOG = LogManager.getLogger("Angelica/SDLGPU");

    private SDLGPUGate() {}

    private static Device device;

    private static volatile Boolean deviceReady;
    private static volatile boolean engaged;
    private static volatile boolean disarmed;
    private static volatile Throwable initFailure;
    private static ByteBuffer[] windowIcons;

    public static void rememberIcons(ByteBuffer[] icons) {
        windowIcons = icons;
    }

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

    public static boolean isEngaged() {
        return engaged;
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

    public static boolean createSDLGPUDisplay(Object format, Object attribs) {
        if (!isDeviceReady()) return false;

        initFailure = null;
        disarmed = false;

        try {
            // lwjglxx creates the GLFW window (and a GL context that the SDL backend will not use).
            Display.create((PixelFormat) format, (ContextAttribs) attribs);
            final long glfwWindow = Display.getWindow();
            if (glfwWindow == 0) {
                throw new IllegalStateException("Display.create did not create a window");
            }
            final long platformHandle;
            switch (Platform.get()) {
                case WINDOWS -> platformHandle = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
                case LINUX -> platformHandle = GLFWNativeX11.glfwGetX11Window(glfwWindow);
                default -> throw new IllegalStateException(
                    "SDL GPU window claim not implemented for " + Platform.get());
            }
            if (platformHandle == 0) {
                throw new IllegalStateException("GLFW returned no platform handle for the window");
            }
            device().claimPlatformWindow(platformHandle);
            engaged = true;
            BackendManager.RENDER_BACKEND.onPostWindowCreate(glfwWindow);
        } catch (Throwable t) {
            initFailure = t;
            LOG.error("SDL GPU could not take the window", t);
            return false;
        }
        return engaged;
    }

    public static void fallBackToGL() {
        // The upstream lwjgl3ify LWJGL-service integration (SDLGPULWJGLService) does not exist in
        // the Cleanroom/lwjglxx environment; the SDL backend is only reachable through BackendManager.
        disarmed = true;
        engaged = false;
        deviceReady = false;

        device().destroyDevice();
        resetSdlLogging();

        Mouse.setGrabbed(false);

        if (Display.isCreated()) {
            Display.destroy();
        }
        Display.isCloseRequested();
        if (windowIcons != null) {
            Display.setIcon(windowIcons);
        }
    }

    private static void resetSdlLogging() {
        SDLLog.SDL_ResetLogPriorities();
    }
}
