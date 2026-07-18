package net.caffeinemc.mods.sodium.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ButtonThemeTest {
    @Test
    void retainsAllStateColors() {
        ButtonTheme theme = new ButtonTheme(1, 2, 3, 4, 5, 6);
        assertEquals(1, theme.theme());
        assertEquals(2, theme.themeLighter());
        assertEquals(3, theme.themeDarker());
        assertEquals(4, theme.backgroundHighlight());
        assertEquals(5, theme.backgroundDefault());
        assertEquals(6, theme.backgroundInactive());
    }
}
