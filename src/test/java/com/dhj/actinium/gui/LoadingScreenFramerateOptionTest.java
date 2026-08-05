package com.dhj.actinium.gui;

import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadingScreenFramerateOptionTest {
    @Test
    void defaultsTo60() {
        assertEquals(60, new SodiumGameOptions().performance.loadingScreenFramerateLimit);
    }
}
