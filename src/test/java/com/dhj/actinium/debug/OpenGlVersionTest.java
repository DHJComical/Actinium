package com.dhj.actinium.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenGlVersionTest {
    @Test
    void parsesDesktopAndTranslationLayerVersionStrings() {
        assertEquals(new OpenGlVersion(4, 6), OpenGlVersion.parse("4.6.0 NVIDIA 572.83"));
        assertEquals(
            new OpenGlVersion(3, 3),
            OpenGlVersion.parse("3.3.0 Translation Layer 1.2.3, Backend, GIT@abcdef")
        );
    }

    @Test
    void comparesMajorAndMinorVersions() {
        assertTrue(new OpenGlVersion(4, 0).isAtLeast(3, 3));
        assertTrue(new OpenGlVersion(3, 3).isAtLeast(3, 3));
        assertFalse(new OpenGlVersion(3, 2).isAtLeast(3, 3));
    }

    @Test
    void rejectsMissingOrMalformedVersions() {
        assertThrows(NullPointerException.class, () -> OpenGlVersion.parse(null));
        assertThrows(IllegalArgumentException.class, () -> OpenGlVersion.parse("OpenGL 4.6"));
        assertThrows(IllegalArgumentException.class, () -> OpenGlVersion.parse("4"));
        assertThrows(IllegalArgumentException.class, () -> OpenGlVersion.parse("4.x NVIDIA"));
    }
}
