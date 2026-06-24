package com.dhj.actinium.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;

public final class ActiniumRuntime {
    public static final String MODID = "actinium";

    private static final Logger LOGGER = LogManager.getLogger("Actinium");
    private static final SodiumGameOptions CONFIG = loadConfig();

    private static volatile String version = "unknown";

    private ActiniumRuntime() {
    }

    public static Logger logger() {
        return LOGGER;
    }

    public static SodiumGameOptions options() {
        return CONFIG;
    }

    public static String version() {
        return version;
    }

    public static void setVersion(String version) {
        if (version != null && !version.isBlank()) {
            ActiniumRuntime.version = version;
        }
    }

    private static SodiumGameOptions loadConfig() {
        try {
            return SodiumGameOptions.load();
        } catch (Throwable t) {
            LOGGER.error("Failed to load configuration file", t);
            LOGGER.error("Using default configuration file in read-only mode");
            SodiumGameOptions config = SodiumGameOptions.defaults();
            config.setReadOnly();
            return config;
        }
    }
}
