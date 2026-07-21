package com.dhj.actinium.compat.lumenized;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LumenizedBloomStrategyTest {
    @Test
    void safeModeUsesSimpleBloomForEveryRequestedStyle() {
        assertEquals(0, LumenizedBloomStrategy.effectiveStyle(2, false));
        assertEquals(0, LumenizedBloomStrategy.effectiveStyle(0, false));
        assertEquals(0, LumenizedBloomStrategy.effectiveStyle(1, false));
    }

    @Test
    void safeModeDisablesDepthTextureHook() {
        assertFalse(LumenizedBloomStrategy.effectiveDepthTextureHook(true, false));
        assertFalse(LumenizedBloomStrategy.effectiveDepthTextureHook(false, false));
    }

    @Test
    void experimentalSwitchPreservesRequestedRenderingPaths() {
        assertEquals(2, LumenizedBloomStrategy.effectiveStyle(2, true));
        assertEquals(1, LumenizedBloomStrategy.effectiveStyle(1, true));
        assertTrue(LumenizedBloomStrategy.effectiveDepthTextureHook(true, true));
        assertFalse(LumenizedBloomStrategy.effectiveDepthTextureHook(false, true));
    }
}
