package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.widgets.AbstractWidget;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.structure.Option;

/**
 * Base implementation used by legacy Celeritas option controls.
 */
public class ControlElement<T> extends AbstractWidget implements OptionControlElement<T> {
    protected final Option<T> option;
    protected final Dim2i dim;
    private final FlatButtonWidget.Style style = FlatButtonWidget.Style.defaults();

    public ControlElement(Option<T> option, Dim2i dim) {
        this.option = option;
        this.dim = dim;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        String name = drawContext.extractString(option.getName());
        boolean hovered = isMouseOver(mouseX, mouseY);
        if (hovered && drawContext.getStringWidth(name) > dim.width() - option.getControl().getMaxWidth()) {
            name = name.substring(0, Math.min(name.length(), 10)) + "...";
        }
        TextComponent label;
        if (!option.isAvailable()) {
            label = TextComponent.literal(name).withStyle(TextFormattingStyle.GRAY, TextFormattingStyle.STRIKETHROUGH);
        } else if (option.hasChanged()) {
            label = TextComponent.literal(name + " *").withStyle(TextFormattingStyle.ITALIC);
        } else {
            label = TextComponent.literal(name).withStyle(TextFormattingStyle.WHITE);
        }
        drawContext.fill(dim.x(), dim.y(), dim.getLimitX(), dim.getLimitY(),
                hovered ? style.bgHovered : style.bgDefault);
        drawContext.drawString(label, dim.x() + 6, dim.getCenterY() - 4, style.textDefault);
    }

    @Override
    public Option<T> getOption() {
        return option;
    }

    public Dim2i getDimensions() {
        return dim;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return dim.containsCursor(x, y);
    }
}
