package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;

/** Immutable colors for a flat button's enabled, hover, and inactive states. */
public record ButtonTheme(int theme, int themeLighter, int themeDarker,
                          int backgroundHighlight, int backgroundDefault, int backgroundInactive) {
    public ButtonTheme(ColorTheme theme, int backgroundHighlight, int backgroundDefault, int backgroundInactive) {
        this(theme.theme(), theme.themeHighlight(), theme.themeDisabled(),
                backgroundHighlight, backgroundDefault, backgroundInactive);
    }
}
