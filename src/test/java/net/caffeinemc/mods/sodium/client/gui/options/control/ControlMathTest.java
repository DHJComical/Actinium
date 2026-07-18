package net.caffeinemc.mods.sodium.client.gui.options.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlMathTest {
    @Test
    void mapsPointerToNearestValidStepAndClampsEdges() {
        assertEquals(2, ControlMath.sliderValue(2, 16, 2, -10, 100));
        assertEquals(10, ControlMath.sliderValue(2, 16, 2, 50, 100));
        assertEquals(16, ControlMath.sliderValue(2, 16, 2, 140, 100));
    }

    @Test
    void cyclesInBothDirectionsWithWraparound() {
        assertEquals(0, ControlMath.cycleIndex(3, 2, 1));
        assertEquals(2, ControlMath.cycleIndex(3, 0, -1));
        assertEquals(1, ControlMath.cycleIndex(3, 0, 4));
    }

    @Test
    void rejectsInvalidControlModels() {
        assertThrows(IllegalArgumentException.class, () -> ControlMath.sliderValue(10, 2, 1, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> ControlMath.sliderValue(0, 10, 3, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> ControlMath.cycleIndex(0, 0, 1));
    }
}
