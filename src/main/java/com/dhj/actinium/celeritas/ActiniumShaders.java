package com.dhj.actinium.celeritas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ActiniumShaders {
    private static final Logger LOGGER = LogManager.getLogger("Actinium Shader");

    private ActiniumShaders() {
    }

    public static void initialize() {
        ActiniumShaderProvider provider = ActiniumShaderProviderHolder.getProvider();

        if (provider == null) {
            provider = new ActiniumIrisShaderProvider();
            ActiniumShaderProviderHolder.setProvider(provider);
            LOGGER.info("Installed default Actinium Shader provider");
        }
    }

    public static void registerProvider(@Nullable ActiniumShaderProvider provider) {
        ActiniumShaderProviderHolder.setProvider(provider);

        if (provider != null) {
            LOGGER.info("Registered Actinium shader provider: {}", provider.getClass().getName());
        }
    }

    public static Logger logger() {
        return LOGGER;
    }
}
