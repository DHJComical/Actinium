package com.dhj.actinium.gui;

import com.dhj.actinium.mixin.vintage.core.MinecraftAccessor;
import com.dhj.actinium.runtime.ActiniumRuntime;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.lwjgl.opengl.Display;

public final class ActiniumWindowModeController {
    private static final Logger LOGGER = ActiniumRuntime.logger();
    private static boolean synchronizing;
    private static String loggedInvalidFullscreenMode;

    private ActiniumWindowModeController() {
    }

    public static FullscreenMode resolveConfiguredMode(SodiumGameOptions options) {
        String configuredModeName = options.window.fullscreenMode;
        if (configuredModeName != null) {
            if ("FULLSCREEN".equals(configuredModeName)) {
                options.window.fullscreenMode = FullscreenMode.EXCLUSIVE.name();
                return FullscreenMode.EXCLUSIVE;
            }
            try {
                return FullscreenMode.valueOf(configuredModeName);
            } catch (IllegalArgumentException e) {
                if (!configuredModeName.equals(loggedInvalidFullscreenMode)) {
                    loggedInvalidFullscreenMode = configuredModeName;
                    LOGGER.warn(
                        "Unknown fullscreen mode '{}' in the options file, falling back to {}",
                        configuredModeName,
                        FullscreenMode.OFF
                    );
                }
            }
        }

        return FullscreenMode.OFF;
    }

    public static void applyMode(Minecraft client, SodiumGameOptions options, FullscreenMode mode) {
        options.window.fullscreenMode = mode.name();
        if (mode != FullscreenMode.OFF) {
            options.window.lastFullscreenMode = mode.name();
        }
        client.gameSettings.fullScreen = mode != FullscreenMode.OFF;
        synchronize(client);
    }

    public static void toggleFullscreen(Minecraft client) {
        SodiumGameOptions options = ActiniumRuntime.options();
        FullscreenMode current = resolveConfiguredMode(options);
        if (current != FullscreenMode.OFF && options.window.lastFullscreenMode == null) {
            options.window.lastFullscreenMode = current.name();
        }
        applyMode(client, options, nextMode(current, resolveLastFullscreenMode(options)));
    }

    static FullscreenMode nextMode(FullscreenMode current, FullscreenMode lastFullscreenMode) {
        return current == FullscreenMode.OFF ? lastFullscreenMode : FullscreenMode.OFF;
    }

    static FullscreenMode resolveLastFullscreenMode(SodiumGameOptions options) {
        String configuredModeName = options.window.lastFullscreenMode;
        if (configuredModeName == null) {
            FullscreenMode current = resolveConfiguredMode(options);
            if (current != FullscreenMode.OFF) {
                return current;
            }
        }
        if (configuredModeName != null) {
            try {
                FullscreenMode mode = FullscreenMode.valueOf(configuredModeName);
                if (mode != FullscreenMode.OFF) {
                    return mode;
                }
            } catch (IllegalArgumentException e) {
                if (!configuredModeName.equals(loggedInvalidFullscreenMode)) {
                    loggedInvalidFullscreenMode = configuredModeName;
                    LOGGER.warn(
                        "Unknown last fullscreen mode '{}', falling back to {}",
                        configuredModeName,
                        FullscreenMode.EXCLUSIVE
                    );
                }
            }
        }

        return FullscreenMode.EXCLUSIVE;
    }

    public static void synchronize(Minecraft client) {
        if (synchronizing) {
            return;
        }

        SodiumGameOptions options = ActiniumRuntime.options();
        FullscreenMode desiredMode = resolveConfiguredMode(options);
        if (options.window.fullscreenMode == null && client.gameSettings.fullScreen) {
            desiredMode = FullscreenMode.EXCLUSIVE;
            options.window.fullscreenMode = desiredMode.name();
        }
        if (isWindowStateCompatible(client, desiredMode)) {
            return;
        }

        synchronizing = true;

        try {
            switch (desiredMode) {
                case OFF -> applyWindowed(client);
                case EXCLUSIVE -> applyExclusiveFullscreen(client);
                case BORDERLESS -> applyBorderlessFullscreen(client);
            }
        } finally {
            synchronizing = false;
        }
    }

    private static boolean isWindowStateCompatible(Minecraft client, FullscreenMode desiredMode) {
        return switch (desiredMode) {
            case OFF -> !client.isFullScreen() && !Display.isFullscreen() && !Display.isBorderless();
            case EXCLUSIVE -> client.isFullScreen() && Display.isFullscreen() && !Display.isBorderless();
            case BORDERLESS -> client.isFullScreen() && Display.isBorderless();
        };
    }

    private static void applyWindowed(Minecraft client) {
        if (Display.isBorderless()) {
            Display.setBorderless(false);
        }
        if (Display.isFullscreen()) {
            Display.setFullscreen(false);
        }
        setFullscreenState(client, false);
        updateClientDisplaySize(client);
    }

    private static void applyExclusiveFullscreen(Minecraft client) {
        if (Display.isBorderless()) {
            Display.setBorderless(false);
        }
        Display.setFullscreen(true);
        setFullscreenState(client, true);
        updateClientDisplaySize(client);
    }

    private static void applyBorderlessFullscreen(Minecraft client) {
        if (Display.isFullscreen()) {
            Display.setFullscreen(false);
        }
        Display.setBorderless(true);
        setFullscreenState(client, true);
        updateClientDisplaySize(client);
    }

    private static void setFullscreenState(Minecraft client, boolean fullscreen) {
        ((MinecraftAccessor) client).celeritas$setFullscreen(fullscreen);
        client.gameSettings.fullScreen = fullscreen;
    }

    private static void updateClientDisplaySize(Minecraft client) {
        int width = Display.getFramebufferWidth();
        int height = Display.getFramebufferHeight();

        if (width > 0 && height > 0) {
            client.displayWidth = width;
            client.displayHeight = height;
            client.resize(width, height);
        }
    }
}
