package com.gtnewhorizons.angelica.sdlgpu;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.gtnewhorizons.angelica.sdlgpu.device.SDLDrawable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.sdl.SDLEvents;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.Drawable;
import org.lwjglx.opengl.DrawableGL;
import org.lwjglx.opengl.SharedDrawable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Bridge between the SDL GPU backend and the Cleanroom/lwjglxx {@link Display}.
 *
 * <p>lwjglxx always creates a GLFW window together with a GL context; Vulkan/D3D12 cannot claim a
 * window whose pixel format has been claimed by a GL context ({@code VK_ERROR_NATIVE_WINDOW_IN_USE_KHR}),
 * so the SDL path creates its own GLFW window with {@code GLFW_NO_API} (no GL context at all) and then
 * mirrors the window handle and size state into lwjglxx's {@code Display} static fields so that
 * Minecraft's {@code Display.*} calls keep working.</p>
 */
public final class SDLGPUDisplayBridge {
    private SDLGPUDisplayBridge() {}

    private static final Logger LOG = LogManager.getLogger("Angelica-SDLGPU");
    private static volatile boolean drawableInstalled;
    private static volatile double pendingMoveX;
    private static volatile double pendingMoveY;
    private static volatile boolean hasPendingMove;

    /**
     * Delivers the coalesced cursor position as a single move event. Called once per frame from
     * the SDL display pump, before lwjglxx's Mouse.poll(), so the input queue never fills with
     * move events and button events are not dropped.
     */
    public static void flushPendingMove() {
        if (hasPendingMove) {
            hasPendingMove = false;
            org.lwjglx.input.Mouse.addMoveEvent(pendingMoveX, pendingMoveY);
        }
    }

