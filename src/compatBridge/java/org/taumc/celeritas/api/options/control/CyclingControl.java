package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.api.options.structure.Option;

/**
 * Legacy enum/integer cycling control retaining allowed values and labels.
 */
public class CyclingControl<T> implements Control<T> {
    private final Option<T> option;
    private final T[] allowedValues;
    private final TextComponent[] names;

    public CyclingControl(Option<T> option, Class<T> enumType) {
        this(option, enumType.getEnumConstants(), determineNames(enumType.getEnumConstants()));
    }

    public CyclingControl(Option<T> option, Class<T> enumType, TextComponent[] names) {
        this(option, enumType.getEnumConstants(), names);
    }

    public CyclingControl(Option<T> option, Class<T> enumType, T[] values) {
        this(option, values, determineNames(values));
    }

    public CyclingControl(Option<T> option, T[] values, TextComponent[] names) {
        this.option = option;
        if (values.length != names.length) {
            throw new IllegalArgumentException();
        }
        this.allowedValues = values;
        this.names = names;
    }

    @Override
    public Option<T> getOption() {
        return option;
    }

    public TextComponent[] getNames() {
        return names;
    }

    public T[] getAllowedValues() {
        return allowedValues;
    }

    @Override
    public ControlElement<T> createElement(Dim2i dim) {
        return new CyclingControlElement<>(option, dim, allowedValues, names);
    }

    @Override
    public int getMaxWidth() {
        return 70;
    }

    private static TextComponent[] determineNames(Object[] values) {
        TextComponent[] result = new TextComponent[values.length];
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof TextProvider provider) {
                result[i] = provider.getLocalizedName();
            } else if (value instanceof Enum<?> enumeration) {
                result[i] = TextComponent.literal(enumeration.name());
            } else {
                String message = "Could not figure out name of object " + value;
                CeleritasVintage.logger().error(message);
                throw new IllegalArgumentException(message);
            }
        }
        return result;
    }

    private static class CyclingControlElement<T> extends ControlElement<T> {
        private final T[] allowedValues;
        private final TextComponent[] names;

        CyclingControlElement(Option<T> option, Dim2i dim, T[] allowedValues, TextComponent[] names) {
            super(option, dim);
            this.allowedValues = allowedValues;
            this.names = names;
        }

        private int getCurrentIndex() {
            for (int index = 0; index < allowedValues.length; index++) {
                if (allowedValues[index] == option.getValue()) {
                    return index;
                }
            }
            return 0;
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            TextComponent name = names[getCurrentIndex()];
            if (!option.isAvailable()) {
                name = name.withStyle(TextFormattingStyle.GRAY, TextFormattingStyle.STRIKETHROUGH);
            }
            int width = drawContext.getStringWidth(name);
            drawContext.drawString(name, dim.getLimitX() - width - 6, dim.getCenterY() - 4, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
            if (option.isAvailable() && button == 0 && dim.containsCursor(mouseX, mouseY)) {
                int current = getCurrentIndex();
                boolean reverse = context.isSpecialKeyDown(InteractionContext.SpecialKey.SHIFT);
                current = reverse ? (current + allowedValues.length - 1) % allowedValues.length
                        : (current + 1) % allowedValues.length;
                option.setValue(allowedValues[current]);
                context.playClickSound();
                return true;
            }
            return false;
        }
    }
}
