package org.embeddedt.embeddium.api.shader;

import org.jetbrains.annotations.Nullable;

public final class ShaderProviderHolder {
    private static ShaderProvider provider;

    private ShaderProviderHolder() {
    }

    public static void setProvider(@Nullable ShaderProvider provider) {
        if (ShaderProviderHolder.provider == provider) {
            return;
        }

        if (ShaderProviderHolder.provider != null) {
            ShaderProviderHolder.provider.deleteShaders();
        }

        ShaderProviderHolder.provider = provider;
    }

    @Nullable
    public static ShaderProvider getProvider() {
        return provider;
    }

    public static boolean isActive() {
        return provider != null && provider.isShadersEnabled();
    }

    public static boolean isShadowPass() {
        return provider != null && provider.isShadowPass();
    }

    public static boolean shouldUseFaceCulling() {
        return provider == null || provider.shouldUseFaceCulling();
    }
}
