package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.value.ConstantValue;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class EnumOptionBuilderImpl<E extends Enum<E>> extends StatefulOptionBuilderImpl<EnumOption<E>, E>
        implements EnumOptionBuilder<E> {
    private final Class<E> enumClass;
    private DependentValue<Set<E>> allowedValues;
    private Function<E, ITextComponent> elementNameProvider;

    EnumOptionBuilderImpl(ResourceLocation id, Class<E> enumClass) {
        super(id);
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class must not be null for option '" + id + "'");
        }
        this.enumClass = enumClass;
    }

    @Override public EnumOptionBuilder<E> setName(ITextComponent name) { super.setName(name); return this; }
    @Override public EnumOptionBuilder<E> setTooltip(ITextComponent tooltip) { super.setTooltip(tooltip); return this; }
    @Override public EnumOptionBuilder<E> setTooltip(Function<E, ITextComponent> provider) { super.setTooltip(provider); return this; }
    @Override public EnumOptionBuilder<E> setEnabled(boolean enabled) { super.setEnabled(enabled); return this; }
    @Override public EnumOptionBuilder<E> setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                             ResourceLocation... dependencies) {
        super.setEnabledProvider(provider, dependencies); return this;
    }
    @Override public EnumOptionBuilder<E> setStorageHandler(StorageEventHandler storage) { super.setStorageHandler(storage); return this; }
    @Override public EnumOptionBuilder<E> setImpact(OptionImpact impact) { super.setImpact(impact); return this; }
    @Override public EnumOptionBuilder<E> setFlags(OptionFlag... flags) { super.setFlags(flags); return this; }
    @Override public EnumOptionBuilder<E> setFlags(ResourceLocation... flags) { super.setFlags(flags); return this; }
    @Override public EnumOptionBuilder<E> setDefaultValue(E value) { super.setDefaultValue(value); return this; }
    @Override public EnumOptionBuilder<E> setDefaultProvider(Function<ConfigState, E> provider,
                                                             ResourceLocation... dependencies) {
        super.setDefaultProvider(provider, dependencies); return this;
    }
    @Override public EnumOptionBuilder<E> setControlHiddenWhenDisabled(boolean hidden) { super.setControlHiddenWhenDisabled(hidden); return this; }
    @Override public EnumOptionBuilder<E> setBinding(Consumer<E> save, Supplier<E> load) { super.setBinding(save, load); return this; }
    @Override public EnumOptionBuilder<E> setBinding(OptionBinding<E> binding) { super.setBinding(binding); return this; }
    @Override public EnumOptionBuilder<E> setApplyHook(Consumer<ConfigState> hook) { super.setApplyHook(hook); return this; }

    @Override
    EnumOption<E> build() {
        this.validateStateful();
        DependentValue<Set<E>> resolvedAllowedValues = this.allowedValues != null ? this.allowedValues
                : this.baseOption() == null
                        ? new ConstantValue<>(Set.copyOf(Arrays.asList(this.enumClass.getEnumConstants())))
                        : this.baseOption().getAllowedValues();
        if (this.baseOption() != null && this.baseOption().getEnumClass() != this.enumClass) {
            throw new IllegalArgumentException("Enum overlay has the wrong enum class for option '" + this.id + "'");
        }
        Function<E, ITextComponent> resolvedNameProvider = this.elementNameProvider != null ? this.elementNameProvider
                : this.baseOption() == null ? null : this.baseOption().getElementNameProvider();
        if (resolvedNameProvider == null) {
            throw new IllegalStateException("Enum element name provider must be set for option '" + this.id + "'");
        }
        return new EnumOption<>(this.id, this.collectDependencies(this.defaultValue(), resolvedAllowedValues), this.name(),
                this.enabled(), this.storage(), this.tooltipProvider(), this.impact(), this.flags(),
                this.defaultValue(), this.controlHiddenWhenDisabled(), this.binding(), this.applyHook(),
                this.enumClass, resolvedAllowedValues, resolvedNameProvider);
    }

    @Override
    @SuppressWarnings("unchecked")
    Class<EnumOption<E>> getOptionClass() {
        return (Class<EnumOption<E>>) (Class<?>) EnumOption.class;
    }

    @Override
    public EnumOptionBuilder<E> setAllowedValues(Set<E> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Allowed values must not be empty for option '" + this.id + "'");
        }
        this.allowedValues = new ConstantValue<>(Set.copyOf(new LinkedHashSet<>(values)));
        return this;
    }

    @Override
    public EnumOptionBuilder<E> setAllowedValueProvider(Function<ConfigState, Set<E>> provider,
                                                        ResourceLocation... dependencies) {
        if (provider == null) {
            throw new IllegalArgumentException("Allowed value provider must not be null for option '" + this.id + "'");
        }
        this.allowedValues = new DynamicValue<>(provider, dependencies);
        return this;
    }

    @Override
    public EnumOptionBuilder<E> setAllowedValuesProvider(Function<ConfigState, Set<E>> provider,
                                                         ResourceLocation... dependencies) {
        return this.setAllowedValueProvider(provider, dependencies);
    }

    @Override
    public EnumOptionBuilder<E> setElementNameProvider(Function<E, ITextComponent> provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Element name provider must not be null for option '" + this.id + "'");
        }
        this.elementNameProvider = provider;
        return this;
    }
}
