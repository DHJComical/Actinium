package com.gtnewhorizons.angelica.glsm.hooks;

/**
 * Configuration holder for the GLSM (GL State Manager).
 * Provides runtime configuration for brightness, shader behavior, etc.
 * Ported from Angelica.
 */
public class GLSMConfig {
    /** Last set brightness X value (lightmap U coordinate). */
    public static float lastBrightnessX = 1.0f;
    /** Last set brightness Y value (lightmap V coordinate). */
    public static float lastBrightnessY = 1.0f;

    /** Whether FFP (Fixed Function Pipeline) emulation is enabled. */
    public static boolean ffpEmulationEnabled = false;

    /** Whether GL state caching is enabled. */
    public static boolean cacheEnabled = true;

    /** Whether to record display list commands. */
    public static boolean recordDisplayLists = false;

    private GLSMConfig() {
    }

    public static void setLastBrightness(float x, float y) {
        lastBrightnessX = x;
        lastBrightnessY = y;
    }
}
