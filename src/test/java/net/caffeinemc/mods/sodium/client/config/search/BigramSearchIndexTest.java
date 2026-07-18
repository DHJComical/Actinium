package net.caffeinemc.mods.sodium.client.config.search;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigramSearchIndexTest {
    @Test
    void rebuildsResolvedTextWhenLanguageChanges() {
        AtomicReference<String> language = new AtomicReference<>("en_us");
        AtomicInteger registrations = new AtomicInteger();
        BigramSearchIndex index = new BigramSearchIndex(language::get, registrations::incrementAndGet);
        index.register(new TextSource() {
            @Override
            protected String getTextFromSource() {
                return language.get().equals("zh_cn") ? "光照质量" : "Lighting Quality";
            }
        });

        assertEquals(1, index.startQuery().getSearchResults("lighting").size());
        language.set("zh_cn");
        assertEquals(0, index.startQuery().getSearchResults("lighting").size());
        assertEquals(1, index.startQuery().getSearchResults("光照").size());
        assertEquals(1, registrations.get());
    }
}
