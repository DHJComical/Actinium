package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

/** Immutable standard configuration page. */
public record OptionPage(ITextComponent name, List<OptionGroup> groups) implements Page {
    public OptionPage {
        if (name == null || name.getUnformattedText().isBlank()) {
            throw new IllegalArgumentException("Option page name must not be blank");
        }
        groups = List.copyOf(groups);
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Option page must contain at least one group");
        }
    }

    @Override
    public void registerTextSources(SearchIndex index, ModOptions modOptions) {
        for (OptionGroup group : this.groups) {
            group.registerTextSources(index, modOptions, this);
        }
    }
}
