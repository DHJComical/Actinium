package org.taumc.celeritas;

import com.dhj.actinium.runtime.ActiniumRuntime;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;

public final class CeleritasRuntime {
    public static final String MODID = ActiniumRuntime.MODID;

    private CeleritasRuntime() {
    }

    public static Logger logger() {
        return ActiniumRuntime.logger();
    }

    public static SodiumGameOptions options() {
        return ActiniumRuntime.options();
    }

    public static String version() {
        return ActiniumRuntime.version();
    }

    public static void setVersion(String version) {
        ActiniumRuntime.setVersion(version);
    }
}
