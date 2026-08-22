package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.dhj.actinium.gui.rso.compat.Component;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultEntry;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PageLayout {
    static final int ROW_HEIGHT = 18;

    private static final int GROUP_PADDING = 4;

    private final List<Row> rows;
    private final int contentHeight;

    private PageLayout(List<Row> rows, int contentHeight) {
        this.rows = List.copyOf(rows);
        this.contentHeight = contentHeight;
    }

    static PageLayout create(OptionPage page, boolean searchActive, List<SearchResultEntry> results, SearchResultOrder resultOrder, boolean hideDisabledOptions, boolean collapsible, Set<ResourceLocation> collapsedGroups) {
        if (searchActive) {
            return createSearchLayout(buildSearchEntries(page, results, resultOrder, hideDisabledOptions));
        }

        return createPageLayout(page, hideDisabledOptions, collapsible, collapsedGroups);
    }

    List<Row> rows() {
        return this.rows;
    }

    int contentHeight() {
        return this.contentHeight;
    }

    private static PageLayout createSearchLayout(List<SearchEntry> searchEntries) {
        List<Row> rows = new ArrayList<>();
        int y = 0;
        OptionGroup lastGroup = null;

        for (SearchEntry entry : searchEntries) {
            OptionGroup group = entry.group();

            if (group != lastGroup) {
                if (lastGroup != null) {
                    y += GROUP_PADDING;
                }

                if (hasLabel(group)) {
                    int labelY = y + GROUP_PADDING;
                    rows.add(new LabelRow(group, Component.fromEmbeddium(group.getName()), labelY, null, false));
                    y = labelY + ROW_HEIGHT;
                }

                lastGroup = group;
            }

            rows.add(new OptionRow(group, entry.option(), y));
            y += ROW_HEIGHT;
        }

        y += GROUP_PADDING;

        return new PageLayout(rows, y);
    }

    private static PageLayout createPageLayout(OptionPage page, boolean hideDisabledOptions, boolean collapsible, Set<ResourceLocation> collapsedGroups) {
        List<Row> rows = new ArrayList<>();
        List<VisibleGroup> groups = visibleGroups(page.getGroups(), hideDisabledOptions);
        int y = 0;

        for (int i = 0; i < groups.size(); i++) {
            VisibleGroup visibleGroup = groups.get(i);
            OptionGroup group = visibleGroup.group();

            ResourceLocation collapseKey = collapsible ? groupCollapseKey(group) : null;
            boolean collapsed = collapseKey != null && collapsedGroups.contains(collapseKey);

            if (hasLabel(group)) {
                int labelY = y + (i == 0 ? 0 : GROUP_PADDING);
                rows.add(new LabelRow(group, Component.fromEmbeddium(group.getName()), labelY, collapseKey, collapsed));
                y = labelY + ROW_HEIGHT;
            }

            if (!collapsed) {
                for (Option<?> option : visibleGroup.options()) {
                    rows.add(new OptionRow(group, option, y));
                    y += ROW_HEIGHT;
                }
            }

            if (i < groups.size() - 1) {
                y += GROUP_PADDING;
            }
        }

        return new PageLayout(rows, y);
    }

    private static List<VisibleGroup> visibleGroups(List<OptionGroup> groups, boolean hideDisabledOptions) {
        if (!hideDisabledOptions) {
            return groups.stream()
                    .map(group -> new VisibleGroup(group, group.getOptions()))
                    .toList();
        }

        List<VisibleGroup> visibleGroups = new ArrayList<>();
        for (OptionGroup group : groups) {
            List<Option<?>> options = group.getOptions()
                    .stream()
                    .filter(Option::isAvailable)
                    .toList();

            if (!options.isEmpty()) {
                visibleGroups.add(new VisibleGroup(group, options));
            }
        }

        return visibleGroups;
    }

    private static @Nullable ResourceLocation groupCollapseKey(OptionGroup group) {
        for (Option<?> option : group.getOptions()) {
            ResourceLocation id = optionId(option);
            if (id != null) {
                return id;
            }
        }

        return null;
    }

    /** Returns the embeddium OptionIdentifier as a 1.12.2 ResourceLocation, or null. */
    static @Nullable ResourceLocation optionId(Option<?> option) {
        org.embeddedt.embeddium.api.options.OptionIdentifier<?> id = option.getId();
        if (id == null || id.getModId().isBlank() || id.getPath().isBlank()) {
            return null;
        }
        return new ResourceLocation(id.getModId(), id.getPath());
    }

    private static boolean hasLabel(OptionGroup group) {
        org.embeddedt.embeddium.impl.gui.framework.TextComponent name = group.getName();
        return name != null && !name.toString().isEmpty();
    }

    private static List<SearchEntry> buildSearchEntries(OptionPage page, List<SearchResultEntry> results, SearchResultOrder resultOrder, boolean hideDisabledOptions) {
        if (results.isEmpty()) {
            return List.of();
        }

        return switch (resultOrder) {
            case PAGE_DISPLAY -> buildSearchEntriesInPageOrder(page, results, hideDisabledOptions);
            case RANKED -> buildSearchEntriesInResultOrder(page, results, hideDisabledOptions);
        };
    }

    private static List<SearchEntry> buildSearchEntriesInPageOrder(OptionPage page, List<SearchResultEntry> results, boolean hideDisabledOptions) {
        Set<Option<?>> resultOptions = Collections.newSetFromMap(new IdentityHashMap<>());
        results.forEach(result -> resultOptions.add(result.option()));

        List<SearchEntry> entries = new ArrayList<>();
        for (OptionGroup group : page.getGroups()) {
            for (Option<?> option : group.getOptions()) {
                if (resultOptions.contains(option) && shouldShowOption(option, hideDisabledOptions)) {
                    entries.add(new SearchEntry(group, option));
                }
            }
        }

        return entries;
    }

    private static List<SearchEntry> buildSearchEntriesInResultOrder(OptionPage page, List<SearchResultEntry> results, boolean hideDisabledOptions) {
        Map<Option<?>, SearchEntry> entriesByOption = new IdentityHashMap<>();
        for (OptionGroup group : page.getGroups()) {
            for (Option<?> option : group.getOptions()) {
                if (shouldShowOption(option, hideDisabledOptions)) {
                    entriesByOption.put(option, new SearchEntry(group, option));
                }
            }
        }

        List<SearchEntry> ordered = new ArrayList<>(results.size());
        for (SearchResultEntry result : results) {
            SearchEntry entry = entriesByOption.get(result.option());
            if (entry != null) {
                ordered.add(entry);
            }
        }

        return ordered;
    }

    private static boolean shouldShowOption(Option<?> option, boolean hideDisabledOptions) {
        return !hideDisabledOptions || option.isAvailable();
    }

    interface Row {
        int y();
    }

    record LabelRow(OptionGroup group, Component text, int y, @Nullable ResourceLocation collapseKey, boolean collapsed) implements Row {
        boolean collapsible() {
            return this.collapseKey != null;
        }
    }

    record OptionRow(OptionGroup group, Option<?> option, int y) implements Row {
    }

    private record SearchEntry(OptionGroup group, Option<?> option) {
    }

    private record VisibleGroup(OptionGroup group, List<Option<?>> options) {
    }
}
