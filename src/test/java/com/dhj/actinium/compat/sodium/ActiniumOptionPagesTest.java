package com.dhj.actinium.compat.sodium;

import dhj.embeddedt.embeddium.api.options.OptionIdentifier;
import dhj.embeddedt.embeddium.api.options.structure.OptionPage;
import dhj.embeddedt.embeddium.api.options.structure.StandardOptions;
import dhj.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import dhj.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the DEBUG tab switch: with {@code enable_debug_tab} off the DEBUG page
 * must be dropped from the candidate pages, and turning the switch on must keep
 * every page in its original order.
 */
class ActiniumOptionPagesTest {
    @Test
    void debugPageIsDroppedAndOffByDefault() {
        SodiumGameOptions options = new SodiumGameOptions();

        List<OptionIdentifier<Void>> ids = pageIds(ActiniumOptionPages.retainEnabledPages(candidatePages(), options));

        assertFalse(options.enableDebugTab, "enable_debug_tab must default to false");
        assertFalse(ids.contains(StandardOptions.Pages.DEBUG));
        assertEquals(List.of(
                StandardOptions.Pages.GENERAL,
                StandardOptions.Pages.QUALITY,
                StandardOptions.Pages.PERFORMANCE,
                StandardOptions.Pages.ADVANCED), ids);
    }

    @Test
    void debugPageIsKeptWhileEnabled() {
        SodiumGameOptions options = new SodiumGameOptions();
        options.enableDebugTab = true;

        List<OptionIdentifier<Void>> ids = pageIds(ActiniumOptionPages.retainEnabledPages(candidatePages(), options));

        assertTrue(ids.contains(StandardOptions.Pages.DEBUG));
        assertEquals(List.of(
                StandardOptions.Pages.GENERAL,
                StandardOptions.Pages.QUALITY,
                StandardOptions.Pages.PERFORMANCE,
                StandardOptions.Pages.ADVANCED,
                StandardOptions.Pages.DEBUG), ids);
    }

    private static List<OptionPage> candidatePages() {
        return List.of(
                page(StandardOptions.Pages.GENERAL),
                page(StandardOptions.Pages.QUALITY),
                page(StandardOptions.Pages.PERFORMANCE),
                page(StandardOptions.Pages.ADVANCED),
                page(StandardOptions.Pages.DEBUG));
    }

    private static OptionPage page(OptionIdentifier<Void> id) {
        return new OptionPage(id, TextComponent.literal(id.toString()), List.of());
    }

    private static List<OptionIdentifier<Void>> pageIds(List<OptionPage> pages) {
        return pages.stream().map(OptionPage::getId).toList();
    }
}
