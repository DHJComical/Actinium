package org.embeddedt.embeddium.api.options.structure;

import net.minecraft.client.gui.GuiScreen;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Actinium extension: a navigation-only page which opens a separately owned
 * screen when activated. The page owns no option groups; the tab rail shows
 * it as a normal tab and activation forwards to the screen consumer.
 */
public final class ExternalPage extends OptionPage {
    private final Consumer<GuiScreen> screenConsumer;

    public ExternalPage(OptionIdentifier<Void> id, TextComponent name, Consumer<GuiScreen> screenConsumer) {
        super(id, name, List.of());
        this.screenConsumer = Objects.requireNonNull(screenConsumer, "Screen consumer must not be null");
    }

    /** Returns the screen-opening command owned by the integration layer. */
    public Consumer<GuiScreen> getScreenConsumer() {
        return this.screenConsumer;
    }
}
