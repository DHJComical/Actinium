package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.value.ConstantValue;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class IntegerOptionBuilderImpl extends StatefulOptionBuilderImpl<IntegerOption, Integer>
        implements IntegerOptionBuilder {
    private DependentValue<? extends SteppedValidator> validator;
    private ControlValueFormatter formatter;

    IntegerOptionBuilderImpl(ResourceLocation id) {
        super(id);
    }

    @Override
    public IntegerOptionBuilder setName(ITextComponent name) {
        super.setName(name);
        return this;
    }

    @Override
    public IntegerOptionBuilder setTooltip(ITextComponent tooltip) {
        super.setTooltip(tooltip);
        return this;
    }

    @Override
    public IntegerOptionBuilder setTooltip(Function<Integer, ITextComponent> tooltipProvider) {
        super.setTooltip(tooltipProvider);
        return this;
    }

    @Override
    public IntegerOptionBuilder setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        return this;
    }

    @Override
    public IntegerOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                   ResourceLocation... dependencies) {
        super.setEnabledProvider(provider, dependencies);
        return this;
    }

    @Override
    public IntegerOptionBuilder setStorageHandler(StorageEventHandler storage) {
        super.setStorageHandler(storage);
        return this;
    }

    @Override
    public IntegerOptionBuilder setImpact(OptionImpact impact) {
        super.setImpact(impact);
        return this;
    }

    @Override
    public IntegerOptionBuilder setFlags(OptionFlag... flags) {
        super.setFlags(flags);
        return this;
    }

    @Override
    public IntegerOptionBuilder setFlags(ResourceLocation... flags) {
        super.setFlags(flags);
        return this;
    }

    @Override
    public IntegerOptionBuilder setDefaultValue(Integer value) {
        super.setDefaultValue(value);
        return this;
    }

    @Override
    public IntegerOptionBuilder setDefaultProvider(Function<ConfigState, Integer> provider,
                                                   ResourceLocation... dependencies) {
        super.setDefaultProvider(provider, dependencies);
        return this;
    }

    @Override
    public IntegerOptionBuilder setControlHiddenWhenDisabled(boolean hidden) {
        super.setControlHiddenWhenDisabled(hidden);
        return this;
    }

    @Override
    public IntegerOptionBuilder setBinding(Consumer<Integer> save, Supplier<Integer> load) {
        super.setBinding(save, load);
        return this;
    }

    @Override
    public IntegerOptionBuilder setBinding(OptionBinding<Integer> binding) {
        super.setBinding(binding);
        return this;
    }

    @Override
    public IntegerOptionBuilder setApplyHook(Consumer<ConfigState> hook) {
        super.setApplyHook(hook);
        return this;
    }

    @Override
    IntegerOption build() {
        this.validateStateful();
        DependentValue<? extends SteppedValidator> resolvedValidator = this.validator != null ? this.validator
                : this.baseOption() == null ? null : this.baseOption().getValidatorProvider();
        ControlValueFormatter resolvedFormatter = this.formatter != null ? this.formatter
                : this.baseOption() == null ? null : this.baseOption().getValueFormatter();
        if (resolvedValidator == null) {
            throw new IllegalStateException("Validator must be set for integer option '" + this.id + "'");
        }
        if (resolvedFormatter == null) {
            throw new IllegalStateException("Value formatter must be set for integer option '" + this.id + "'");
        }
        return new IntegerOption(this.id, this.collectDependencies(this.defaultValue(), resolvedValidator), this.name(),
                this.enabled(), this.storage(), this.tooltipProvider(), this.impact(), this.flags(),
                this.defaultValue(), this.controlHiddenWhenDisabled(), this.binding(), this.applyHook(),
                resolvedValidator, resolvedFormatter);
    }

    @Override
    Class<IntegerOption> getOptionClass() {
        return IntegerOption.class;
    }

    @Override
    public IntegerOptionBuilder setRange(int min, int max, int step) {
        return this.setRange(new Range(min, max, step));
    }

    @Override
    public IntegerOptionBuilder setRange(Range range) {
        return this.setValidator(range);
    }

    @Override
    public IntegerOptionBuilder setValidator(SteppedValidator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("Validator must not be null for option '" + this.id + "'");
        }
        this.validator = new ConstantValue<>(validator);
        return this;
    }

    @Override
    public IntegerOptionBuilder setValidatorProvider(
            Function<ConfigState, ? extends SteppedValidator> provider, ResourceLocation... dependencies) {
        if (provider == null) {
            throw new IllegalArgumentException("Validator provider must not be null for option '" + this.id + "'");
        }
        this.validator = new DynamicValue<>(provider, dependencies);
        return this;
    }

    @Override
    public IntegerOptionBuilder setRangeProvider(Function<ConfigState, ? extends SteppedValidator> provider,
                                                  ResourceLocation... dependencies) {
        return this.setValidatorProvider(provider, dependencies);
    }

    @Override
    public IntegerOptionBuilder setValueFormatter(ControlValueFormatter formatter) {
        this.formatter = formatter;
        return this;
    }
}
