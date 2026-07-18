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

/** Boolean stateful option consumed by tick-box controls. */
public final class BooleanOption extends StatefulOption<Boolean> {
    public BooleanOption(ResourceLocation id, Collection<ResourceLocation> dependencies, ITextComponent name,
                         DependentValue<Boolean> enabled, StorageEventHandler storage,
                         Function<Boolean, ITextComponent> tooltipProvider, OptionImpact impact,
                         Set<ResourceLocation> flags, DependentValue<Boolean> defaultValue,
                         Boolean controlHiddenWhenDisabled, OptionBinding<Boolean> binding,
                         Consumer<ConfigState> applyHook) {
        super(id, dependencies, name, enabled, storage, tooltipProvider, impact, flags, defaultValue,
                controlHiddenWhenDisabled, binding, applyHook);
    }

    @Override
    protected Boolean validateOptionValue(Boolean value) {
        return Objects.requireNonNull(value, "Boolean option value must not be null");
    }
}
