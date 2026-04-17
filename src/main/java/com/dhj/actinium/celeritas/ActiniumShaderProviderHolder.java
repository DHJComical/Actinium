package com.dhj.actinium.celeritas;

import org.jetbrains.annotations.Nullable;

public final class ActiniumShaderProviderHolder {
    private static ActiniumShaderProvider provider;

    private ActiniumShaderProviderHolder() {
    }

    public static void setProvider(@Nullable ActiniumShaderProvider provider) {
        if (ActiniumShaderProviderHolder.provider == provider) {
            return;
        }

        if (ActiniumShaderProviderHolder.provider != null) {
            ActiniumShaderProviderHolder.provider.deleteShaders();
        }

        ActiniumShaderProviderHolder.provider = provider;
    }

    @Nullable
    public static ActiniumShaderProvider getProvider() {
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
