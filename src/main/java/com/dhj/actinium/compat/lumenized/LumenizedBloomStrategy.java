package com.dhj.actinium.compat.lumenized;

/** Resolves Lumenized rendering options against Actinium's default safe mode. */
public final class LumenizedBloomStrategy {
    /** JVM property that opts out of safe mode and restores Lumenized's rendering paths. */
    public static final String EXPERIMENTAL_UNREAL_PROPERTY = "actinium.allowLumenizedUnrealBloom";

    private LumenizedBloomStrategy() {
    }

    /** Returns the requested style only when the complete experimental opt-out is enabled. */
    public static int effectiveStyle(int requestedStyle, boolean allowExperimentalUnreal) {
        return allowExperimentalUnreal ? requestedStyle : 0;
    }

    /** Disables Lumenized's depth texture path unless the complete experimental opt-out is enabled. */
    public static boolean effectiveDepthTextureHook(boolean requestedHook, boolean allowExperimentalUnreal) {
        return allowExperimentalUnreal && requestedHook;
    }

    /** Reads the opt-out switch when configuration or a protected rendering option is evaluated. */
    public static boolean isExperimentalUnrealAllowed() {
        return Boolean.getBoolean(EXPERIMENTAL_UNREAL_PROPERTY);
    }
}
