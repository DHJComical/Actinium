package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.caffeinemc.mods.sodium.client.gui.options.control.OptionControl;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void contentHeightMatchesRenderedLinesWithoutTrailingSpacing() {
        assertEquals(9, ScrollableTooltip.calculateContentHeight(1, 9));
        assertEquals(31, ScrollableTooltip.calculateContentHeight(3, 9));
        assertEquals(0, ScrollableTooltip.calculateContentHeight(0, 9));
    }

    @Test
    void focusedOptionDoesNotCreateTooltipInViewportBlankSpace() {
        OptionControl<?> focused = new TestControl();

        assertNull(ScrollableTooltip.selectTarget(null, focused, null, false));
        assertNull(ScrollableTooltip.selectTarget(null, focused, null, true));
    }

    @Test
    void visibleTooltipIsRetainedWhilePointerIsInsideItsOverlay() {
        OptionControl<?> current = new TestControl();

        assertEquals(current, ScrollableTooltip.selectTarget(null, null, current, true));
        assertNull(ScrollableTooltip.selectTarget(null, null, current, false));
    }

    @Test
    void hoveredControlAlwaysWinsWithinViewport() {
        OptionControl<?> hovered = new TestControl();
        OptionControl<?> focused = new TestControl();

        assertEquals(hovered, ScrollableTooltip.selectTarget(hovered, focused, null, true));
        assertEquals(hovered, ScrollableTooltip.selectTarget(hovered, focused, null, false));
    }

    private static final class TestControl extends OptionControl<Option> {
        private TestControl() {
            super(null, null, null);
        }

        @Override
        protected int controlWidth() {
            return 0;
        }

        @Override
        protected void renderControl(GuiRect control, boolean enabled, int mouseX, int mouseY) {
        }

        @Override
        protected boolean onMouseClicked(GuiRect control, int mouseX, int mouseY) {
            return false;
        }

        @Override
        protected boolean onKeyPressed(int keyCode) {
            return false;
        }
    }
}
