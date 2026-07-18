package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Type-refined builder for stepped integer options. */
public interface IntegerOptionBuilder extends StatefulOptionBuilder<Integer> {
    @Override
    IntegerOptionBuilder setName(ITextComponent name);

    @Override
    IntegerOptionBuilder setTooltip(ITextComponent tooltip);

    @Override
    IntegerOptionBuilder setTooltip(Function<Integer, ITextComponent> tooltipProvider);

    @Override
    IntegerOptionBuilder setEnabled(boolean enabled);

    @Override
    IntegerOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                            ResourceLocation... dependencies);

    @Override
    IntegerOptionBuilder setStorageHandler(StorageEventHandler storage);

    @Override
    IntegerOptionBuilder setImpact(OptionImpact impact);

    @Override
    IntegerOptionBuilder setFlags(OptionFlag... flags);

    @Override
    IntegerOptionBuilder setFlags(ResourceLocation... flags);

    @Override
    IntegerOptionBuilder setDefaultValue(Integer value);

    @Override
    IntegerOptionBuilder setDefaultProvider(Function<ConfigState, Integer> provider,
                                            ResourceLocation... dependencies);

    @Override
    IntegerOptionBuilder setControlHiddenWhenDisabled(boolean hidden);

    @Override
    IntegerOptionBuilder setBinding(Consumer<Integer> save, Supplier<Integer> load);

    @Override
    IntegerOptionBuilder setBinding(OptionBinding<Integer> binding);

    @Override
    IntegerOptionBuilder setApplyHook(Consumer<ConfigState> hook);

    /** Sets a constant stepped range. */
    IntegerOptionBuilder setRange(int min, int max, int step);

    /** Sets a constant stepped range. */
    IntegerOptionBuilder setRange(Range range);

    /** Sets a constant stepped validator. */
    IntegerOptionBuilder setValidator(SteppedValidator validator);

    /** Sets a dynamic stepped validator and its complete dependency list. */
    IntegerOptionBuilder setValidatorProvider(Function<ConfigState, ? extends SteppedValidator> provider,
                                              ResourceLocation... dependencies);

    /** Compatibility alias for the dynamic stepped range provider. */
    default IntegerOptionBuilder setRangeProvider(Function<ConfigState, ? extends SteppedValidator> provider,
                                                  ResourceLocation... dependencies) {
        return setValidatorProvider(provider, dependencies);
    }

    /** Sets the formatter consumed by numeric controls. */
    IntegerOptionBuilder setValueFormatter(ControlValueFormatter formatter);
}
