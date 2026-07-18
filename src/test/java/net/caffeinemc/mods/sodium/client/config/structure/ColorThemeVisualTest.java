package net.caffeinemc.mods.sodium.client.config.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorThemeVisualTest {
    @Test
    void derivesUpstreamHsvVariants() {
        ColorTheme theme = ColorTheme.fromBase(0xFFE494A5);

        assertEquals(0xFFE494A5, theme.theme());
        assertEquals(0xFFFFC0CD, theme.themeHighlight());
        assertEquals(0xFFAF808A, theme.themeDisabled());
    }
}
