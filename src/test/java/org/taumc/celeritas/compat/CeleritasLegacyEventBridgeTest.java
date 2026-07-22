package org.taumc.celeritas.compat;

import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.options.structure.StandardOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CeleritasLegacyEventBridgeTest {
    @Test
    void mapsLegacyFullscreenResolutionIntoCurrentNamespaceAndBack() {
        var model = LegacyOptionMapper.describeIdentifier(StandardOptions.Option.FULLSCREEN_RESOLUTION);
        var current = CurrentOptionMapper.createIdentifier(model);
        assertEquals("actinium", current.getModId());
        assertEquals("fullscreen_resolution", current.getPath());

        var legacy = LegacyOptionMapper.createIdentifier(CurrentOptionMapper.describeIdentifier(current));
        assertSame(StandardOptions.Option.FULLSCREEN_RESOLUTION, legacy);
        assertEquals("minecraft", legacy.getModId());
    }

    @Test
    void leavesUnaliasedIdentifiersCanonical() {
        var current = CurrentOptionMapper.createIdentifier(
                LegacyOptionMapper.describeIdentifier(StandardOptions.Group.WINDOW));
        assertEquals("minecraft", current.getModId());
        assertEquals("window", current.getPath());
        assertSame(StandardOptions.Group.WINDOW,
                LegacyOptionMapper.createIdentifier(CurrentOptionMapper.describeIdentifier(current)));
    }
}
