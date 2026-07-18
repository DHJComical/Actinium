package net.caffeinemc.mods.sodium.client.gui.input;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayInputArbiterTest {
    @Test
    void capturesPointerOnlyInsideVisibleOverlay() {
        GuiRect overlay = new GuiRect(20, 20, 100, 60);

        assertTrue(OverlayInputArbiter.captures(true, true, overlay, 40, 40));
        assertFalse(OverlayInputArbiter.captures(false, true, overlay, 40, 40));
        assertFalse(OverlayInputArbiter.captures(true, false, overlay, 40, 40));
        assertFalse(OverlayInputArbiter.captures(true, true, overlay, 10, 10));
    }
}
