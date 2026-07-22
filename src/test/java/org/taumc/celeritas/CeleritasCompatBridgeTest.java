package org.taumc.celeritas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CeleritasCompatBridgeTest {
    @Test
    void exposesLegacyModIdentity() {
        assertEquals("celeritas", CeleritasVintage.MODID);
        assertNull(CeleritasVintage.VERSION);
    }
}
