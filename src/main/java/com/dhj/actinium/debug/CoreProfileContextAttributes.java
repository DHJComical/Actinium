package com.dhj.actinium.debug;

import net.minecraftforge.common.ForgeEarlyConfig;
import net.minecraftforge.common.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ContextAttribs;

import java.util.function.Consumer;

/**
 * Creates the core-profile context attributes required during early Minecraft startup.
 * Keeping this logic outside every Mixin configuration package allows transformed classes to load it normally.
 */
public final class CoreProfileContextAttributes {
    private static final Logger LOGGER = LogManager.getLogger("Celeritas");

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
     * Restores the Forge early OpenGL config after the core-profile context was created.
     *
     * <p>LWJGLXX reads these fields when creating the context and Cleanroom persists them back to
     * forge_early.cfg. Leaving the core-profile request in place breaks startup without Actinium
     * (Cleanroom's own default path expects the compatibility profile), so restore the requested
     * version/debug state and force the compatibility profile back on. The compatibility flag is
     * restored unconditionally: Actinium is the only writer that sets it to false.
     *
     * @param originalMajor OpenGL major version before Actinium's request
     * @param originalMinor OpenGL minor version before Actinium's request
     * @param originalDebug debug-context flag before Actinium's request
     */
    public static void restoreForgeEarlyCompatProfile(int originalMajor, int originalMinor, boolean originalDebug) {
        ForgeEarlyConfig.OPENGL_VERSION_MAJOR = originalMajor;
        ForgeEarlyConfig.OPENGL_VERSION_MINOR = originalMinor;
        ForgeEarlyConfig.OPENGL_COMPAT_PROFILE = true;
        ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT = originalDebug;
    }

    /**
     * Persists the restored compatibility profile back to {@code forge_early.cfg}.
     *
     * <p>LWJGLXX calls {@code ConfigManager.sync(ForgeEarlyConfig.class)} while creating the
     * core-profile context, which writes the in-memory core-profile request (compatibility profile
     * disabled) to the file before {@link #restoreForgeEarlyCompatProfile} can undo it in memory.
     * Without this follow-up sync the file stays on the core profile and a later launch without
     * Actinium creates a core context whose fixed-pipeline startup code (SplashProgress texture
     * setup) fails with {@code GL_INVALID_ENUM}, kills the splash thread, and aborts the JVM on the
     * first {@code glAlphaFunc}. Sync again now that the fields are restored so the file always
     * matches the compatibility profile.</p>
     */
    public static void persistForgeEarlyCompatProfile() {
        try {
            ConfigManager.sync(ForgeEarlyConfig.class);
        } catch (RuntimeException syncFailure) {
            // The in-memory state is already restored and this run is unaffected; only the file
            // would keep the core-profile request and break the next launch without Actinium.
            LOGGER.error("Failed to persist the restored compatibility profile to forge_early.cfg", syncFailure);
        }
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
