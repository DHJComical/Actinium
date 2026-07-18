package net.caffeinemc.mods.sodium.client.gui.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetButtonTest {
    @Test
    void activationMatchesShiftHoverChangedContract() {
        assertTrue(ResetButton.isActive(true, true, true));
        assertFalse(ResetButton.isActive(false, true, true));
        assertFalse(ResetButton.isActive(true, false, true));
        assertFalse(ResetButton.isActive(true, true, false));
    }
}
