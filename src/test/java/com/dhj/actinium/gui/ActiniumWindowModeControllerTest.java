package com.dhj.actinium.gui;

import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiniumWindowModeControllerTest {
    @Test
    void resolvesStoredFullscreenModes() {
        SodiumGameOptions options = new SodiumGameOptions();

        options.window.fullscreenMode = FullscreenMode.BORDERLESS.name();
        assertEquals(FullscreenMode.BORDERLESS, ActiniumWindowModeController.resolveConfiguredMode(options));

        options.window.fullscreenMode = FullscreenMode.FULLSCREEN.name();
        assertEquals(FullscreenMode.FULLSCREEN, ActiniumWindowModeController.resolveConfiguredMode(options));
    }

    @Test
    void fallsBackToExclusiveFullscreenForMissingOrInvalidMode() {
        SodiumGameOptions options = new SodiumGameOptions();

        assertEquals(FullscreenMode.FULLSCREEN, ActiniumWindowModeController.resolveConfiguredMode(options));

        options.window.fullscreenMode = "invalid-mode";
        assertEquals(FullscreenMode.FULLSCREEN, ActiniumWindowModeController.resolveConfiguredMode(options));
    }
}
