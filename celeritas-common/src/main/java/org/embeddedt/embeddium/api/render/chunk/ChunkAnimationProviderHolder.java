package org.embeddedt.embeddium.api.render.chunk;

import org.jetbrains.annotations.Nullable;

/**
 * Static registration point for the single active {@link ChunkAnimationProvider}.
 *
 * <p>Follows the same pattern as the shader provider holder: the host mod (Actinium) installs the
 * provider when a compatible animation mod is present, and the chunk renderer queries it without
 * holding a compile-time reference to any external mod class.</p>
 */
public final class ChunkAnimationProviderHolder {
    private static ChunkAnimationProvider provider;

    private ChunkAnimationProviderHolder() {
    }

    /**
     * Replaces the active animation provider. Passing {@code null} disables per-section chunk
     * animations.
     */
    public static void setProvider(@Nullable ChunkAnimationProvider provider) {
        ChunkAnimationProviderHolder.provider = provider;
    }

    /**
     * @return the active animation provider, or {@code null} if none is installed
     */
    @Nullable
    public static ChunkAnimationProvider getProvider() {
        return provider;
    }
}
