package org.embeddedt.embeddium.api.options.control;

import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.client.gui.GuiScreen;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Actinium extension: a non-stateful control which opens a separately owned
 * screen when activated, mirroring the modern Sodium external-button option.
 * The control owns no value; the row renders a link-style label and forwards
 * clicks to the screen consumer.
 */
public final class ExternalButtonControl implements Control<Void> {
    private final Option<Void> option;
    private final Consumer<GuiScreen> screenConsumer;

    public ExternalButtonControl(Option<Void> option, Consumer<GuiScreen> screenConsumer) {
        this.option = Objects.requireNonNull(option, "Option must not be null");
        this.screenConsumer = Objects.requireNonNull(screenConsumer, "Screen consumer must not be null");
    }

    @Override
    public Option<Void> getOption() {
        return this.option;
    }

    /** Returns the screen-opening command owned by the integration layer. */
    public Consumer<GuiScreen> getScreenConsumer() {
        return this.screenConsumer;
    }

    @Override
    public ControlElement<Void> createElement(Dim2i dim) {
        return new ExternalButtonControlElement(this.option, dim, this.screenConsumer);
    }

    @Override
    public int getMaxWidth() {
        return 65;
    }

    private static final class ExternalButtonControlElement extends ControlElement<Void> {
        private final Consumer<GuiScreen> screenConsumer;

        private ExternalButtonControlElement(Option<Void> option, Dim2i dim, Consumer<GuiScreen> screenConsumer) {
            super(option, dim);
            this.screenConsumer = screenConsumer;
        }

        @Override
        public boolean mouseClicked(org.embeddedt.embeddium.impl.gui.framework.InteractionContext context,
                                    double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                this.screenConsumer.accept(net.minecraft.client.Minecraft.getMinecraft().currentScreen);
                context.playClickSound();
                return true;
            }
            return false;
        }
    }
}
