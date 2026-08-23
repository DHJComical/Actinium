package net.coderbot.iris.gui.element.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the shader option slider's drag mapping is the inverse of its render
 * mapping, so the handle tracks the cursor one-to-one instead of lagging behind it.
 */
class SliderGeometryTest {
    private static final int X = 40;
    private static final int WIDTH = 200;

    @Test
    void handleCenterDragsBackToTheSameIndexWhenCellsAreWiderThanTheHandle() {
        // Typical shader option counts: each cell is wider than the 6px handle, so
        // grabbing the handle center and moving must select the very same index.
        for (int valueCount : new int[] {2, 3, 5, 21}) {
            for (int index = 0; index < valueCount; index++) {
                final int handleCenter = SliderGeometry.sliderXForIndex(X, WIDTH, index, valueCount)
                    + SliderGeometry.ACTIVE_SLIDER_WIDTH / 2;

                assertEquals(index, SliderGeometry.valueIndexForMouseX(handleCenter, X, WIDTH, valueCount),
                    "valueCount=" + valueCount + " index=" + index);
            }
        }
    }

    @Test
    void trackEndpointsMapToFirstAndLastValue() {
        assertEquals(0, SliderGeometry.valueIndexForMouseX(X + SliderGeometry.TRACK_INSET, X, WIDTH, 21));
        assertEquals(20, SliderGeometry.valueIndexForMouseX(X + WIDTH - SliderGeometry.TRACK_INSET, X, WIDTH, 21));

        // Positions beyond the track clamp to the endpoints instead of overflowing
        assertEquals(0, SliderGeometry.valueIndexForMouseX(X - 50, X, WIDTH, 21));
        assertEquals(20, SliderGeometry.valueIndexForMouseX(X + WIDTH + 50, X, WIDTH, 21));
    }

    @Test
    void mappingIsMonotonicallyNonDecreasingAcrossTheTrack() {
        int previous = SliderGeometry.valueIndexForMouseX(0, X, WIDTH, 21);

        for (int mouseX = 1; mouseX <= X + WIDTH; mouseX++) {
            final int current = SliderGeometry.valueIndexForMouseX(mouseX, X, WIDTH, 21);

            assertTrue(current >= previous, "mouseX=" + mouseX);
            previous = current;
        }
    }

    @Test
    void largeValueCountsKeepDriftWithinHalfAHandleWidth() {
        // When a cell is narrower than the handle the grab point can shift by at most
        // about half a handle width (3px); at 101 values a cell is 1.86px wide, so the
        // worst drift rounds out to 2 cells. The selection must never drift further.
        final int valueCount = 101;

        for (int index = 0; index < valueCount; index++) {
            final int handleCenter = SliderGeometry.sliderXForIndex(X, WIDTH, index, valueCount)
                + SliderGeometry.ACTIVE_SLIDER_WIDTH / 2;
            final int mapped = SliderGeometry.valueIndexForMouseX(handleCenter, X, WIDTH, valueCount);

            assertTrue(Math.abs(mapped - index) <= 2,
                "valueCount=" + valueCount + " index=" + index + " mapped=" + mapped);
        }
    }
}
