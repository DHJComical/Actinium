package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.StatefulOptionBuilder;
import net.caffeinemc.mods.sodium.client.config.AnonymousOptionBinding;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
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

abstract class StatefulOptionBuilderImpl<O extends StatefulOption<V>, V> extends OptionBuilderImpl<O>
        implements StatefulOptionBuilder<V> {
    private StorageEventHandler storage;
    private Function<V, ITextComponent> tooltipProvider;
    private OptionImpact impact;
    private Set<ResourceLocation> flags;
    private DependentValue<V> defaultValue;
    private Boolean controlHiddenWhenDisabled;
    private OptionBinding<V> binding;
    private Consumer<ConfigState> applyHook;

    StatefulOptionBuilderImpl(ResourceLocation id) {
        super(id);
    }

    final void validateStateful() {
        this.validateBase();
        if (this.storage() == null) {
            throw new IllegalStateException("Storage handler must be set for option '" + this.id + "'");
        }
        if (this.tooltipProvider() == null) {
            throw new IllegalStateException("Tooltip must be set for option '" + this.id + "'");
        }
        if (this.defaultValue() == null) {
            throw new IllegalStateException("Default value must be set for option '" + this.id + "'");
        }
        if (this.binding() == null) {
            throw new IllegalStateException("Binding must be set for option '" + this.id + "'");
        }
    }

    final StorageEventHandler storage() {
        return this.storage != null ? this.storage
                : this.baseOption() == null ? null : this.baseOption().getStorage();
    }

    final Function<V, ITextComponent> tooltipProvider() {
        return this.tooltipProvider != null ? this.tooltipProvider
                : this.baseOption() == null ? null : this.baseOption().getTooltipProvider();
    }

    final OptionImpact impact() {
        return this.impact != null ? this.impact
                : this.baseOption() == null ? null : this.baseOption().getImpact();
    }

    final Set<ResourceLocation> flags() {
        return this.flags != null ? this.flags
                : this.baseOption() == null ? Set.of() : this.baseOption().getFlags();
    }

    final DependentValue<V> defaultValue() {
        return this.defaultValue != null ? this.defaultValue
                : this.baseOption() == null ? null : this.baseOption().getDefaultValue();
    }

    final Boolean controlHiddenWhenDisabled() {
        return this.controlHiddenWhenDisabled != null ? this.controlHiddenWhenDisabled
                : this.baseOption() == null ? null : this.baseOption().getControlHiddenWhenDisabled();
    }

    final OptionBinding<V> binding() {
        return this.binding != null ? this.binding
                : this.baseOption() == null ? null : this.baseOption().getBinding();
    }

    final Consumer<ConfigState> applyHook() {
        return this.applyHook != null ? this.applyHook
                : this.baseOption() == null ? null : this.baseOption().getApplyHook();
    }

    @Override
    public StatefulOptionBuilder<V> setName(ITextComponent name) {
        super.setName(name);
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setTooltip(ITextComponent tooltip) {
        if (tooltip == null || tooltip.getUnformattedText().isBlank()) {
            throw new IllegalArgumentException("Tooltip must not be blank for option '" + this.id + "'");
        }
        this.tooltipProvider = value -> tooltip;
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setTooltip(Function<V, ITextComponent> tooltipProvider) {
        if (tooltipProvider == null) {
            throw new IllegalArgumentException("Tooltip provider must not be null for option '" + this.id + "'");
        }
        this.tooltipProvider = tooltipProvider;
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                       ResourceLocation... dependencies) {
        super.setEnabledProvider(provider, dependencies);
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setStorageHandler(StorageEventHandler storage) {
        this.storage = storage;
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setImpact(OptionImpact impact) {
        this.impact = impact;
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setFlags(OptionFlag... flags) {
        ResourceLocation[] ids = Arrays.stream(flags).map(OptionFlag::getId).toArray(ResourceLocation[]::new);
        return this.setFlags(ids);
    }

    @Override
    public StatefulOptionBuilder<V> setFlags(ResourceLocation... flags) {
        this.flags = Set.copyOf(new LinkedHashSet<>(Arrays.asList(flags)));
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setDefaultValue(V value) {
        if (value == null) {
            throw new IllegalArgumentException("Default value must not be null for option '" + this.id + "'");
        }
        this.defaultValue = new ConstantValue<>(value);
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setDefaultProvider(Function<ConfigState, V> provider,
                                                       ResourceLocation... dependencies) {
        if (provider == null) {
            throw new IllegalArgumentException("Default provider must not be null for option '" + this.id + "'");
        }
        this.defaultValue = new DynamicValue<>(provider, dependencies);
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setControlHiddenWhenDisabled(boolean hidden) {
        this.controlHiddenWhenDisabled = hidden;
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setBinding(Consumer<V> save, Supplier<V> load) {
        if (save == null || load == null) {
            throw new IllegalArgumentException("Binding functions must not be null for option '" + this.id + "'");
        }
        this.binding = new AnonymousOptionBinding<>(save, load);
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setBinding(OptionBinding<V> binding) {
        if (binding == null) {
            throw new IllegalArgumentException("Binding must not be null for option '" + this.id + "'");
        }
        this.binding = binding;
        return this;
    }

    @Override
    public StatefulOptionBuilder<V> setApplyHook(Consumer<ConfigState> hook) {
        this.applyHook = hook;
        return this;
    }
}
