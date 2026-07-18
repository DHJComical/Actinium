package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.util.text.ITextComponent;

/**
 * Builds a named page from explicit groups or loose options.
 */
public interface OptionPageBuilder extends PageBuilder {
    /** Sets the required localized page name. */
    OptionPageBuilder setName(ITextComponent name);

    /** Adds an explicit option group. */
    OptionPageBuilder addOptionGroup(OptionGroupBuilder group);

    /** Adds an option to the page's implicit unnamed group. */
    OptionPageBuilder addOption(OptionBuilder option);
}
