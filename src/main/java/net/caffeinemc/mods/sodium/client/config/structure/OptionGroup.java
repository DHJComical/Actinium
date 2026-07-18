package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

/** Immutable visual group of options. */
public record OptionGroup(ITextComponent name, List<Option> options) {
    public OptionGroup {
        options = List.copyOf(options);
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Option group must contain at least one option");
        }
    }

    /** Registers every option's page, name, and description search sources. */
    public void registerTextSources(SearchIndex index, ModOptions modOptions, OptionPage page) {
        for (Option option : this.options) {
            option.registerTextSources(index, modOptions, page, this);
        }
    }
}
