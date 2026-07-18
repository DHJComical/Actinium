package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class BooleanOptionBuilderImpl extends StatefulOptionBuilderImpl<BooleanOption, Boolean>
        implements BooleanOptionBuilder {
    BooleanOptionBuilderImpl(ResourceLocation id) {
        super(id);
    }

    @Override public BooleanOptionBuilder setName(ITextComponent name) { super.setName(name); return this; }
    @Override public BooleanOptionBuilder setTooltip(ITextComponent tooltip) { super.setTooltip(tooltip); return this; }
    @Override public BooleanOptionBuilder setTooltip(Function<Boolean, ITextComponent> provider) { super.setTooltip(provider); return this; }
    @Override public BooleanOptionBuilder setEnabled(boolean enabled) { super.setEnabled(enabled); return this; }
    @Override public BooleanOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                              ResourceLocation... dependencies) {
        super.setEnabledProvider(provider, dependencies); return this;
    }
    @Override public BooleanOptionBuilder setStorageHandler(StorageEventHandler storage) { super.setStorageHandler(storage); return this; }
    @Override public BooleanOptionBuilder setImpact(OptionImpact impact) { super.setImpact(impact); return this; }
    @Override public BooleanOptionBuilder setFlags(OptionFlag... flags) { super.setFlags(flags); return this; }
    @Override public BooleanOptionBuilder setFlags(ResourceLocation... flags) { super.setFlags(flags); return this; }
    @Override public BooleanOptionBuilder setDefaultValue(Boolean value) { super.setDefaultValue(value); return this; }
    @Override public BooleanOptionBuilder setDefaultProvider(Function<ConfigState, Boolean> provider,
                                                             ResourceLocation... dependencies) {
        super.setDefaultProvider(provider, dependencies); return this;
    }
    @Override public BooleanOptionBuilder setControlHiddenWhenDisabled(boolean hidden) { super.setControlHiddenWhenDisabled(hidden); return this; }
    @Override public BooleanOptionBuilder setBinding(Consumer<Boolean> save, Supplier<Boolean> load) { super.setBinding(save, load); return this; }
    @Override public BooleanOptionBuilder setBinding(OptionBinding<Boolean> binding) { super.setBinding(binding); return this; }
    @Override public BooleanOptionBuilder setApplyHook(Consumer<ConfigState> hook) { super.setApplyHook(hook); return this; }

    @Override
    BooleanOption build() {
        this.validateStateful();
        return new BooleanOption(this.id, this.collectDependencies(this.defaultValue()), this.name(), this.enabled(),
                this.storage(), this.tooltipProvider(), this.impact(), this.flags(), this.defaultValue(),
                this.controlHiddenWhenDisabled(), this.binding(), this.applyHook());
    }

    @Override
    Class<BooleanOption> getOptionClass() {
        return BooleanOption.class;
    }
}
