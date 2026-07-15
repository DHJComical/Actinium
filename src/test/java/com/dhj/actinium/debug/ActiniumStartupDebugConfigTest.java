package com.dhj.actinium.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiniumStartupDebugConfigTest {
    @Test
    void resolvesLwjglDebugOnlyFromAnExplicitTrueProperty() {
        assertTrue(ActiniumStartupDebugConfig.resolveLwjglDebug("true"));
        assertTrue(ActiniumStartupDebugConfig.resolveLwjglDebug("TRUE"));
        assertFalse(ActiniumStartupDebugConfig.resolveLwjglDebug("false"));
        assertFalse(ActiniumStartupDebugConfig.resolveLwjglDebug(null));
    }
}
