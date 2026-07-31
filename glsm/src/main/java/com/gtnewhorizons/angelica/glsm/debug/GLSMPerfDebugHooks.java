package com.gtnewhorizons.angelica.glsm.debug;

import java.util.function.Supplier;

public final class GLSMPerfDebugHooks {
    private static volatile Supplier<String> extraStatsSupplier = () -> "";
    private static volatile Runnable enabledChangeListener = () -> { };

    private GLSMPerfDebugHooks() {
    }

    public static String getExtraStats() {
        return extraStatsSupplier.get();
    }

    public static void setExtraStatsSupplier(Supplier<String> supplier) {
        extraStatsSupplier = supplier != null ? supplier : () -> "";
    }

    public static void setEnabledChangeListener(Runnable listener) {
        enabledChangeListener = listener != null ? listener : () -> { };
    }

    public static void setConfiguredEnabled(boolean enabled) {
        if (GLSMPerfDebug.setConfiguredEnabled(enabled)) {
            extraStatsSupplier.get();
            enabledChangeListener.run();
        }
    }
}
