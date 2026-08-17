package me.flashyreese.mods.reeses_sodium_options.client.gui.theme;

/**
 * Actinium's own theme color model: provides the ARGB theme-color triple
 * used by RSO tab headers, option controls and tooltip borders, matching
 * the upstream palette. No Sodium/Embeddium config model is involved.
 */
public final class ActiniumTheme {
    private final int theme;
    private final int themeHighlight;
    private final int themeDisabled;

    public ActiniumTheme(int theme, int themeHighlight, int themeDisabled) {
        this.theme = theme;
        this.themeHighlight = themeHighlight;
        this.themeDisabled = themeDisabled;
    }

    /** Derives highlight/disabled variants from a base color (same algorithm as upstream ColorTheme.fromBase). */
    public static ActiniumTheme fromBase(int theme) {
        return new ActiniumTheme(theme, adjust(theme, 0.30F), adjust(theme, -0.23F));
    }

    /** Deterministically picks the default theme (hashed by configId, matching the upstream palette). */
    public static ActiniumTheme defaultFor(String configId) {
        int[] presets = {
                0xFFE494A5,
                0xFFAB94E4,
                0xFFCDE494,
                0xFFD394E4,
                0xFFE4D394
        };
        return fromBase(presets[Math.floorMod(configId.hashCode(), presets.length)]);
    }

    public int theme() {
        return this.theme;
    }

    public int themeHighlight() {
        return this.themeHighlight;
    }

    public int themeDisabled() {
        return this.themeDisabled;
    }

    private static int adjust(int color, float factor) {
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float difference = max - min;
        float hue;
        if (difference == 0.0F) hue = 0.0F;
        else if (max == red) hue = ((green - blue) / difference * 0.1666F + 1.0F) % 1.0F;
        else if (max == green) hue = ((blue - red) / difference * 0.1666F + 0.333F) % 1.0F;
        else hue = ((red - green) / difference * 0.1666F + 0.666F) % 1.0F;
        float saturation = max == 0.0F ? 0.0F : difference / max;
        saturation = Math.max(0.0F, Math.min(1.0F, saturation * (1.0F - Math.abs(factor))));
        float value = Math.max(0.0F, Math.min(1.0F, max * (1.0F + factor)));
        float h = hue * 6.0F;
        int sector = (int) h % 6;
        float fraction = h - (int) h;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - fraction * saturation);
        float t = value * (1.0F - (1.0F - fraction) * saturation);
        float r = sector == 0 ? value : sector == 1 ? q : sector == 2 ? p : sector == 3 ? p : sector == 4 ? t : value;
        float g = sector == 0 ? t : sector == 1 ? value : sector == 2 ? value : sector == 3 ? q : p;
        float b = sector == 0 ? p : sector == 1 ? p : sector == 2 ? t : sector == 3 ? value : sector == 4 ? value : q;
        return 0xFF000000 | ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
    }
}
