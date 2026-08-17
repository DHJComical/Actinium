package me.flashyreese.mods.reeses_sodium_options.client.gui.search;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionLayoutState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultEntry;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchIndex;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchResult;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.Option;
import com.dhj.actinium.gui.rso.compat.Component;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class OptionSearch {
    private final List<SearchableOption> options;
    private final SearchIndex<SearchableOption> searchIndex;

    OptionSearch(List<RsoModOptions> modOptionsList) {
        List<SearchableOption> options = new ArrayList<>();

        for (RsoModOptions modOptions : modOptionsList) {
            for (RsoModOptions.RsoPage pageView : modOptions.pages()) {
                OptionPage page = pageView.unwrap();
                String tabKey = modOptions.configId() + ":" + pageView.name().getUnformattedText();
                for (OptionGroup group : page.getGroups()) {
                    for (Option<?> option : group.getOptions()) {
                        String id = optionIdString(option);
                        if (id == null) {
                            continue;
                        }

                        options.add(new SearchableOption(
                                new ResourceLocation(id),
                                tabKey,
                                option,
                                String.format("%s %s",
                                        Component.fromEmbeddium(option.getName()).getString(),
                                        Component.fromEmbeddium(option.getTooltip()).getString())));
                    }
                }
            }
        }

        this.options = List.copyOf(options);
        this.searchIndex = SearchIndex.builder(SearchableOption::searchableText)
                .addAll(this.options)
                .foldDiacritics(true)
                .maxResults(ReeseSodiumOptionsConfig.config().getSearchResultLimit())
                .minScore(0.3)
                .rerankWithEditDistance(true)
                .rerankLimit(50)
                .rerankWeight(0.1)
                .build();
    }

    private static @Nullable String optionIdString(Option<?> option) {
        org.embeddedt.embeddium.api.options.OptionIdentifier<?> id = option.getId();
        if (id == null || id.getModId().isBlank() || id.getPath().isBlank()) {
            return null;
        }
        return id.getModId() + ":" + id.getPath();
    }

    List<SearchResultEntry> query(String query) {
        return this.searchIndex.newSession(query)
                .results()
                .stream()
                .map(SearchResult::item)
                .map(SearchableOption::toSearchResult)
                .toList();
    }

    List<NavigationTarget> navigationTargets(OptionStateStore optionStateStore, SearchResultOrder order) {
        List<SearchResultEntry> orderedResults = this.orderResults(optionStateStore.searchResults(), order);

        List<NavigationTarget> targets = new ArrayList<>(orderedResults.size());
        for (SearchResultEntry result : orderedResults) {
            NavigationTarget target = this.createNavigationTarget(result, optionStateStore);
            if (target != null) {
                targets.add(target);
            }
        }

        return targets;
    }

    private List<SearchResultEntry> orderResults(List<SearchResultEntry> results, SearchResultOrder order) {
        if (results.isEmpty()) {
            return List.of();
        }

        if (order == SearchResultOrder.RANKED) {
            return results;
        }

        Set<Option<?>> resultOptions = Collections.newSetFromMap(new IdentityHashMap<>());
        results.forEach(result -> resultOptions.add(result.option()));

        List<SearchResultEntry> ordered = new ArrayList<>(results.size());
        for (SearchableOption option : this.options) {
            if (resultOptions.contains(option.option())) {
                ordered.add(option.toSearchResult());
            }
        }

        return ordered;
    }

    private @Nullable NavigationTarget createNavigationTarget(SearchResultEntry result, OptionStateStore optionStateStore) {
        OptionUiState optionUiState = optionStateStore.optionUiState(result.optionId());
        OptionLayoutState optionLayoutState = optionStateStore.optionLayoutState(result.optionId());
        LayoutBounds bounds = optionLayoutState.bounds();
        LayoutBounds parentBounds = optionLayoutState.parentBounds();

        if (!optionUiState.isHighlighted() || parentBounds == null || bounds == null) {
            return null;
        }

        return new NavigationTarget(result.tabKey(), optionUiState, bounds, parentBounds);
    }

    private record SearchableOption(ResourceLocation id, String tabKey, Option<?> option, String searchableText) {
        SearchResultEntry toSearchResult() {
            return new SearchResultEntry(this.tabKey, this.id, this.option);
        }
    }

    record NavigationTarget(String tabKey, OptionUiState optionUiState, LayoutBounds bounds, LayoutBounds parentBounds) {
        int scrollOffset(int viewportHeight) {
            int contentHeight = this.parentBounds.height();
            if (contentHeight <= 0 || contentHeight <= viewportHeight) {
                return 0;
            }

            int maxOffset = contentHeight - viewportHeight;
            int input = this.bounds.y() - this.parentBounds.y();
            int inputOffset = input + this.bounds.height() == contentHeight ? contentHeight : input;

            return (int) ((long) inputOffset * maxOffset / contentHeight);
        }
    }
}
