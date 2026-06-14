package com.dhj.actinium.config;

import org.taumc.celeritas.CeleritasVintage;

public final class ActiniumRuntimeOptions {
    private static final String ALLOW_DIRECT_MEMORY_ACCESS_PROPERTY = "actinium.allowDirectMemoryAccess";

    private ActiniumRuntimeOptions() {
    }

    public static boolean allowDirectMemoryAccess() {
        String override = System.getProperty(ALLOW_DIRECT_MEMORY_ACCESS_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return CeleritasVintage.options().advanced.allowDirectMemoryAccess;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }
}
