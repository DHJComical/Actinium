package com.gtnewhorizons.angelica.glsm.hooks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLSMHooksConsumersGateTest {
    @AfterEach
    void restoreGate() {
        GLSMHooks.consumersActive = true;
    }

    @Test
    void clearingGateSkipsConsumerWork() {
        GLSMHooks.consumersActive = false;
        assertFalse(GLSMHooks.hasActiveConsumers());

        GLSMHooks.consumersActive = true;
        assertTrue(GLSMHooks.hasActiveConsumers());
    }
}
