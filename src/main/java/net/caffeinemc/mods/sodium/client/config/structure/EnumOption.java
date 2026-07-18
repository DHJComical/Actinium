package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Enum stateful option with a dynamic allowed-value set. */
public final class EnumOption<E extends Enum<E>> extends StatefulOption<E> {
    private final Class<E> enumClass;
    private final DependentValue<Set<E>> allowedValues;
    private final Function<E, ITextComponent> elementNameProvider;

    public EnumOption(ResourceLocation id, Collection<ResourceLocation> dependencies, ITextComponent name,
                      DependentValue<Boolean> enabled, StorageEventHandler storage,
                      Function<E, ITextComponent> tooltipProvider, OptionImpact impact,
                      Set<ResourceLocation> flags, DependentValue<E> defaultValue,
                      Boolean controlHiddenWhenDisabled, OptionBinding<E> binding,
                      Consumer<ConfigState> applyHook, Class<E> enumClass,
                      DependentValue<Set<E>> allowedValues, Function<E, ITextComponent> elementNameProvider) {
        super(id, dependencies, name, enabled, storage, tooltipProvider, impact, flags, defaultValue,
                controlHiddenWhenDisabled, binding, applyHook);
        this.enumClass = Objects.requireNonNull(enumClass, "Enum class must not be null");
        this.allowedValues = Objects.requireNonNull(allowedValues, "Allowed values must not be null");
        authorizeParent(allowedValues, id);
        this.elementNameProvider = Objects.requireNonNull(elementNameProvider, "Element name provider must not be null");
    }

    @Override
    protected E validateOptionValue(E value) {
        if (!this.enumClass.isInstance(value)) {
            return this.getDefaultValueProvider().get(this.requireState());
        }
        if (!this.allowedValues.get(this.requireState()).contains(value)) {
            return this.getDefaultValueProvider().get(this.requireState());
        }
        return value;
    }

    /** Returns the enum class retained without reflection-based discovery. */
    public Class<E> getEnumClass() {
        return this.enumClass;
    }

    /** Returns localized text for an enum element. */
    public ITextComponent getElementName(E value) {
        return this.elementNameProvider.apply(value);
    }

    /** Returns whether the value is currently exposed by the dynamic allowed-value set. */
    public boolean isValueAllowed(E value) {
        return this.allowedValues.get(this.requireState()).contains(value);
    }

    /** Returns the dynamic allowed-value provider. */
    public DependentValue<Set<E>> getAllowedValues() {
        return this.allowedValues;
    }

    /** Returns localized enum element text generation. */
    public Function<E, ITextComponent> getElementNameProvider() {
        return this.elementNameProvider;
    }
}
