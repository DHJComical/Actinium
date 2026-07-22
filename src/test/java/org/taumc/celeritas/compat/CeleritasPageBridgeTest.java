package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.OptionPageConstructionEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CeleritasPageBridgeTest {
    @Test
    void currentPageConstructionAcceptsLegacyAdditionalGroupOnce() {
        CeleritasLegacyEventBridge.install();
        AtomicInteger calls = new AtomicInteger();
        EventHandlerRegistrar.Handler<OptionPageConstructionEvent> handler = event -> {
            if (event.getId().getPath().equals("current_page")) {
                calls.incrementAndGet();
                event.addGroup(LegacyOptionFixtures.group("legacy_added_group"));
            }
        };
        OptionPageConstructionEvent.BUS.addListener(handler);
        try {
            OptionPage page = currentPage("current_page");
            assertEquals(1, calls.get());
            assertEquals(2, page.getGroups().size());
            assertEquals("legacy_added_group", page.getGroups().get(1).getId().getPath());
        } finally {
            OptionPageConstructionEvent.BUS.removeListener(handler);
        }
    }

    @Test
    void convertingLegacyPageDoesNotDispatchLegacyPageEventTwice() {
        CeleritasLegacyEventBridge.install();
        AtomicInteger calls = new AtomicInteger();
        EventHandlerRegistrar.Handler<OptionPageConstructionEvent> handler = event -> {
            if (event.getId().getPath().equals("round_trip_page")) {
                calls.incrementAndGet();
            }
        };
        OptionPageConstructionEvent.BUS.addListener(handler);
        try {
            var legacyPage = LegacyOptionFixtures.page("round_trip_page");
            OptionPage currentPage = CurrentOptionMapper.createPage(LegacyOptionMapper.describePage(legacyPage));
            assertEquals(1, calls.get());
            assertEquals("round_trip_page", currentPage.getId().getPath());
        } finally {
            OptionPageConstructionEvent.BUS.removeListener(handler);
        }
    }

    @Test
    void legacyGuiEventSeesExistingPagesAndAddsWithoutDuplicatingOriginal() {
        OptionPage builtIn = currentPage("existing_page");
        CeleritasLegacyEventBridge.install();
        AtomicInteger constructionCalls = new AtomicInteger();
        EventHandlerRegistrar.Handler<OptionPageConstructionEvent> pageHandler = event -> {
            if (event.getId().getPath().equals("gui_added_page")) {
                constructionCalls.incrementAndGet();
            }
        };
        EventHandlerRegistrar.Handler<OptionGUIConstructionEvent> guiHandler = event -> {
            assertEquals(1, event.getPages().size());
            assertEquals("existing_page", event.getPages().getFirst().getId().getPath());
            event.addPage(LegacyOptionFixtures.page("gui_added_page"));
        };
        OptionPageConstructionEvent.BUS.addListener(pageHandler);
        OptionGUIConstructionEvent.BUS.addListener(guiHandler);
        try {
            List<OptionPage> pages = new ArrayList<>(List.of(builtIn));
            CeleritasLegacyEventBridge.INSTANCE.appendPages(pages);
            assertEquals(1, constructionCalls.get());
            assertEquals(2, pages.size());
            assertSame(builtIn, pages.getFirst());
            assertEquals("gui_added_page", pages.get(1).getId().getPath());
        } finally {
            OptionGUIConstructionEvent.BUS.removeListener(guiHandler);
            OptionPageConstructionEvent.BUS.removeListener(pageHandler);
        }
    }

    private static OptionPage currentPage(String path) {
        OptionGroup group = OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("compat_test", path + "_group"))
                .add(CurrentOptionFixtures.tickBox())
                .build();
        return new OptionPage(OptionIdentifier.create("compat_test", path), TextComponent.literal(path),
                List.of(group));
    }
}
