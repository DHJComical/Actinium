package com.dhj.actinium.render.terrain.compile.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LightDataCacheTest {
    @Test
    void keepsVanillaAoAtFullStrengthByDefault() {
        assertEquals(0.8f, LightDataCache.applyAmbientOcclusionLevel(0.8f, 1.0f), 0.0001f);
    }

    @Test
    void disablesVanillaAoWhenShaderPackLevelIsZero() {
        assertEquals(1.0f, LightDataCache.applyAmbientOcclusionLevel(0.2f, 0.0f), 0.0001f);
    }

    @Test
    void interpolatesVanillaAoWithShaderPackLevel() {
        assertEquals(0.9f, LightDataCache.applyAmbientOcclusionLevel(0.8f, 0.5f), 0.0001f);
    }
}
