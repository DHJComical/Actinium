package net.caffeinemc.mods.sodium.client.config.structure;

/**
 * Immutable ARGB theme data kept independent from Minecraft rendering classes.
 */
public record ColorTheme(int theme, int themeHighlight, int themeDisabled) {
    private static final ColorTheme[] PRESETS = {
            fromBase(0xFFE494A5),
            fromBase(0xFFAB94E4),
            fromBase(0xFFCDE494),
            fromBase(0xFFD394E4),
            fromBase(0xFFE4D394)
    };

    /** Derives readable highlight and disabled variants from a base ARGB color. */
    public static ColorTheme fromBase(int theme) {
        return new ColorTheme(theme, adjust(theme, 0.30F), adjust(theme, -0.23F));
    }

    /** Selects the deterministic upstream-style default for a configuration ID. */
    public static ColorTheme defaultFor(String configId) {
        return PRESETS[Math.floorMod(configId.hashCode(), PRESETS.length)];
    }

    private static int blend(int first, int second, float secondWeight) {
        float firstWeight = 1.0F - secondWeight;
        int red = Math.round(((first >>> 16) & 0xFF) * firstWeight + ((second >>> 16) & 0xFF) * secondWeight);
        int green = Math.round(((first >>> 8) & 0xFF) * firstWeight + ((second >>> 8) & 0xFF) * secondWeight);
        int blue = Math.round((first & 0xFF) * firstWeight + (second & 0xFF) * secondWeight);
        return 0xFF000000 | red << 16 | green << 8 | blue;
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
