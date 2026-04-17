package com.dhj.actinium.shader;

import com.dhj.actinium.celeritas.ActiniumShaderProvider;
import com.dhj.actinium.celeritas.ActiniumShaderProviderHolder;
import com.dhj.actinium.celeritas.ActiniumShaders;
import org.jetbrains.annotations.Nullable;

public final class ActiniumShaderEntrypoint {
    private ActiniumShaderEntrypoint() {
    }

    public static void initialize() {
        ActiniumShaders.initialize();
    }

    public static void registerProvider(@Nullable ActiniumShaderProvider provider) {
        ActiniumShaders.registerProvider(provider);
    }

    public static @Nullable ActiniumShaderProvider getProvider() {
        return ActiniumShaderProviderHolder.getProvider();
    }
}
