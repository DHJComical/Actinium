package com.gtnewhorizons.angelica.sdlgpu;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.gtnewhorizons.angelica.sdlgpu.device.SDLDrawable;
import org.lwjgl.glfw.GLFW;
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

    private static volatile boolean drawableInstalled;

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
            final Class<?> windowClass = Class.forName("org.lwjglx.opengl.Display$Window");
            windowHandleField = MethodHandles.privateLookupIn(windowClass, MethodHandles.lookup())
                .findStaticVarHandle(windowClass, "handle", long.class);
            // SharedDrawable.drawable is a lwjgl3ify-only field; absent in the Cleanroom lwjglxx.
            sharedDrawableWrappedField = lookupSharedDrawableField();
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
