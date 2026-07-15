package com.gtnewhorizons.angelica.glsm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLDebugTest {
    @Test
    void acceptsBothInstalledCallbackResults() {
        assertDoesNotThrow(() -> GLDebug.requireInstalledCallback(1, "test backend"));
        assertDoesNotThrow(() -> GLDebug.requireInstalledCallback(2, "test backend"));
    }

    @Test
    void rejectsMissingOrUnknownCallbackResults() {
        IllegalStateException missing = assertThrows(
            IllegalStateException.class,
            () -> GLDebug.requireInstalledCallback(0, "test backend")
        );
        assertTrue(missing.getMessage().contains("test backend"));

        assertThrows(
            IllegalStateException.class,
            () -> GLDebug.requireInstalledCallback(3, "test backend")
        );
    }
}
