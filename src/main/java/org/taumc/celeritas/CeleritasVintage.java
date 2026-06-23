package org.taumc.celeritas;

import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;

/**
 * Compatibility bridge for code that still references the old product shell.
 * The real mod entrypoint now lives in {@code com.dhj.actinium.Actinium}.
 */
public class CeleritasVintage {
    public static final String MODID = CeleritasRuntime.MODID;
    public static String VERSION = CeleritasRuntime.version();

    private CeleritasVintage() {
    }

    public static Logger logger() {
        return CeleritasRuntime.logger();
    }

    public static SodiumGameOptions options() {
        return CeleritasRuntime.options();
    }
}
