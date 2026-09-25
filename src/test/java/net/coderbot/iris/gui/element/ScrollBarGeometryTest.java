package net.coderbot.iris.gui.element;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the scrollbar drag factor maps "handle travels the whole track" to
 * "content scrolls from top to bottom", so the handle keeps up with the cursor on
 * long lists instead of crawling behind it.
 */
class ScrollBarGeometryTest {
    // Vanilla GuiSlot handle sizing: viewport^2 / contentHeight, clamped to [32, viewport - 8]
    private static int handleHeight(int contentHeight, int viewportHeight) {
        return Math.min(Math.max((int) ((float) viewportHeight * viewportHeight / contentHeight), 32), viewportHeight - 8);
    }

    @Test
    void fullHandleTravelCoversTheWholeScrollRange() {
        final int viewportHeight = 400;

        for (int contentHeight : new int[] {500, 800, 2000, 5000}) {
            final float factor = ScrollBarGeometry.dragFactor(contentHeight, viewportHeight, 0);
            final int travel = viewportHeight - handleHeight(contentHeight, viewportHeight);
            final int scrollable = contentHeight - viewportHeight;

            assertEquals((float) scrollable, factor * travel, 0.01f,
                "contentHeight=" + contentHeight);
        }
    }

    @Test
    void longListScalesHandleSpeedUpToContentRatio() {
        // 2000px of content in a 400px viewport: the handle is 80px tall, its track is
        // 320px, so one cursor pixel must scroll 1600/320 = 5 content pixels.
        assertEquals(5.0f, ScrollBarGeometry.dragFactor(2000, 400, 0), 0.0001f);
    }

    @Test
    void headerPaddingExtendsTheScrollRange() {
        // 10px of header padding adds 10px of scrollable content
        assertEquals(1610f / 320f, ScrollBarGeometry.dragFactor(2000, 400, 10), 0.0001f);
    }

    @Test
    void shortListFallsBackToVanillaMinimumFactor() {
        // Content shorter than the viewport: nothing to scroll, factor collapses to
        // vanilla's 1/(viewport - (viewport - 8)) = 1/8 without dividing by zero
        assertEquals(0.125f, ScrollBarGeometry.dragFactor(300, 400, 0), 0.0001f);
    }
}
