package com.gtnewhorizons.angelica.glsm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuCommandDiagnosticsTest {
    @Test
    void trustsOnlyTheEnabledNonBypassedOwningContextCache() {
        Object mainContext = new Object();

        assertTrue(GpuCommandDiagnostics.isCacheTrusted(true, false, mainContext, mainContext));
        assertFalse(GpuCommandDiagnostics.isCacheTrusted(false, false, mainContext, mainContext));
        assertFalse(GpuCommandDiagnostics.isCacheTrusted(true, true, mainContext, mainContext));
        assertFalse(GpuCommandDiagnostics.isCacheTrusted(true, false, new Object(), mainContext));
        assertFalse(GpuCommandDiagnostics.isCacheTrusted(true, false, null, mainContext));
        assertFalse(GpuCommandDiagnostics.isCacheTrusted(true, false, mainContext, null));
    }

    @Test
    void replacesUntrustedCachedMetadataWithTheUnknownSentinel() {
        assertEquals(73, GpuCommandDiagnostics.trustedValue(true, 73));
        assertEquals(GpuCommandDiagnostics.UNKNOWN, GpuCommandDiagnostics.trustedValue(false, 73));
    }

    @Test
    void writesUnknownFramebufferOperandsForASharedContext() {
        Object mainContext = new Object();
        Object sharedContext = new Object();
        boolean cacheTrusted = GpuCommandDiagnostics.isCacheTrusted(
            true,
            false,
            sharedContext,
            mainContext
        );

        assertEquals(GpuCommandDiagnostics.UNKNOWN, GpuCommandDiagnostics.trustedValue(cacheTrusted, 17));
        assertEquals(GpuCommandDiagnostics.UNKNOWN, GpuCommandDiagnostics.trustedValue(cacheTrusted, 23));
    }
}
