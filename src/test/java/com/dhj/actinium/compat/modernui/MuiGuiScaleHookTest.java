package com.dhj.actinium.compat.modernui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuiGuiScaleHookTest {
    @Test
    void keepsManualRangeAtLeastOneThroughFive() {
        assertEquals(5, MuiGuiScaleHook.resolveMaximum(2, 2));
    }

    @Test
    void doesNotLetExternalHookReduceNativeMaximum() {
        assertEquals(6, MuiGuiScaleHook.resolveMaximum(6, 2));
    }

    @Test
    void preservesLargerExternalMaximum() {
        assertEquals(7, MuiGuiScaleHook.resolveMaximum(2, 7));
    }
}
