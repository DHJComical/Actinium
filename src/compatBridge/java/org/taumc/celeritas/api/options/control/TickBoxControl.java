package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.theme.DefaultColors;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.structure.Option;

/**
 * Legacy boolean checkbox control.
 */
public class TickBoxControl implements Control<Boolean> {
    private final Option<Boolean> option;

    public TickBoxControl(Option<Boolean> option) {
        this.option = option;
    }

    @Override
    public Option<Boolean> getOption() {
        return option;
    }

    @Override
    public ControlElement<Boolean> createElement(Dim2i dim) {
        return new TickBoxControlElement(option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 30;
    }

    private static class TickBoxControlElement extends ControlElement<Boolean> {
        private final Dim2i button;

        TickBoxControlElement(Option<Boolean> option, Dim2i dim) {
            super(option, dim);
            this.button = new Dim2i(dim.getLimitX() - 16, dim.getCenterY() - 5, 10, 10);
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            int x = button.x();
            int y = button.y();
            int limitX = x + button.width();
            int limitY = y + button.height();
            boolean enabled = option.isAvailable();
            boolean ticked = option.getValue();
            int color = enabled ? ticked ? DefaultColors.ELEMENT_ACTIVATED : 0xFFFFFFFF : 0xFFAAAAAA;
            if (ticked) {
                drawContext.fill(x + 2, y + 2, limitX - 2, limitY - 2, color);
            }
            drawContext.drawBorder(x, y, limitX, limitY, color);
        }

        @Override
        public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
            if (option.isAvailable() && button == 0 && dim.containsCursor(mouseX, mouseY)) {
                option.setValue(!option.getValue());
                context.playClickSound();
                return true;
            }
            return false;
        }
    }
}
