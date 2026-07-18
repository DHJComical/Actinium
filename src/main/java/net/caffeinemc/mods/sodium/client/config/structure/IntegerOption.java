package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Integer stateful option with a dynamic stepped validator. */
public final class IntegerOption extends StatefulOption<Integer> {
    private final DependentValue<? extends SteppedValidator> validator;
    private final ControlValueFormatter formatter;

    public IntegerOption(ResourceLocation id, Collection<ResourceLocation> dependencies, ITextComponent name,
                         DependentValue<Boolean> enabled, StorageEventHandler storage,
                         Function<Integer, ITextComponent> tooltipProvider, OptionImpact impact,
                         Set<ResourceLocation> flags, DependentValue<Integer> defaultValue,
                         Boolean controlHiddenWhenDisabled, OptionBinding<Integer> binding,
                         Consumer<ConfigState> applyHook, DependentValue<? extends SteppedValidator> validator,
                         ControlValueFormatter formatter) {
        super(id, dependencies, name, enabled, storage, tooltipProvider, impact, flags, defaultValue,
                controlHiddenWhenDisabled, binding, applyHook);
        this.validator = Objects.requireNonNull(validator, "Integer validator must not be null");
        authorizeParent(validator, id);
        this.formatter = formatter;
    }

    @Override
    protected Integer validateOptionValue(Integer value) {
        return this.validator.get(this.requireState()).getValidatedValue(value,
                () -> this.getDefaultValueProvider().get(this.requireState()));
    }

    /** Returns the current stepped validator for control construction. */
    public SteppedValidator getSteppedValidator() {
        return this.validator.get(this.requireState());
    }

    /** Formats a control value, failing fast when no formatter was registered. */
    public ITextComponent formatValue(int value) {
        if (this.formatter == null) {
            throw new IllegalStateException("No value formatter registered for option '" + this.getId() + "'");
        }
        return this.formatter.format(value);
    }

    /** Returns the dynamic validator provider used by controls and dependency inspection. */
    public DependentValue<? extends SteppedValidator> getValidatorProvider() {
        return this.validator;
    }

    /** Returns the optional numeric value formatter. */
    public ControlValueFormatter getValueFormatter() {
        return this.formatter;
    }

}
