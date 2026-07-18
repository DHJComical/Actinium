package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Type-refined builder for enum options. */
public interface EnumOptionBuilder<E extends Enum<E>> extends StatefulOptionBuilder<E> {
    @Override EnumOptionBuilder<E> setName(ITextComponent name);
    @Override EnumOptionBuilder<E> setTooltip(ITextComponent tooltip);
    @Override EnumOptionBuilder<E> setTooltip(Function<E, ITextComponent> tooltipProvider);
    @Override EnumOptionBuilder<E> setEnabled(boolean enabled);
    @Override EnumOptionBuilder<E> setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                      ResourceLocation... dependencies);
    @Override EnumOptionBuilder<E> setStorageHandler(StorageEventHandler storage);
    @Override EnumOptionBuilder<E> setImpact(OptionImpact impact);
    @Override EnumOptionBuilder<E> setFlags(OptionFlag... flags);
    @Override EnumOptionBuilder<E> setFlags(ResourceLocation... flags);
    @Override EnumOptionBuilder<E> setDefaultValue(E value);
    @Override EnumOptionBuilder<E> setDefaultProvider(Function<ConfigState, E> provider,
                                                      ResourceLocation... dependencies);
    @Override EnumOptionBuilder<E> setControlHiddenWhenDisabled(boolean hidden);
    @Override EnumOptionBuilder<E> setBinding(Consumer<E> save, Supplier<E> load);
    @Override EnumOptionBuilder<E> setBinding(OptionBinding<E> binding);
    @Override EnumOptionBuilder<E> setApplyHook(Consumer<ConfigState> hook);

    /** Sets the values available to the user. */
    EnumOptionBuilder<E> setAllowedValues(Set<E> values);

    /** Sets a dynamic allowed-value set and its complete dependency list. */
    EnumOptionBuilder<E> setAllowedValueProvider(Function<ConfigState, Set<E>> provider,
                                                 ResourceLocation... dependencies);

    /** Compatibility alias matching the upstream API spelling. */
    default EnumOptionBuilder<E> setAllowedValuesProvider(Function<ConfigState, Set<E>> provider,
                                                         ResourceLocation... dependencies) {
        return setAllowedValueProvider(provider, dependencies);
    }

    /** Creates a display-name provider indexed by enum ordinal. */
    static <E extends Enum<E>> Function<E, ITextComponent> nameProviderFrom(ITextComponent... names) {
        if (names == null) {
            throw new IllegalArgumentException("Enum names must not be null");
        }
        return value -> {
            if (value.ordinal() >= names.length || names[value.ordinal()] == null) {
                throw new IllegalArgumentException("Missing display name for enum value " + value);
            }
            return names[value.ordinal()];
        };
    }

    /** Sets localized display text for each enum constant. */
    EnumOptionBuilder<E> setElementNameProvider(Function<E, ITextComponent> provider);
}
