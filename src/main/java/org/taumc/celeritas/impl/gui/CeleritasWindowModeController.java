package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.mixin.core.MinecraftAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.IntBuffer;

public final class CeleritasWindowModeController {
    private static final Logger LOGGER = CeleritasVintage.logger();
    private static boolean synchronizing;
    private static boolean loggedWindowLookupFailure;
    private static final WindowBounds savedWindowedBounds = new WindowBounds();

    private CeleritasWindowModeController() {
    }

    public static CeleritasFullscreenMode resolveConfiguredMode(SodiumGameOptions options) {
        CeleritasFullscreenMode configuredMode = options.window.fullscreenMode;

        if (configuredMode != null) {
            return configuredMode;
        }

        return CeleritasFullscreenMode.FULLSCREEN;
    }

    public static void applyMode(Minecraft client, SodiumGameOptions options, CeleritasFullscreenMode mode) {
        options.window.fullscreenMode = mode;

        if (client.gameSettings.fullScreen || client.isFullScreen()) {
            synchronize(client);
        }
    }

    public static void applyFullscreenEnabled(Minecraft client, boolean enabled) {
        client.gameSettings.fullScreen = enabled;
        synchronize(client);
    }

    public static void synchronize(Minecraft client) {
        if (synchronizing) {
            return;
        }

        boolean shouldBeFullscreen = client.gameSettings.fullScreen;
        CeleritasFullscreenMode desiredMode = resolveConfiguredMode(CeleritasVintage.options());

        long windowHandle = findWindowHandle(client);

        if (isWindowStateCompatible(client, windowHandle, shouldBeFullscreen, desiredMode)) {
            return;
        }

        synchronizing = true;

        try {
            if (!shouldBeFullscreen) {
                applyWindowed(client, windowHandle);
            } else {
                switch (desiredMode) {
                    case FULLSCREEN -> applyExclusiveFullscreen(client, windowHandle);
                    case BORDERLESS -> applyBorderlessFullscreen(client, windowHandle);
                }
            }
        } finally {
            synchronizing = false;
        }
    }

    private static boolean isWindowStateCompatible(Minecraft client, long windowHandle, boolean shouldBeFullscreen, CeleritasFullscreenMode desiredMode) {
        if (windowHandle == 0L) {
            return client.isFullScreen() == shouldBeFullscreen;
        }

        if (!shouldBeFullscreen) {
            return !client.isFullScreen() && !isBorderless(windowHandle) && GLFW.glfwGetWindowMonitor(windowHandle) == 0L;
        }

        return switch (desiredMode) {
            case FULLSCREEN -> client.isFullScreen() && !isBorderless(windowHandle);
            case BORDERLESS -> client.isFullScreen() && isBorderless(windowHandle);
        };
    }

    private static void applyWindowed(Minecraft client, long windowHandle) {
        if (windowHandle == 0L) {
            if (client.isFullScreen()) {
                client.toggleFullscreen();
            }

            return;
        }

        restoreWindowDecorations(windowHandle);
        WindowBounds bounds = getSavedWindowedBounds(windowHandle);
        GLFW.glfwSetWindowMonitor(windowHandle, 0L, bounds.x, bounds.y, bounds.width, bounds.height, GLFW.GLFW_DONT_CARE);
        setFullscreenState(client, false);
        updateClientDisplaySize(client, windowHandle);
    }

    private static void applyExclusiveFullscreen(Minecraft client, long windowHandle) {
        if (windowHandle == 0L) {
            if (!client.isFullScreen()) {
                client.toggleFullscreen();
            }

            return;
        }

        captureWindowedBounds(windowHandle);
        long monitor = resolveMonitor(windowHandle);
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);

        if (videoMode == null) {
            return;
        }

        GLFW.glfwSetWindowAttrib(windowHandle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowMonitor(windowHandle, monitor, 0, 0, videoMode.width(), videoMode.height(), videoMode.refreshRate());
        setFullscreenState(client, true);
        updateClientDisplaySize(client, windowHandle);
    }

    private static void applyBorderlessFullscreen(Minecraft client, long windowHandle) {
        if (windowHandle == 0L) {
            if (!client.isFullScreen()) {
                client.toggleFullscreen();
            }

            return;
        }

        captureWindowedBounds(windowHandle);
        long monitor = resolveMonitor(windowHandle);
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);

        if (videoMode == null) {
            return;
        }

