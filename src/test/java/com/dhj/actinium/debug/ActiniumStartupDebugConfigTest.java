package com.dhj.actinium.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiniumStartupDebugConfigTest {
    @Test
    void resolvesLwjglDebugFromPropertyOverrideOrConfiguredFallback() {
        assertTrue(ActiniumStartupDebugConfig.resolveLwjglDebug("true", false));
        assertTrue(ActiniumStartupDebugConfig.resolveLwjglDebug("TRUE", false));
        assertFalse(ActiniumStartupDebugConfig.resolveLwjglDebug("false", true));
        assertTrue(ActiniumStartupDebugConfig.resolveLwjglDebug(null, true));
        assertFalse(ActiniumStartupDebugConfig.resolveLwjglDebug(null, false));
    }
}
