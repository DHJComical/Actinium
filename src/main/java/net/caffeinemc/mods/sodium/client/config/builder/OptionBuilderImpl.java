package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.value.ConstantValue;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

abstract class OptionBuilderImpl<O extends Option> implements OptionBuilder {
    final ResourceLocation id;
    private O baseOption;
    private ITextComponent name;
    private DependentValue<Boolean> enabled;

    OptionBuilderImpl(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Option ID must not be null");
        }
        this.id = id;
    }

    abstract O build();

    abstract Class<O> getOptionClass();

    final O buildWithBaseOption(Option baseOption) {
        if (!this.getOptionClass().isInstance(baseOption)) {
            throw new IllegalArgumentException("Overlay for option '" + this.id + "' requires "
                    + this.getOptionClass().getSimpleName() + " but target is "
                    + baseOption.getClass().getSimpleName());
        }
        this.baseOption = this.getOptionClass().cast(baseOption);
        return this.build();
    }

    final void validateBase() {
        if (this.name() == null || this.name().getUnformattedText().isBlank()) {
            throw new IllegalStateException("Name must be set for option '" + this.id + "'");
        }
    }

    final Set<ResourceLocation> collectDependencies(DependentValue<?>... values) {
        Set<ResourceLocation> dependencies = new LinkedHashSet<>(this.enabled().getDependencies());
        for (DependentValue<?> value : values) {
            if (value != null) {
                dependencies.addAll(value.getDependencies());
            }
        }
        return dependencies;
    }

    final ITextComponent name() {
        return this.name != null ? this.name : this.baseOption == null ? null : this.baseOption.getName();
    }

    final DependentValue<Boolean> enabled() {
        if (this.enabled != null) {
            return this.enabled;
        }
        return this.baseOption == null ? new ConstantValue<>(true) : this.baseOption.getEnabled();
    }

    final O baseOption() {
        return this.baseOption;
    }

    @Override
    public OptionBuilder setName(ITextComponent name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null for option '" + this.id + "'");
        }
        this.name = name;
        return this;
    }

    @Override
    public OptionBuilder setEnabled(boolean enabled) {
        this.enabled = new ConstantValue<>(enabled);
        return this;
    }

    @Override
    public OptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                            ResourceLocation... dependencies) {
        if (provider == null) {
            throw new IllegalArgumentException("Enabled provider must not be null for option '" + this.id + "'");
        }
        this.enabled = new DynamicValue<>(provider, dependencies);
        return this;
    }
}
