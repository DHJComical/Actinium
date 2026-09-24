package com.dhj.actinium.compat.glsm;

import com.dhj.actinium.mixin.vintage.core.terrain.AccessorGlStateManagerTextureState;
import com.dhj.actinium.runtime.ActiniumRuntime;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMHooks;
import net.minecraft.client.renderer.GlStateManager;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/** Creates the host callback that keeps vanilla's texture binding mirror aligned with GLSM. */
public final class VanillaTextureMirrorCompat {
    /** Runtime field aliases for MCP, renamed, SRG, and official obfuscated environments. */
    private static final String[] TEXTURE_STATE_FIELD_NAMES = {
        "textureState", "TEXTURES", "field_179174_p", "r"
    };

    /** Prevents instances because this class only creates the GLSM host callback. */
    private VanillaTextureMirrorCompat() {
    }

    /**
     * Resolves vanilla's texture-state array once and returns a callback for every executed
     * {@code GL_TEXTURE_2D} bind. The returned callback performs only an array lookup and accessor
     * write, keeping reflection off the binding hot path.
     */
    public static GLSMHooks.TextureBindSyncCallback createCallback() {
        final Object[] textureStates = readVanillaTextureStates();
        return (unit, textureId) -> {
            if (unit >= 0 && unit < textureStates.length
                && textureStates[unit] instanceof AccessorGlStateManagerTextureState state) {
                state.celeritas$setTextureName(textureId);
            }
        };
    }

    /**
     * Reads the package-private texture-state array once during construction. Reflection is needed
     * only to obtain the array; its elements are updated later through the configured Mixin accessor.
     */
    private static Object[] readVanillaTextureStates() {
        NoSuchFieldException missingField = null;
        for (String name : TEXTURE_STATE_FIELD_NAMES) {
            final Field texturesField;
            try {
                texturesField = GlStateManager.class.getDeclaredField(name);
            } catch (NoSuchFieldException exception) {
                if (missingField != null) {
                    exception.addSuppressed(missingField);
                }
                missingField = exception;
                ActiniumRuntime.logger().debug(
                    "Vanilla GlStateManager field '{}' was not found; trying the next mapped name",
                    name, exception);
                continue;
            }
            try {
                texturesField.setAccessible(true);
                MethodHandle getter = MethodHandles.lookup().unreflectGetter(texturesField);
                return (Object[]) getter.invoke();
            } catch (Throwable exception) {
                ActiniumRuntime.logger().error("Failed to read vanilla GlStateManager texture states", exception);
                if (exception instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Failed to read GlStateManager." + name, exception);
            }
        }
        IllegalStateException exception = new IllegalStateException(
            "Vanilla GlStateManager texture-state field is unavailable", missingField);
        ActiniumRuntime.logger().error("Failed to resolve vanilla GlStateManager texture states", exception);
        throw exception;
    }
}
