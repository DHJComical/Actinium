package com.dhj.actinium.compat.sodium;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.StandardOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the RSO tab grouping: the settings pages (general/quality/
 * performance/advanced/debug) must live under the host "actinium" namespace
 * so they do not end up in the celeritas compatibility-bridge tab.
 */
class EmbeddiumStandardOptionsPagesTest {
    @Test
    void settingsPagesAreHostOwned() {
        assertEquals("actinium", StandardOptions.Pages.GENERAL.getModId());
        assertEquals("actinium", StandardOptions.Pages.QUALITY.getModId());
        assertEquals("actinium", StandardOptions.Pages.PERFORMANCE.getModId());
        assertEquals("actinium", StandardOptions.Pages.ADVANCED.getModId());
        assertEquals("actinium", StandardOptions.Pages.DEBUG.getModId());
    }

    @Test
    void optionIdentifiersRemainCeleritasScoped() {
        assertEquals("celeritas", StandardOptions.Option.CHUNK_UPDATE_THREADS.getModId());
        assertEquals("celeritas", StandardOptions.Option.BLOCK_FACE_CULLING.getModId());
        assertEquals(OptionIdentifier.create("celeritas", "chunk_update_threads"),
                StandardOptions.Option.CHUNK_UPDATE_THREADS);
    }
}
