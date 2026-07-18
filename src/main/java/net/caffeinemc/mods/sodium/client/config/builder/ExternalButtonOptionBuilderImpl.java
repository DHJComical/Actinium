package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.ExternalButtonOptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;

final class ExternalButtonOptionBuilderImpl extends OptionBuilderImpl<ExternalButtonOption>
        implements ExternalButtonOptionBuilder {
    private ITextComponent tooltip;
    private Consumer<GuiScreen> screenConsumer;

    ExternalButtonOptionBuilderImpl(ResourceLocation id) {
        super(id);
    }

    @Override
    ExternalButtonOption build() {
        this.validateBase();
        if (this.tooltip == null || this.tooltip.getUnformattedText().isBlank()) {
            throw new IllegalStateException("Tooltip must be set for external button option '" + this.id + "'");
        }
        if (this.screenConsumer == null) {
            throw new IllegalStateException("Screen consumer must be set for external button option '" + this.id + "'");
        }
        return new ExternalButtonOption(this.id, this.collectDependencies(), this.name(), this.enabled(),
                this.tooltip, this.screenConsumer);
    }

    @Override
    Class<ExternalButtonOption> getOptionClass() {
        return ExternalButtonOption.class;
    }

    @Override public ExternalButtonOptionBuilder setName(ITextComponent name) { super.setName(name); return this; }
    @Override public ExternalButtonOptionBuilder setTooltip(ITextComponent tooltip) { this.tooltip = tooltip; return this; }
    @Override public ExternalButtonOptionBuilder setEnabled(boolean enabled) { super.setEnabled(enabled); return this; }
    @Override public ExternalButtonOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                                    ResourceLocation... dependencies) {
        super.setEnabledProvider(provider, dependencies);
        return this;
    }
    @Override public ExternalButtonOptionBuilder setScreenConsumer(Consumer<GuiScreen> screenConsumer) {
        this.screenConsumer = screenConsumer;
        return this;
    }
}
