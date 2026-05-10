package com.dhj.actinium.celeritas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class Shaders {
    private static final Logger LOGGER = LogManager.getLogger("Actinium Shader");

    private Shaders() {
    }

    public static void initialize() {
        ShaderProviderHolder.setProvider(null);
    }

    public static void registerProvider(@Nullable ShaderProvider provider) {
        ShaderProviderHolder.setProvider(provider);

        if (provider != null) {
            LOGGER.info("Registered Actinium shader provider: {}", provider.getClass().getName());
        }
    }

    public static Logger logger() {
        return LOGGER;
    }
}
