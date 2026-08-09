package com.dhj.actinium.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RevoScreenEffectsGradientTest {
    @AfterEach
    void clearPendingGradient() {
        RevoScreenEffectsGradient.poll(null, null, 0, 0);
    }

    @Test
    void returnsDeferredGradientParameters() {
        Object world = new Object();
        Object screen = new Object();
        RevoScreenEffectsGradient.defer(1280, 720, 0.625F, world, screen);

        RevoScreenEffectsGradient.Gradient gradient =
            RevoScreenEffectsGradient.poll(world, screen, 1280, 720);

        assertNotNull(gradient);
        assertEquals(1280, gradient.width());
        assertEquals(720, gradient.height());
        assertEquals(0.625F, gradient.progress());
    }

    @Test
    void laterDeferredGradientReplacesEarlierParameters() {
        Object world = new Object();
        Object screen = new Object();
        RevoScreenEffectsGradient.defer(640, 360, 0.25F, world, screen);
        RevoScreenEffectsGradient.defer(1920, 1080, 0.875F, world, screen);

        RevoScreenEffectsGradient.Gradient gradient =
            RevoScreenEffectsGradient.poll(world, screen, 1920, 1080);

        assertNotNull(gradient);
        assertEquals(0.875F, gradient.progress());
    }

    @Test
    void consumesDeferredGradientOnlyOnce() {
        Object world = new Object();
        Object screen = new Object();
        RevoScreenEffectsGradient.defer(854, 480, 1.0F, world, screen);

        RevoScreenEffectsGradient.poll(world, screen, 854, 480);

        assertNull(RevoScreenEffectsGradient.poll(world, screen, 854, 480));
    }

    @Test
    void discardsGradientWhenRenderContextChanges() {
        Object world = new Object();
        Object screen = new Object();
        RevoScreenEffectsGradient.defer(1280, 720, 0.5F, world, screen);

        assertNull(RevoScreenEffectsGradient.poll(new Object(), screen, 1280, 720));
    }
}
