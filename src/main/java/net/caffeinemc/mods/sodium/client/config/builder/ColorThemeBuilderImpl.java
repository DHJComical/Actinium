package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.ColorThemeBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;

final class ColorThemeBuilderImpl implements ColorThemeBuilder {
    private Integer baseTheme;
    private Integer themeHighlight;
    private Integer themeDisabled;

    ColorTheme build() {
        if (this.baseTheme == null) {
            throw new IllegalStateException("Base theme color must be set");
        }
        if (this.themeHighlight == null && this.themeDisabled == null) {
            return ColorTheme.fromBase(this.baseTheme);
        }
        if (this.themeHighlight == null || this.themeDisabled == null) {
            throw new IllegalStateException("Highlight and disabled theme colors must both be set");
        }
        return new ColorTheme(this.baseTheme, this.themeHighlight, this.themeDisabled);
    }

    @Override
    public ColorThemeBuilder setBaseThemeRGB(int theme) {
        this.baseTheme = opaque(theme);
        this.themeHighlight = null;
        this.themeDisabled = null;
        return this;
    }

    @Override
    public ColorThemeBuilder setFullThemeRGB(int theme, int themeHighlight, int themeDisabled) {
        this.baseTheme = opaque(theme);
        this.themeHighlight = opaque(themeHighlight);
        this.themeDisabled = opaque(themeDisabled);
        return this;
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }
}
