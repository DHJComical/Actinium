package org.taumc.celeritas.compat;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.OptionPageConstructionEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyOptionPageContractTest {
    @Test
    void constructionEventAppendsGroupsAndPageCachesFlattenedOptions() {
        OptionIdentifier<Void> pageId = OptionIdentifier.create("compat_test", "legacy_page_contract");
        TextComponent pageName = TextComponent.literal("Legacy page contract");
        Option<Boolean> baseOption = LegacyOptionFixtures.option("base_option");
        Option<Boolean> extraOption = LegacyOptionFixtures.option("extra_option");
        OptionGroup baseGroup = OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("compat_test", "base_group"))
                .add(baseOption)
                .build();
        OptionGroup extraGroup = OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("compat_test", "extra_group"))
                .add(extraOption)
                .build();
        AtomicReference<OptionPageConstructionEvent> observed = new AtomicReference<>();
        EventHandlerRegistrar.Handler<OptionPageConstructionEvent> handler = event -> {
            if (event.getId().matches(pageId)) {
                observed.set(event);
                event.addGroup(extraGroup);
            }
        };
        OptionPageConstructionEvent.BUS.addListener(handler);
        try {
            OptionPage page = new OptionPage(pageId, pageName, List.of(baseGroup));
            assertEquals(List.of(baseGroup, extraGroup), page.getGroups());
            assertEquals(List.of(baseOption, extraOption), page.getOptions());
            assertSame(pageId, observed.get().getId());
            assertSame(pageName, observed.get().getTranslationKey());
            assertEquals(List.of(extraGroup), observed.get().getAdditionalGroups());
            assertThrows(UnsupportedOperationException.class,
                    () -> observed.get().getAdditionalGroups().add(baseGroup));
        } finally {
            OptionPageConstructionEvent.BUS.removeListener(handler);
        }
    }
}
