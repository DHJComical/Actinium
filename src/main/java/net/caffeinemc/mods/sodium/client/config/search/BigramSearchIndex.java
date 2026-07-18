package net.caffeinemc.mods.sodium.client.config.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Unicode-aware fuzzy index based on weighted leading and inner character bigrams.
 */
public final class BigramSearchIndex extends SourceStoringIndex {
    private static final Pattern NON_WORD = Pattern.compile("[\\W_]+", Pattern.UNICODE_CHARACTER_CLASS);
    private final Map<String, List<SourceBigramCount>> bigrams = new HashMap<>();

    private record SourceBigramCount(TextSource source, int count, float prevalenceInverse) {
    }

    public BigramSearchIndex(Supplier<?> languageProvider, Runnable registerCallback) {
        super(languageProvider, registerCallback);
    }

    @Override
    protected void rebuildIndex() {
        this.bigrams.clear();
        Map<String, List<RawCount>> raw = new HashMap<>();
        for (TextSource source : this.sources) {
            String text = source.getText();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Text source returned blank text: " + source);
            }
            for (Map.Entry<String, Integer> entry : countBigrams(conditionText(text)).entrySet()) {
                raw.computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(new RawCount(source, entry.getValue()));
            }
        }
        for (Map.Entry<String, List<RawCount>> entry : raw.entrySet()) {
            float prevalenceInverse = (float) this.sources.size() / entry.getValue().size();
            List<SourceBigramCount> counts = new ArrayList<>();
            for (RawCount count : entry.getValue()) {
                counts.add(new SourceBigramCount(count.source(), count.count(), prevalenceInverse));
            }
            this.bigrams.put(entry.getKey(), counts);
        }
    }

    private record RawCount(TextSource source, int count) {
    }

    @Override
    protected SearchQuerySession createQuery() {
        return query -> this.search(query);
    }

    private List<TextSource> search(String rawQuery) {
        String query = conditionText(rawQuery).trim();
        if (query.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> queryBigrams = countBigrams(query);
        Map<TextSource, Float> scores = new LinkedHashMap<>();
        float maximum = 0.0F;
        for (Map.Entry<String, Integer> entry : queryBigrams.entrySet()) {
            List<SourceBigramCount> sourcesForBigram = this.bigrams.get(entry.getKey());
            if (sourcesForBigram == null) {
                continue;
            }
            for (SourceBigramCount count : sourcesForBigram) {
                float score = ((float) entry.getValue() / Math.max(1, query.length()))
                        * ((float) Math.log(count.count()) + 1.0F) * count.prevalenceInverse();
                String sourceText = conditionText(count.source().getText());
                if (sourceText.startsWith(query)) {
                    score *= 3.0F;
                } else if (sourceText.contains(query)) {
                    score *= 2.0F;
                }
                float total = scores.merge(count.source(), score, Float::sum);
                maximum = Math.max(maximum, total);
            }
        }
        float cutoff = maximum * 0.2F;
        List<TextSource> results = new ArrayList<>();
        for (Map.Entry<TextSource, Float> entry : scores.entrySet()) {
            if (entry.getValue() >= cutoff) {
                entry.getKey().setScore(entry.getValue());
                results.add(entry.getKey());
            }
        }
        results.sort(Comparator.comparing(TextSource::getScore).reversed());
        return List.copyOf(results.subList(0, Math.min(10, results.size())));
    }

    private static String conditionText(String text) {
        return NON_WORD.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    private static Map<String, Integer> countBigrams(String text) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (text.isEmpty()) {
            return counts;
        }
        add(counts, " " + text.charAt(0));
        for (int index = 0; index < text.length() - 1; index++) {
            add(counts, text.substring(index, index + 2));
        }
        add(counts, text.charAt(text.length() - 1) + " ");
        return counts;
    }

    private static void add(Map<String, Integer> counts, String bigram) {
        counts.merge(bigram, 1, Integer::sum);
    }
}
