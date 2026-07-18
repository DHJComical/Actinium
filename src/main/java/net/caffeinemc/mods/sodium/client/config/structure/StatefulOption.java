package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Option with a binding-backed applied baseline and an independently editable pending value.
 */
public abstract class StatefulOption<V> extends Option {
    private static final Logger LOGGER = LogManager.getLogger("SodiumConfig");
    private final StorageEventHandler storage;
    private final Function<V, ITextComponent> tooltipProvider;
    private final OptionImpact impact;
    private final Set<ResourceLocation> flags;
    private final DependentValue<V> defaultValue;
    private final Boolean controlHiddenWhenDisabled;
    private final OptionBinding<V> binding;
    private final Consumer<ConfigState> applyHook;
    private V appliedValue;
    private V pendingValue;
    private boolean requiresPersistence;

    protected StatefulOption(ResourceLocation id, Collection<ResourceLocation> dependencies, ITextComponent name,
                             DependentValue<Boolean> enabled, StorageEventHandler storage,
                             Function<V, ITextComponent> tooltipProvider, OptionImpact impact,
                             Set<ResourceLocation> flags, DependentValue<V> defaultValue,
                             Boolean controlHiddenWhenDisabled, OptionBinding<V> binding,
                             Consumer<ConfigState> applyHook) {
        super(id, dependencies, name, enabled);
        this.storage = Objects.requireNonNull(storage, "Storage handler must not be null");
        this.tooltipProvider = Objects.requireNonNull(tooltipProvider, "Tooltip provider must not be null");
        this.impact = impact;
        this.flags = Set.copyOf(flags);
        this.defaultValue = Objects.requireNonNull(defaultValue, "Default value must not be null");
        authorizeParent(defaultValue, id);
        this.controlHiddenWhenDisabled = controlHiddenWhenDisabled;
        this.binding = Objects.requireNonNull(binding, "Binding must not be null");
        this.applyHook = applyHook;
    }

    /** Replaces only the pending value; bindings and storage remain untouched. */
    public final void modifyValue(V value) {
        this.pendingValue = Objects.requireNonNull(value, "Pending value must not be null");
    }

    /** Returns the validated pending value currently represented by the model. */
    public final V getPendingValue() {
        return this.validateOptionValue(this.pendingValue);
    }

    /** Returns the baseline established by load or the most recent successful apply. */
    public final V getAppliedValue() {
        return this.appliedValue;
    }

    @Override
    public final boolean hasChanged() {
        return this.requiresPersistence || !Objects.equals(this.getPendingValue(), this.appliedValue);
    }

    @Override
    public final void undo() {
        this.pendingValue = this.appliedValue;
    }

    @Override
    public final void resetToDefault() {
        this.pendingValue = this.defaultValue.get(this.requireState());
    }

    @Override
    public final ITextComponent getTooltip() {
        ITextComponent tooltip = this.tooltipProvider.apply(this.getPendingValue());
        if (tooltip == null || tooltip.getUnformattedText().isBlank()) {
            throw new IllegalStateException("Tooltip provider returned blank text for option '" + this.getId() + "'");
        }
        return tooltip;
    }

    @Override
    public final OptionImpact getImpact() {
        return this.impact;
    }

    @Override
    public final Set<ResourceLocation> getFlags() {
        return this.flags;
    }

    /** Returns the persistence boundary used for transaction-level deduplication. */
    public final StorageEventHandler getStorage() {
        return this.storage;
    }

    /** Returns whether a disabled control should be omitted instead of merely disabled. */
    public final boolean shouldHideControl() {
        return !this.isEnabled() && (this.controlHiddenWhenDisabled == null || this.controlHiddenWhenDisabled);
    }

    /** Returns the public binding for adapter inspection and integration tests. */
    public final OptionBinding<V> getBinding() {
        return this.binding;
    }

    /** Returns the value-sensitive tooltip provider. */
    public final Function<V, ITextComponent> getTooltipProvider() {
        return this.tooltipProvider;
    }

    /** Returns the option-defined reset provider. */
    public final DependentValue<V> getDefaultValue() {
        return this.defaultValue;
    }

    /** Returns the explicit hidden-control preference, or null for the default behavior. */
    public final Boolean getControlHiddenWhenDisabled() {
        return this.controlHiddenWhenDisabled;
    }

    /** Returns the optional post-save option hook. */
    public final Consumer<ConfigState> getApplyHook() {
        return this.applyHook;
    }

    /** Exposes the reset provider to type-specific validation implementations. */
    protected final DependentValue<V> getDefaultValueProvider() {
        return this.defaultValue;
    }

    final void loadInitialValue() {
        V loaded;
        try {
            loaded = this.binding.load();
            if (loaded == null) {
                throw new IllegalArgumentException("Binding returned null");
            }
            this.appliedValue = this.validateOptionValue(loaded);
            this.requiresPersistence = !Objects.equals(loaded, this.appliedValue);
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid binding value for option '{}'; using its default", this.getId(), exception);
            this.appliedValue = this.defaultValue.get(this.requireState());
            this.appliedValue = this.validateOptionValue(this.appliedValue);
            this.requiresPersistence = true;
        }
        this.pendingValue = this.appliedValue;
    }

    final V validateForApply() {
        V optionValidated = this.validateOptionValue(this.pendingValue);
        V bindingValidated = Objects.requireNonNull(this.binding.validate(optionValidated),
                "Binding validator returned null for option '" + this.getId() + "'");
        return this.validateOptionValue(bindingValidated);
    }

    final void saveValidatedValue(Object value) {
        @SuppressWarnings("unchecked")
        V typedValue = (V) value;
        this.binding.save(typedValue);
        this.pendingValue = typedValue;
    }

    final void commitAppliedValue() {
        this.appliedValue = this.pendingValue;
        this.requiresPersistence = false;
    }

    final void runApplyHook() {
        if (this.applyHook != null) {
            this.applyHook.accept(this.requireState());
        }
    }

    final Object captureBindingValue() {
        return this.binding.load();
    }

    final void restoreBindingValue(Object value) {
        @SuppressWarnings("unchecked")
        V typedValue = (V) value;
        this.binding.save(typedValue);
    }

    final Object capturePendingValue() {
        return this.pendingValue;
    }

    final void restorePendingValue(Object value) {
        @SuppressWarnings("unchecked")
        V typedValue = (V) value;
        this.pendingValue = typedValue;
    }

    protected abstract V validateOptionValue(V value);
}
