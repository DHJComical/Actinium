package net.caffeinemc.mods.sodium.client.config.search;

import java.util.List;

/** Immutable-language search session created after index invalidation checks. */
public interface SearchQuerySession {
    /** Returns at most ten text sources ordered by descending relevance. */
    List<? extends TextSource> getSearchResults(String query);
}
