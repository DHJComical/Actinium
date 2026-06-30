package com.gtnewhorizons.angelica.glsm.debug;

import java.util.function.Supplier;

public final class GLSMPerfDebugHooks {
    private static volatile Supplier<String> extraStatsSupplier = () -> "";

    private GLSMPerfDebugHooks() {
    }

    public static String getExtraStats() {
        return extraStatsSupplier.get();
    }

    public static void setExtraStatsSupplier(Supplier<String> supplier) {
        extraStatsSupplier = supplier != null ? supplier : () -> "";
    }
}
