package com.gtnewhorizons.angelica.glsm.compat;

/**
 * Simple holder for fog color values captured from vanilla rendering.
 * Used by terrain renderers to access fog state.
 * Ported from Angelica.
 */
public class FogHelper {
    public static float red;
    public static float green;
    public static float blue;

    private FogHelper() {
    }

    public static void setFogColor(float r, float g, float b) {
        red = r;
        green = g;
        blue = b;
    }
}
