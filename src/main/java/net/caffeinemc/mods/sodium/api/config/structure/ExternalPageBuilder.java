package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

/**
 * Builds a page which transfers control to a separately owned configuration screen.
 */
public interface ExternalPageBuilder extends PageBuilder {
    /** Sets the required localized page name. */
    ExternalPageBuilder setName(ITextComponent name);

    /** Sets the action which opens the external page from the current Sodium screen. */
    ExternalPageBuilder setScreenConsumer(Consumer<GuiScreen> screenConsumer);
}
