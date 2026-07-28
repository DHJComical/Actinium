package com.dhj.actinium.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiniumRuntimeOptionsTest {
    private static final String PERF_DEBUG_PROPERTY = "actinium.perfDebug";
    private final String originalPerfDebugProperty = System.getProperty(PERF_DEBUG_PROPERTY);

    @AfterEach
    void restorePerfDebugProperty() {
        if (this.originalPerfDebugProperty == null) {
            System.clearProperty(PERF_DEBUG_PROPERTY);
        } else {
            System.setProperty(PERF_DEBUG_PROPERTY, this.originalPerfDebugProperty);
        }
    }

    @Test
    void usesConfiguredValueWithoutAnExplicitOverride() {
        System.clearProperty(PERF_DEBUG_PROPERTY);

        assertTrue(ActiniumRuntimeOptions.resolvePerfDebugEnabled(true));
        assertFalse(ActiniumRuntimeOptions.resolvePerfDebugEnabled(false));
    }

    @Test
    void explicitOverrideTakesPriorityOverConfiguredValue() {
        System.setProperty(PERF_DEBUG_PROPERTY, "true");
        assertTrue(ActiniumRuntimeOptions.resolvePerfDebugEnabled(false));

        System.setProperty(PERF_DEBUG_PROPERTY, "false");
        assertFalse(ActiniumRuntimeOptions.resolvePerfDebugEnabled(true));
    }
}
