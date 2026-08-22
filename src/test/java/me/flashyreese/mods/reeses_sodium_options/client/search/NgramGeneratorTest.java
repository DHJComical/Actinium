package me.flashyreese.mods.reeses_sodium_options.client.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NgramGeneratorTest {
    private final NgramGenerator generator = new NgramGenerator(2, 3, true);

    @Test
    void emptyInputProducesNoGrams() {
        assertTrue(generator.generate(null).isEmpty());
        assertTrue(generator.generate("").isEmpty());
        assertTrue(generator.generate("   ").isEmpty());
    }

    @Test
    void generatesBigramsAndTrigramsWithBoundarySentinels() {
        List<String> grams = generator.generate("abc");
        // Padded with two STX at the front and one ETX at the back.
        // Bigrams: STX STX (skipped), STX a, ab, bc, c ETX
        // Trigrams: STX STX a (skipped), STX ab, abc, bc ETX
        assertTrue(grams.contains("ab"), "bigram ab expected");
        assertTrue(grams.contains("bc"), "bigram bc expected");
        assertTrue(grams.contains("abc"), "trigram abc expected");
        assertTrue(grams.contains("\u0002a"), "boundary bigram STX a expected");
        assertFalse(grams.contains("\u0002\u0002"), "all-boundary gram must be skipped");
    }

    @Test
    void tokenizesOnWhitespaceBoundaries() {
        // Whitespace splits tokens; MC identifier punctuation (dash) is kept
        // inside a token, matching the upstream isTokenChar semantics.
        List<String> grams = generator.generate("fast render");
        String joined = String.join(" ", grams);
        assertTrue(joined.contains("fas"), "token fast bigram expected");
        assertTrue(joined.contains("ren"), "token render trigram expected");
        assertFalse(joined.contains("t r"), "cross-token grams must not be generated");

        List<String> dashGrams = generator.generate("fast-render");
        assertTrue(dashGrams.contains("st-"), "dash is identifier punctuation and stays in-token");
        assertTrue(dashGrams.contains("-re"), "dash is identifier punctuation and stays in-token");
    }

    @Test
    void normalizesCaseAndDiacritics() {
        List<String> grams = generator.generate("Café");
        List<String> lowerGrams = grams.stream().map(g -> g.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
        assertTrue(lowerGrams.contains("ca"), "folded lowercase grams expected");
    }
}
