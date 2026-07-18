package net.caffeinemc.mods.sodium.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {
    @Test
    void preservesUpstreamWidthInterpolationBoundaries() {
        assertLayoutContent(605, 360, new GuiRect(0, 0, 605, 360));
        assertLayoutContent(606, 360, new GuiRect(50, 6, 506, 348));
        assertLayoutContent(640, 360, new GuiRect(50, 6, 540, 348));
        assertLayoutContent(655, 360, new GuiRect(50, 6, 555, 348));
        assertLayoutContent(960, 540, new GuiRect(202, 6, 555, 528));
    }

    @Test
    void onlyInsetsHeightWhenWidthIsInset() {
        assertLayoutContent(605, 313, new GuiRect(0, 0, 605, 313));
        assertLayoutContent(606, 312, new GuiRect(50, 0, 506, 312));
        assertLayoutContent(606, 313, new GuiRect(50, 6, 506, 301));
    }

    @Test
    void allocatesFullOptionViewportAndOverlayTooltip() {
        Layout.ScreenLayout layout = Layout.calculate(500, 300);

        assertEquals(new GuiRect(0, 20, 125, 280), layout.pages());
        assertEquals(new GuiRect(130, 25, 295, 245), layout.options());
        assertEquals(layout.options(), layout.tooltip());
        assertTrue(layout.tooltipOverlay());
    }

    @Test
    void preservesStrictActionButtonBreakpoints() {
        Layout.ScreenLayout belowMinimum = Layout.calculate(421, 240);
        assertTrue(belowMinimum.reserveBottomSpace());
        assertFalse(belowMinimum.stackActionButtons());

        Layout.ScreenLayout atMinimum = Layout.calculate(422, 240);
        assertFalse(atMinimum.reserveBottomSpace());
        assertFalse(atMinimum.stackActionButtons());

        assertTrue(Layout.calculate(423, 240).stackActionButtons());
        assertTrue(Layout.calculate(556, 300).stackActionButtons());
        assertFalse(Layout.calculate(557, 300).stackActionButtons());
        assertFalse(Layout.calculate(558, 300).stackActionButtons());
    }

    @Test
    void positionsThreeFixedSizeActionButtonsLikeUpstream() {
        Layout.ActionButtons horizontal = Layout.calculate(605, 360).actionButtons();
        assertEquals(new GuiRect(535, 335, 65, 20), horizontal.done());
        assertEquals(new GuiRect(465, 335, 65, 20), horizontal.apply());
        assertEquals(new GuiRect(395, 335, 65, 20), horizontal.undo());

        Layout.ActionButtons vertical = Layout.calculate(500, 300).actionButtons();
        assertEquals(new GuiRect(430, 275, 65, 20), vertical.done());
        assertEquals(new GuiRect(430, 250, 65, 20), vertical.apply());
        assertEquals(new GuiRect(430, 225, 65, 20), vertical.undo());
        assertFalse(intersects(layoutOptions(500, 300), vertical.undo()));
        assertFalse(intersects(layoutOptions(500, 300), vertical.apply()));
        assertFalse(intersects(layoutOptions(500, 300), vertical.done()));
        Layout.ActionButtons wide = Layout.calculate(605, 360).actionButtons();
        GuiRect wideOptions = layoutOptions(605, 360);
        assertFalse(intersects(wideOptions, wide.undo()));
        assertFalse(intersects(wideOptions, wide.apply()));
        assertFalse(intersects(wideOptions, wide.done()));
    }

    private static GuiRect layoutOptions(int width, int height) {
        return Layout.calculate(width, height).options();
    }

    private static boolean intersects(GuiRect first, GuiRect second) {
        return first.intersection(second).width() > 0 && first.intersection(second).height() > 0;
    }

    private static void assertLayoutContent(int width, int height, GuiRect expected) {
        assertEquals(expected, Layout.calculate(width, height).content());
    }
}
