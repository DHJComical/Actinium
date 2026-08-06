package com.dhj.actinium.debug;

import net.minecraftforge.common.ForgeEarlyConfig;
import org.lwjgl.opengl.ContextAttribs;

import java.util.function.Consumer;

/**
 * Creates the core-profile context attributes required during early Minecraft startup.
 * Keeping this logic outside every Mixin configuration package allows transformed classes to load it normally.
 */
public final class CoreProfileContextAttributes {
    /**
     * Prevents instantiation because context attribute construction is stateless.
     */
    private CoreProfileContextAttributes() {
    }

    /**
     * Configures one context attribute object in place.
     *
     * <p>The lwjglx compatibility implementation returns {@code null} from these mutator methods, so their
     * return values must not be chained or retained.</p>
     *
     * @param major requested OpenGL major version
     * @param minor requested OpenGL minor version
     * @param lwjglDebug whether the debug context was requested at startup
     * @return configured context attributes
     */
    public static ContextAttribs create(int major, int minor, boolean lwjglDebug) {
        ContextAttribs attributes = new ContextAttribs(major, minor);
        return configure(
            attributes,
            value -> value.withProfileCore(true),
            value -> value.withForwardCompatible(true),
            value -> value.withDebug(lwjglDebug)
        );
    }

    /**
     * Configures the Forge early OpenGL context used by Cleanroom's LWJGLXX implementation.
     *
     * <p>LWJGLXX ignores {@code Display.create(PixelFormat, ContextAttribs)} and reads the desired version,
     * profile, and debug state from {@code ForgeEarlyConfig}. Without this override, macOS can downgrade a
     * requested modern compatibility-profile context to the legacy 2.1 Metal context.</p>
     *
     * @param major requested OpenGL major version
     * @param minor requested OpenGL minor version
     * @param lwjglDebug whether the debug context was requested at startup
     */
    public static void applyForgeEarlyCoreProfile(int major, int minor, boolean lwjglDebug) {
        ForgeEarlyConfig.OPENGL_VERSION_MAJOR = major;
        ForgeEarlyConfig.OPENGL_VERSION_MINOR = minor;
        ForgeEarlyConfig.OPENGL_COMPAT_PROFILE = false;
        ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT = lwjglDebug;
    }

    /**
     * Applies context mutations without retaining compatibility-layer return values.
     *
     * @param attributes context object mutated by every operation
     * @param coreProfile enables the core profile
     * @param forwardCompatible enables forward-compatible behavior
     * @param debug configures the requested debug state
     * @param <T> concrete context attribute type
     * @return the original context object
     */
    static <T> T configure(
        T attributes,
        Consumer<T> coreProfile,
        Consumer<T> forwardCompatible,
        Consumer<T> debug
    ) {
        coreProfile.accept(attributes);
        forwardCompatible.accept(attributes);
        debug.accept(attributes);
        return attributes;
    }
}
