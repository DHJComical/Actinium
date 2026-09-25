package com.gtnewhorizons.angelica.glsm.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class GLSMPerfDebugHooks {
    /**
     * Extra stats providers appended to the periodic perf report, concatenated in registration
     * order. Registration and invocation are both expected on the render thread, so a plain
     * ArrayList is used instead of a concurrent collection.
     */
    private static final List<Supplier<String>> statsProviders = new ArrayList<>();
    private static volatile Runnable enabledChangeListener = () -> { };

    private GLSMPerfDebugHooks() {
    }

    public static String getExtraStats() {
        if (statsProviders.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (Supplier<String> provider : statsProviders) {
            final String stats = provider.get();
            if (stats == null || stats.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(stats);
        }
        return sb.toString();
    }

    /**
     * Registers an extra stats provider. Providers are invoked and concatenated into the
     * periodic report in registration order.
     */
    public static void addStatsProvider(Supplier<String> provider) {
        statsProviders.add(Objects.requireNonNull(provider, "provider"));
    }

    public static boolean removeStatsProvider(Supplier<String> provider) {
        return statsProviders.remove(Objects.requireNonNull(provider, "provider"));
    }

    /**
     * Legacy single-slot entry point: clears all registered providers and installs the given
     * one. A null supplier only clears the list.
     */
    public static void setExtraStatsSupplier(Supplier<String> supplier) {
        statsProviders.clear();
        if (supplier != null) {
            statsProviders.add(supplier);
        }
    }

    public static void setEnabledChangeListener(Runnable listener) {
        enabledChangeListener = listener != null ? listener : () -> { };
    }

    public static void setConfiguredEnabled(boolean enabled) {
        if (GLSMPerfDebug.setConfiguredEnabled(enabled)) {
            for (Supplier<String> provider : statsProviders) {
                provider.get();
            }
            enabledChangeListener.run();
        }
    }
}
