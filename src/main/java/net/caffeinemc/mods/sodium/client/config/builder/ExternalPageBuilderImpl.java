package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.ExternalPageBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

final class ExternalPageBuilderImpl implements ExternalPageBuilder {
    private ITextComponent name;
    private Consumer<GuiScreen> screenConsumer;

    ExternalPage build() {
        return new ExternalPage(this.name, this.screenConsumer);
    }

    @Override
    public ExternalPageBuilder setName(ITextComponent name) {
        this.name = name;
        return this;
    }

    @Override
    public ExternalPageBuilder setScreenConsumer(Consumer<GuiScreen> screenConsumer) {
        this.screenConsumer = screenConsumer;
        return this;
    }
}