    private static final VarHandle DISPLAY_DRAWABLE_FIELD;
    private static final VarHandle SHARED_DRAWABLE_WRAPPED_FIELD;
    private static final VarHandle WINDOW_HANDLE_FIELD;
    private static final VarHandle DISPLAY_CREATED_FIELD;
    private static final VarHandle DISPLAY_WIDTH_FIELD;
    private static final VarHandle DISPLAY_HEIGHT_FIELD;
    private static final VarHandle DISPLAY_FB_WIDTH_FIELD;
    private static final VarHandle DISPLAY_FB_HEIGHT_FIELD;
    private static final VarHandle DISPLAY_TITLE_FIELD;
    private static final VarHandle DISPLAY_RESIZABLE_FIELD;
    private static final VarHandle DISPLAY_FOCUSED_FIELD;
    /**
     * LWJGL2 compatibility shim {@code org.lwjgl.opengl.Display} (com.cleanroommc:lwjglx), a
     * separate class from lwjglxx's {@link Display}; Minecraft's {@code EntityRenderer} reads
     * {@code Display.isActive()} from this shim, so its focus state must be mirrored too.
     */
    private static final java.lang.reflect.Field DISPLAY2_FOCUSED_FIELD;
    /**
     * The shim's own {@code org.lwjgl.opengl.Display$Window.handle}, used by shim
     * {@code Display.getWindow()} (DWM styling / taskbar integrations). The mixin universe
     * cannot see the shim classes, so mirror the handle here instead of a mixin.
     */
    private static final java.lang.reflect.Field DISPLAY2_WINDOW_HANDLE_FIELD;
    static {
        final VarHandle displayDrawableField;
        final VarHandle sharedDrawableWrappedField;
        final VarHandle windowHandleField;
        final VarHandle displayCreatedField;
        final VarHandle displayWidthField;
        final VarHandle displayHeightField;
        final VarHandle displayFbWidthField;
        final VarHandle displayFbHeightField;
        final VarHandle displayTitleField;
        final VarHandle displayResizableField;
        final VarHandle displayFocusedField;
        java.lang.reflect.Field display2FocusedField = null;
        java.lang.reflect.Field display2WindowHandleField = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Display.class, MethodHandles.lookup());
            displayDrawableField = lookup.findStaticVarHandle(Display.class, "drawable", DrawableGL.class);
            displayCreatedField = lookup.findStaticVarHandle(Display.class, "displayCreated", boolean.class);
            displayWidthField = lookup.findStaticVarHandle(Display.class, "displayWidth", int.class);
            displayHeightField = lookup.findStaticVarHandle(Display.class, "displayHeight", int.class);
            displayFbWidthField = lookup.findStaticVarHandle(Display.class, "displayFramebufferWidth", int.class);
            displayFbHeightField = lookup.findStaticVarHandle(Display.class, "displayFramebufferHeight", int.class);
            displayTitleField = lookup.findStaticVarHandle(Display.class, "windowTitle", String.class);
            displayResizableField = lookup.findStaticVarHandle(Display.class, "displayResizable", boolean.class);
            displayFocusedField = lookup.findStaticVarHandle(Display.class, "displayFocused", boolean.class);
            final Class<?> windowClass = Class.forName("org.lwjglx.opengl.Display$Window");
            windowHandleField = MethodHandles.privateLookupIn(windowClass, MethodHandles.lookup())
                .findStaticVarHandle(windowClass, "handle", long.class);
            // SharedDrawable.drawable is a lwjgl3ify-only field; absent in the Cleanroom lwjglxx.
            sharedDrawableWrappedField = lookupSharedDrawableField();
            try {
                final Class<?> lwjgl2Display = Class.forName("org.lwjgl.opengl.Display");
                display2FocusedField = lwjgl2Display.getDeclaredField("displayFocused");
                display2FocusedField.setAccessible(true);
                final Class<?> lwjgl2Window = Class.forName("org.lwjgl.opengl.Display$Window");
                display2WindowHandleField = lwjgl2Window.getDeclaredField("handle");
                display2WindowHandleField.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                LOG.warn("LWJGL2 compat Display fields unavailable; focus/window handle will not be mirrored", e);
            }
        } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
        DISPLAY_DRAWABLE_FIELD = displayDrawableField;
        SHARED_DRAWABLE_WRAPPED_FIELD = sharedDrawableWrappedField;
        WINDOW_HANDLE_FIELD = windowHandleField;
        DISPLAY_CREATED_FIELD = displayCreatedField;
        DISPLAY_WIDTH_FIELD = displayWidthField;
        DISPLAY_HEIGHT_FIELD = displayHeightField;
        DISPLAY_FB_WIDTH_FIELD = displayFbWidthField;
        DISPLAY_FB_HEIGHT_FIELD = displayFbHeightField;
        DISPLAY_TITLE_FIELD = displayTitleField;
        DISPLAY_RESIZABLE_FIELD = displayResizableField;
        DISPLAY_FOCUSED_FIELD = displayFocusedField;
        DISPLAY2_FOCUSED_FIELD = display2FocusedField;
        DISPLAY2_WINDOW_HANDLE_FIELD = display2WindowHandleField;
    }

    private static void setFocused(boolean focused) {
        DISPLAY_FOCUSED_FIELD.set(focused);
        if (DISPLAY2_FOCUSED_FIELD != null) {
            try {
                DISPLAY2_FOCUSED_FIELD.setBoolean(null, focused);
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to mirror window focus into LWJGL2 compat Display", e);
            }
        }
    }

    private static void mirrorLwjgl2WindowHandle(long window) {
        if (DISPLAY2_WINDOW_HANDLE_FIELD != null) {
            try {
                DISPLAY2_WINDOW_HANDLE_FIELD.setLong(null, window);
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to mirror window handle into LWJGL2 compat Display$Window", e);
            }
        }
    }

    private static VarHandle lookupSharedDrawableField() {
        try {
            return MethodHandles.privateLookupIn(SharedDrawable.class, MethodHandles.lookup())
                .findVarHandle(SharedDrawable.class, "drawable", Drawable.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null; // Cleanroom's lwjglxx SharedDrawable has no drawable field.
        }
    }

    /**
     * Creates a GLFW window without any GL context ({@code GLFW_NO_API}): the SDL GPU backend
     * presents through SDL, and a GL context on the window would prevent Vulkan/D3D12 from
     * claiming it.
     *
     * @param width initial window width
     * @param height initial window height
     * @param title window title
     * @return the GLFW window handle
     */
    public static long createWindowNoGlContext(int width, int height, String title) {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
        final long window = GLFW.glfwCreateWindow(width, height, title, 0L, 0L);
        if (window == 0) {
            throw new IllegalStateException("glfwCreateWindow failed (no GL context)");
        }
        return window;
    }

    /**
     * Mirrors the given GLFW window into lwjglxx's {@code Display} static state so Minecraft's
     * {@code Display.*} queries keep working without going through lwjglxx's own window creation.
     */
    public static void adoptWindow(long window, int width, int height, String title) {
        WINDOW_HANDLE_FIELD.set(window);
        mirrorLwjgl2WindowHandle(window);
        DISPLAY_CREATED_FIELD.set(true);
        DISPLAY_WIDTH_FIELD.set(width);
        DISPLAY_HEIGHT_FIELD.set(height);
        final int[] fbWidth = new int[1];
        final int[] fbHeight = new int[1];
        GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);
        DISPLAY_FB_WIDTH_FIELD.set(fbWidth[0]);
        DISPLAY_FB_HEIGHT_FIELD.set(fbHeight[0]);
        DISPLAY_TITLE_FIELD.set(title);
        DISPLAY_RESIZABLE_FIELD.set(true);
        installLwjglxWindowCallbacks();
        initializeLwjglxInput();
    }

    /**
     * Safe equivalent of the LWJGL2 {@code Display.destroy()} shutdown path for the SDL backend.
     *
     * <p>Minecraft's {@code shutdownMinecraftApplet()} calls {@code Display.destroy()} in a
     * finally block. The LWJGL2 compatibility shim's {@code destroy()} first runs
     * {@code Display$Window.releaseCallbacks()}, which frees every GLFW callback field
     * unconditionally; those fields are never populated on the SDL path, so the shutdown NPEs.
     * The shim classes are invisible to the mixin universe, so the call is redirected here
     * instead: just destroy the GLFW window (GLFW releases the callbacks when the window is
     * destroyed) and clear the mirrored state. GLFW itself is still initialized by this point.</p>
     */
    public static void safeDisplayDestroy() {
        final long window = (long) WINDOW_HANDLE_FIELD.get();
        if (window != 0) {
            GLFW.glfwDestroyWindow(window);
        }
        DISPLAY_CREATED_FIELD.set(false);
        DISPLAY_WIDTH_FIELD.set(0);
        DISPLAY_HEIGHT_FIELD.set(0);
    }

    /**
     * lwjglxx creates and initializes Mouse and Keyboard inside {@code Display.create()}, which the
     * SDL GPU backend bypasses (no GL context is created). Without this, the input queues never
     * fill and the mouse/keyboard are dead even though the window callbacks are installed.
     */
    private static void initializeLwjglxInput() {
        try {
            org.lwjglx.input.Mouse.create();
        } catch (Throwable t) {
            LOG.error("Failed to initialize lwjglxx Mouse; mouse input will not work", t);
        }
        try {
            org.lwjglx.input.Keyboard.create();
        } catch (Throwable t) {
            LOG.error("Failed to initialize lwjglxx Keyboard; keyboard input will not work", t);
        }
    }

    /**
     * Installs lwjglxx's GLFW callbacks on the adopted window. lwjglxx creates the callback
     * objects inside its own {@code Display.create()} path, which the SDL GPU backend bypasses
     * (no GL context is created); without them the mouse and keyboard are dead and window resize
     * is not propagated to Minecraft. We build equivalent callbacks that forward into lwjglxx's
     * public input API (Mouse.addMoveEvent/..., Keyboard.addGlfwKeyEvent/...) and install them.
     */
    private static void installLwjglxWindowCallbacks() {
        try {
            final Class<?> windowClass = Class.forName("org.lwjglx.opengl.Display$Window");
            // The callback objects must stay strongly reachable: GLFW calls into them through
            // native function pointers, and lwjgl detaches a callback when its Java object is
            // collected. Storing them in lwjglxx's Window static fields keeps them alive and is
            // exactly what lwjglxx itself does.
            final org.lwjgl.glfw.GLFWCursorPosCallback cursorPos = new org.lwjgl.glfw.GLFWCursorPosCallback() {
                @Override public void invoke(long window, double xpos, double ypos) {
                    // Coalesce moves to one event per frame: lwjglxx's input queue holds only 32
                    // events and Minecraft polls it once per tick, so an unbounded stream of move
                    // events can overflow the queue and drop button events, making clicks dead.
                    pendingMoveX = xpos;
                    pendingMoveY = ypos;
                    hasPendingMove = true;
                }
            };
            final org.lwjgl.glfw.GLFWMouseButtonCallback mouseButton = new org.lwjgl.glfw.GLFWMouseButtonCallback() {
                @Override public void invoke(long window, int button, int action, int mods) {
                    org.lwjglx.input.Mouse.addButtonEvent(button, action != org.lwjgl.glfw.GLFW.GLFW_RELEASE);
                }
            };
            final org.lwjgl.glfw.GLFWScrollCallback scroll = new org.lwjgl.glfw.GLFWScrollCallback() {
                @Override public void invoke(long window, double xoffset, double yoffset) {
                    org.lwjglx.input.Mouse.addWheelEvent(yoffset);
                }
            };
            final org.lwjgl.glfw.GLFWKeyCallback key = new org.lwjgl.glfw.GLFWKeyCallback() {
                @Override public void invoke(long window, int key, int scancode, int action, int mods) {
                    final char ascii = (key > 32 && key <= 96) ? org.lwjglx.input.KeyCodes.glfwToASCII(key) : (char) (key & 31);
                    org.lwjglx.input.Keyboard.addGlfwKeyEvent(window, key, scancode, action, mods, ascii);
                }
            };
            final org.lwjgl.glfw.GLFWCharCallback ch = new org.lwjgl.glfw.GLFWCharCallback() {
                @Override public void invoke(long window, int codepoint) {
                    org.lwjglx.input.Keyboard.addCharEvent(0, (char) codepoint);
                }
            };
            // lwjglxx's Display.create() initializes displayFocused from ForgeEarlyConfig and never
            // updates it again (its windowFocusCallback field is never assigned, so focus changes
            // are not tracked at all). The SDL path bypasses Display.create(), leaving
            // Display.isActive() permanently false; EntityRenderer.updateCameraAndRender then
            // treats the window as unfocused and, with pauseOnLostFocus enabled, opens the pause
            // menu on its own ~500 ms after the last frame while in-game. Track real focus and
            // mirror it into both lwjglxx's Display and the LWJGL2 compat shim.
            final org.lwjgl.glfw.GLFWWindowFocusCallback focus = new org.lwjgl.glfw.GLFWWindowFocusCallback() {
                @Override public void invoke(long window, boolean focused) {
                    setFocused(focused);
                }
            };
            setWindowCallbackField(windowClass, "windowFocusCallback", focus);
            setFocused(true);
            setWindowCallbackField(windowClass, "cursorPosCallback", cursorPos);
            setWindowCallbackField(windowClass, "mouseButtonCallback", mouseButton);
            setWindowCallbackField(windowClass, "scrollCallback", scroll);
            setWindowCallbackField(windowClass, "keyCallback", key);
            setWindowCallbackField(windowClass, "charCallback", ch);
            final java.lang.reflect.Method setCallbacks = windowClass.getMethod("setCallbacks");
            setCallbacks.setAccessible(true);
            setCallbacks.invoke(null);
            LOG.info("lwjglxx input callbacks installed on SDL window");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.error("Failed to install lwjglxx window callbacks; input will not work", e);
        }
    }

    private static void setWindowCallbackField(Class<?> windowClass, String name, Object callback) throws ReflectiveOperationException {
        final java.lang.reflect.Field field = windowClass.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, callback);
    }

    public static void ensureDrawableInstalled() {
        if (drawableInstalled) return;
        if (!SDLGPUGate.isActive()) return;
        if (DISPLAY_DRAWABLE_FIELD.get() == null) {
            DISPLAY_DRAWABLE_FIELD.set(new SDLDrawable());
        }
        drawableInstalled = true;
    }

    public static void present() {
        if (SDLGPUGate.isActive()) {
            BackendManager.RENDER_BACKEND.handleSwapBuffers();
        }
    }

    /**
     * Pumps SDL's event queue. The SDL window wraps an external GLFW-created window; SDL needs
     * its own event processing to track visibility/resize, otherwise the Vulkan swapchain acquire
     * blocks waiting for the window to be shown.
     */
    public static void pumpEvents() {
        if (SDLGPUGate.isActive()) {
            SDLEvents.SDL_PumpEvents();
        }
    }

    public static void releaseRenderThread() {
        if (SDLGPUGate.isActive()) {
            BackendManager.RENDER_BACKEND.onRenderThreadReleased(Thread.currentThread());
        }
    }

    public static boolean isSdlDrawable(SharedDrawable sd) {
        return sd != null && SHARED_DRAWABLE_WRAPPED_FIELD.get(sd) instanceof SDLDrawable;
    }

    public static boolean isSdlSharedDrawable(Object drawable) {
        return drawable instanceof SharedDrawable && isSdlDrawable((SharedDrawable) drawable);
    }
}