        int monitorX = 0;
        int monitorY = 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetMonitorPos(monitor, x, y);
            monitorX = x.get(0);
            monitorY = y.get(0);
        }

        GLFW.glfwSetWindowAttrib(windowHandle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowMonitor(windowHandle, 0L, monitorX, monitorY, videoMode.width(), videoMode.height(), videoMode.refreshRate());
        setFullscreenState(client, true);
        updateClientDisplaySize(client, windowHandle);
    }

    private static void restoreWindowDecorations(long windowHandle) {
        if (windowHandle == 0L) {
            return;
        }

        GLFW.glfwSetWindowAttrib(windowHandle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
    }

    private static boolean isBorderless(long windowHandle) {
        return windowHandle != 0L
                && GLFW.glfwGetWindowMonitor(windowHandle) == 0L
                && GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_DECORATED) == GLFW.GLFW_FALSE;
    }

    private static void captureWindowedBounds(long windowHandle) {
        if (windowHandle == 0L || isBorderless(windowHandle) || GLFW.glfwGetWindowMonitor(windowHandle) != 0L) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(windowHandle, x, y);
            GLFW.glfwGetWindowSize(windowHandle, width, height);
            savedWindowedBounds.set(x.get(0), y.get(0), width.get(0), height.get(0));
        }
    }

    private static WindowBounds getSavedWindowedBounds(long windowHandle) {
        if (savedWindowedBounds.isValid()) {
            return savedWindowedBounds;
        }

        long monitor = resolveMonitor(windowHandle);
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);

        if (videoMode == null) {
            savedWindowedBounds.set(100, 100, 1280, 720);
            return savedWindowedBounds;
        }

        int monitorX = 0;
        int monitorY = 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetMonitorPos(monitor, x, y);
            monitorX = x.get(0);
            monitorY = y.get(0);
        }

        int width = Math.max(854, (int) (videoMode.width() * 0.8f));
        int height = Math.max(480, (int) (videoMode.height() * 0.8f));
        int x = monitorX + Math.max(0, (videoMode.width() - width) / 2);
        int y = monitorY + Math.max(0, (videoMode.height() - height) / 2);
        savedWindowedBounds.set(x, y, width, height);
        return savedWindowedBounds;
    }

    private static void setFullscreenState(Minecraft client, boolean fullscreen) {
        ((MinecraftAccessor) client).celeritas$setFullscreen(fullscreen);
        client.gameSettings.fullScreen = fullscreen;
    }

    private static void updateClientDisplaySize(Minecraft client, long windowHandle) {
        if (windowHandle == 0L) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer framebufferWidth = stack.mallocInt(1);
            IntBuffer framebufferHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(windowHandle, framebufferWidth, framebufferHeight);

            int width = framebufferWidth.get(0);
            int height = framebufferHeight.get(0);

            if (width > 0 && height > 0) {
                client.displayWidth = width;
                client.displayHeight = height;
                client.resize(width, height);
            }
        }
    }

    private static long resolveMonitor(long windowHandle) {
        long monitor = GLFW.glfwGetWindowMonitor(windowHandle);
        return monitor != 0L ? monitor : GLFW.glfwGetPrimaryMonitor();
    }

    private static long findWindowHandle(Minecraft client) {
        Long handle = extractLongHandle(client);

        if (handle != null) {
            return handle;
        }

        if (!loggedWindowLookupFailure) {
            loggedWindowLookupFailure = true;
            LOGGER.warn("Unable to resolve the GLFW window handle, borderless fullscreen will be unavailable");
        }

        return 0L;
    }

    private static @Nullable Long extractLongHandle(Object instance) {
        if (instance == null) {
            return null;
        }

        if (instance instanceof Number number) {
            long value = number.longValue();
            return value != 0L ? value : null;
        }

        for (String methodName : new String[] { "getMainWindow", "getWindow", "getWindowHandle", "getHandle", "getWindowId" }) {
            Object nested = invokeNoArgs(instance, methodName);

            if (nested == null) {
                continue;
            }

            Long nestedHandle = extractLongHandle(nested);

            if (nestedHandle != null) {
                return nestedHandle;
            }
        }

        for (String fieldName : new String[] { "mainWindow", "window", "windowHandle", "handle", "windowId" }) {
            Object nested = getFieldValue(instance, fieldName);

            if (nested == null) {
                continue;
            }

            Long nestedHandle = extractLongHandle(nested);

            if (nestedHandle != null) {
                return nestedHandle;
            }
        }

        return null;
    }

    private static @Nullable Object invokeNoArgs(Object instance, String methodName) {
        try {
            Method method = instance.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Object getFieldValue(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static final class WindowBounds {
        private int x;
        private int y;
        private int width;
        private int height;

        private void set(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean isValid() {
            return this.width > 0 && this.height > 0;
        }
    }
}
