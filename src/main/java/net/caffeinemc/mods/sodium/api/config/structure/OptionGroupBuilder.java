package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.util.text.ITextComponent;

/**
 * Groups related options for layout while preserving registration order.
 */
public interface OptionGroupBuilder {
    /** Sets an optional localized group title. */
    OptionGroupBuilder setName(ITextComponent name);

    /** Adds one complete option definition. */
    OptionGroupBuilder addOption(OptionBuilder option);
}
