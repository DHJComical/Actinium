package me.flashyreese.mods.reeses_sodium_options.client.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchIndexSessionTest {

    private static SearchIndex<String> indexOf(String... texts) {
        SearchIndex.Builder<String> builder = SearchIndex.builder(text -> text);
        for (String text : texts) {
            builder.add(text);
        }
        return builder
                .foldDiacritics(true)
                .maxResults(15)
                .minScore(0.3)
                .rerankWithEditDistance(true)
                .build();
    }

    @Test
    void findsExactTokenMatches() {
        SearchIndex<String> index = indexOf("Graphics Quality", "Particles", "Cloud Render Distance");
        List<SearchResult<String>> results = index.newSession("quality").results();

        assertFalse(results.isEmpty());
        assertEquals("Graphics Quality", results.get(0).item());
    }

    @Test
    void emptyQueryReturnsNoResults() {
        SearchIndex<String> index = indexOf("Graphics Quality", "Particles");
        assertTrue(index.newSession("").results().isEmpty());
        assertTrue(index.newSession("   ").results().isEmpty());
    }

    @Test
    void matchingIsCaseInsensitive() {
        SearchIndex<String> index = indexOf("Graphics Quality");
        assertFalse(index.newSession("GRAPHICS").results().isEmpty());
    }

    @Test
    void respectsMaxResults() {
        SearchIndex.Builder<String> builder = SearchIndex.builder((String text) -> text);
        SearchIndex<String> index = builder
                .add("One")
                .add("Two")
                .add("Three")
                .maxResults(1)
                .build();
        assertEquals(1, index.newSession("t").results().size());
    }

    @Test
    void editDistanceRerankFindsCloseMatches() {
        SearchIndex<String> index = indexOf("Fog Start", "Fog End", "Brightness");
        List<SearchResult<String>> results = index.newSession("fog").results();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result -> result.item().equals("Fog Start")));
        assertTrue(results.stream().anyMatch(result -> result.item().equals("Fog End")));
    }
}
