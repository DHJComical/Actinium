package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder contract for options backed by an existing configuration value.
 */
public interface StatefulOptionBuilder<V> extends OptionBuilder {
    @Override
    StatefulOptionBuilder<V> setName(ITextComponent name);

    @Override
    StatefulOptionBuilder<V> setTooltip(ITextComponent tooltip);

    @Override
    StatefulOptionBuilder<V> setEnabled(boolean enabled);

    @Override
    StatefulOptionBuilder<V> setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                 ResourceLocation... dependencies);

    /** Assigns the persistence boundary shared by related options. */
    StatefulOptionBuilder<V> setStorageHandler(StorageEventHandler storage);

    /** Supplies value-sensitive searchable help text. */
    StatefulOptionBuilder<V> setTooltip(Function<V, ITextComponent> tooltipProvider);

    /** Sets the optional performance impact label. */
    StatefulOptionBuilder<V> setImpact(OptionImpact impact);

    /** Sets built-in apply flags. */
    StatefulOptionBuilder<V> setFlags(OptionFlag... flags);

    /** Sets arbitrary apply flag IDs. */
    StatefulOptionBuilder<V> setFlags(ResourceLocation... flags);

    /** Sets a constant reset value. */
    StatefulOptionBuilder<V> setDefaultValue(V value);

    /** Sets a dynamic reset value and its complete dependency list. */
    StatefulOptionBuilder<V> setDefaultProvider(Function<ConfigState, V> provider,
                                                ResourceLocation... dependencies);

    /** Controls whether a disabled option's control remains visible. */
    StatefulOptionBuilder<V> setControlHiddenWhenDisabled(boolean hidden);

    /** Creates a binding from direct read and write functions. */
    StatefulOptionBuilder<V> setBinding(Consumer<V> save, Supplier<V> load);

    /** Assigns a binding with optional validation. */
    StatefulOptionBuilder<V> setBinding(OptionBinding<V> binding);

    /** Registers an option-local hook executed after persistence succeeds. */
    StatefulOptionBuilder<V> setApplyHook(Consumer<ConfigState> hook);
}
