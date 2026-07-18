package net.caffeinemc.mods.sodium.api.config.structure;

/**
 * Defines the three RGB colors consumed by a platform-specific Sodium GUI theme.
 */
public interface ColorThemeBuilder {
    /** Derives highlight and disabled colors from one base RGB color. */
    ColorThemeBuilder setBaseThemeRGB(int theme);

    /** Sets base, highlight, and disabled RGB colors explicitly. */
    ColorThemeBuilder setFullThemeRGB(int theme, int themeHighlight, int themeDisabled);
}
