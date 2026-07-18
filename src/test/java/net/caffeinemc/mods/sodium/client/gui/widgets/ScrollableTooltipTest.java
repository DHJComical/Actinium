package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollableTooltipTest {
    private static final GuiRect AREA = new GuiRect(100, 80, 200, 120);
    private static final GuiRect CONTROL = new GuiRect(140, 110, 20, 18);

    @Test
    void placesTooltipBesideMouseWhenThereIsRoom() {
        assertEquals(new GuiRect(153, 133, 80, 40),
                ScrollableTooltip.calculateOverlayBounds(AREA, CONTROL, 80, 40, 150, 130));
    }

    @Test
    void flipsTooltipAwayFromBottomRightBoundary() {
        GuiRect bounds = ScrollableTooltip.calculateOverlayBounds(AREA, CONTROL, 80, 40, 295, 195);

        assertEquals(new GuiRect(212, 152, 80, 40), bounds);
        assertEquals(bounds, bounds.intersection(AREA));
    }

    @Test
    void clampsTooltipToTopLeftBoundary() {
        GuiRect bounds = ScrollableTooltip.calculateOverlayBounds(AREA, CONTROL, 80, 40, 95, 75);

        assertEquals(new GuiRect(100, 80, 80, 40), bounds);
        assertEquals(bounds, bounds.intersection(AREA));
    }
}
