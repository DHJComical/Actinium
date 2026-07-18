package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Navigation entry whose state and apply/discard lifecycle are owned by another screen.
 */
public record ExternalPage(ITextComponent name, Consumer<GuiScreen> screenConsumer) implements Page {
    public ExternalPage {
        if (name == null || name.getUnformattedText().isBlank()) {
            throw new IllegalArgumentException("External page name must not be blank");
        }
        Objects.requireNonNull(screenConsumer, "External page screen consumer must not be null");
    }

    @Override
    public List<OptionGroup> groups() {
        return List.of();
    }

    @Override
    public void registerTextSources(SearchIndex index, ModOptions modOptions) {
        // External pages own their searchable content and lifecycle.
    }
}
