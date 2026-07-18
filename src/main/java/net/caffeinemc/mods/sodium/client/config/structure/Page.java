package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

/**
 * Immutable page model consumed by platform-specific GUI adapters.
 */
public interface Page {
    /** Returns the localized page name. */
    ITextComponent name();

    /** Returns immutable option groups in display order. */
    List<OptionGroup> groups();

    /** Registers language-sensitive search sources for this page. */
    void registerTextSources(SearchIndex index, ModOptions modOptions);
}
