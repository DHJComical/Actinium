package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Defines pages and hooks owned by one stable configuration namespace.
 */
public interface ModOptionsBuilder {
    /** Replaces the human-readable owner name. */
    ModOptionsBuilder setName(String name);

    /** Replaces the owner version. */
    ModOptionsBuilder setVersion(String version);

    /** Formats the owner version already supplied to the builder. */
    ModOptionsBuilder formatVersion(Function<String, String> formatter);

    /** Sets explicit renderer-independent theme data. */
    ModOptionsBuilder setColorTheme(ColorThemeBuilder theme);

    /** Sets a monochrome icon texture ID which the GUI may tint. */
    ModOptionsBuilder setIcon(ResourceLocation texture);

    /** Sets an icon texture ID which the GUI must render in its original colors. */
    ModOptionsBuilder setNonTintedIcon(ResourceLocation texture);

    /** Adds a complete page definition. */
    ModOptionsBuilder addPage(PageBuilder page);

    /** Replaces an option owned by another configuration namespace. */
    ModOptionsBuilder registerOptionReplacement(ResourceLocation target, OptionBuilder replacement);

    /** Overlays explicitly supplied fields onto an option owned by another namespace. */
    ModOptionsBuilder registerOptionOverlay(ResourceLocation target, OptionBuilder overlay);

    /** Registers a hook for the supplied flag IDs. */
    ModOptionsBuilder registerFlagHook(BiConsumer<Collection<ResourceLocation>, ConfigState> hook,
                                       ResourceLocation... triggers);

    /** Registers a reusable flag hook. */
    ModOptionsBuilder registerFlagHook(FlagHook hook);
}
