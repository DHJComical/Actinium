package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Type-refined builder for boolean options. */
public interface BooleanOptionBuilder extends StatefulOptionBuilder<Boolean> {
    @Override BooleanOptionBuilder setName(ITextComponent name);
    @Override BooleanOptionBuilder setTooltip(ITextComponent tooltip);
    @Override BooleanOptionBuilder setTooltip(Function<Boolean, ITextComponent> tooltipProvider);
    @Override BooleanOptionBuilder setEnabled(boolean enabled);
    @Override BooleanOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                      ResourceLocation... dependencies);
    @Override BooleanOptionBuilder setStorageHandler(StorageEventHandler storage);
    @Override BooleanOptionBuilder setImpact(OptionImpact impact);
    @Override BooleanOptionBuilder setFlags(OptionFlag... flags);
    @Override BooleanOptionBuilder setFlags(ResourceLocation... flags);
    @Override BooleanOptionBuilder setDefaultValue(Boolean value);
    @Override BooleanOptionBuilder setDefaultProvider(Function<ConfigState, Boolean> provider,
                                                      ResourceLocation... dependencies);
    @Override BooleanOptionBuilder setControlHiddenWhenDisabled(boolean hidden);
    @Override BooleanOptionBuilder setBinding(Consumer<Boolean> save, Supplier<Boolean> load);
    @Override BooleanOptionBuilder setBinding(OptionBinding<Boolean> binding);
    @Override BooleanOptionBuilder setApplyHook(Consumer<ConfigState> hook);
}
