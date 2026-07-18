package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;
import java.util.function.Function;

/** Builds a non-stateful option which opens a separately owned screen. */
public interface ExternalButtonOptionBuilder extends OptionBuilder {
    @Override ExternalButtonOptionBuilder setName(ITextComponent name);
    @Override ExternalButtonOptionBuilder setTooltip(ITextComponent tooltip);
    @Override ExternalButtonOptionBuilder setEnabled(boolean enabled);
    @Override ExternalButtonOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider,
                                                             ResourceLocation... dependencies);

    /** Sets the action which opens the external screen. */
    ExternalButtonOptionBuilder setScreenConsumer(Consumer<GuiScreen> screenConsumer);
}
