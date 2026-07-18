package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Function;

/**
 * Common builder contract for configuration options.
 */
public interface OptionBuilder {
    /** Sets the localized display name. */
    OptionBuilder setName(ITextComponent name);

    /** Sets the localized searchable description. */
    OptionBuilder setTooltip(ITextComponent tooltip);

    /** Sets a constant enabled state. */
    OptionBuilder setEnabled(boolean enabled);

    /** Sets a dynamic enabled state and its complete dependency list. */
    OptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, ResourceLocation... dependencies);
}
