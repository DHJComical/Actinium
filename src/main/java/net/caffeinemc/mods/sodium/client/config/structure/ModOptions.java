package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.caffeinemc.mods.sodium.client.config.search.Searchable;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.List;

/**
 * Immutable configuration contribution owned by one stable namespace.
 */
public record ModOptions(String configId, String name, String version, ColorTheme theme,
                         ResourceLocation icon, boolean iconMonochrome, List<Page> pages,
                         List<OptionOverride> overrides, List<OptionOverlay> overlays,
                         Collection<FlagHook> flagHooks) implements Searchable {
    public ModOptions {
        if (configId == null || configId.isBlank()) {
            throw new IllegalArgumentException("Config ID must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Config name must not be blank for '" + configId + "'");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Config version must not be blank for '" + configId + "'");
        }
        pages = List.copyOf(pages);
        overrides = List.copyOf(overrides);
        overlays = List.copyOf(overlays);
        flagHooks = List.copyOf(flagHooks);
        if (pages.isEmpty() && overrides.isEmpty() && overlays.isEmpty()) {
            throw new IllegalArgumentException("Config '" + configId + "' must contain a page or option change");
        }
    }

    @Override
    public void registerTextSources(SearchIndex index) {
        for (Page page : this.pages) {
            page.registerTextSources(index, this);
        }
    }
}
