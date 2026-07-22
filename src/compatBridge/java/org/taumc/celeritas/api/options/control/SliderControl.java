package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.structure.Option;

/**
 * Legacy integer slider metadata consumed by the bridge GUI adapter.
 */
public class SliderControl implements Control<Integer> {
    private final Option<Integer> option;
    private final int min;
    private final int max;
    private final int interval;
    private final ControlValueFormatter formatter;

    public SliderControl(Option<Integer> option, int min, int max, int interval, ControlValueFormatter formatter) {
        if (max <= min || interval <= 0 || (max - min) % interval != 0 || formatter == null) {
            throw new IllegalArgumentException("Invalid slider bounds or formatter");
        }
        this.option = option;
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.formatter = formatter;
    }

    @Override
    public Option<Integer> getOption() {
        return option;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getInterval() {
        return interval;
    }

    public ControlValueFormatter getFormatter() {
        return formatter;
    }

    @Override
    public ControlElement<Integer> createElement(Dim2i dim) {
        return new Button(option, dim, min, max, interval, formatter);
    }

    @Override
    public int getMaxWidth() {
        return 130;
    }

    private static class Button extends ControlElement<Integer> {
        private static final int THUMB_WIDTH = 2;
        private static final int TRACK_HEIGHT = 1;
        private final Dim2i sliderBounds;
        private final ControlValueFormatter formatter;
        private final int min;
        private final int range;
        private final int interval;
        private double thumbPosition;
        private boolean sliderHeld;

        Button(Option<Integer> option, Dim2i dim, int min, int max, int interval,
               ControlValueFormatter formatter) {
            super(option, dim);
            this.min = min;
            this.range = max - min;
            this.interval = interval;
            this.thumbPosition = getThumbPositionForValue(option.getValue());
            this.formatter = formatter;
            this.sliderBounds = new Dim2i(dim.getLimitX() - 96, dim.getCenterY() - 5, 90, 10);
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            if (option.isAvailable() && isMouseOver(mouseX, mouseY)) {
                renderSlider(drawContext);
            } else {
                renderStandaloneValue(drawContext);
            }
        }

        private void renderStandaloneValue(DrawContext drawContext) {
            var label = formatter.format(option.getValue());
            int labelWidth = drawContext.getStringWidth(label);
            drawContext.drawString(label, sliderBounds.getLimitX() - labelWidth,
                    sliderBounds.getCenterY() - 4, 0xFFFFFFFF);
        }

        private void renderSlider(DrawContext drawContext) {
            thumbPosition = getThumbPositionForValue(option.getValue());
            double thumbOffset = clamp(0.0D, sliderBounds.width(),
                    (double) (getIntValue() - min) / range * sliderBounds.width());
            int thumbX = (int) (sliderBounds.x() + thumbOffset - THUMB_WIDTH);
            int trackY = (int) (sliderBounds.y() + sliderBounds.height() / 2.0F - TRACK_HEIGHT / 2.0D);
            drawContext.fill(thumbX, sliderBounds.y(), thumbX + THUMB_WIDTH * 2,
                    sliderBounds.getLimitY(), 0xFFFFFFFF);
            drawContext.fill(sliderBounds.x(), trackY, sliderBounds.getLimitX(), trackY + TRACK_HEIGHT, 0xFFFFFFFF);
            var label = formatter.format(getIntValue());
            drawContext.drawString(label, sliderBounds.x() - drawContext.getStringWidth(label) - 6,
                    sliderBounds.getCenterY() - 4, 0xFFFFFFFF);
        }

        private int getIntValue() {
            return min + interval * (int) Math.round(getSnappedThumbPosition() / interval);
        }

        private double getSnappedThumbPosition() {
            return thumbPosition / (1.0D / range);
        }

        private double getThumbPositionForValue(int value) {
            return (value - min) * (1.0D / range);
        }

        @Override
        public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
            sliderHeld = false;
            if (option.isAvailable() && button == 0 && dim.containsCursor(mouseX, mouseY)) {
                if (sliderBounds.containsCursor(mouseX, mouseY)) {
                    setValueFromMouse(mouseX);
                    sliderHeld = true;
                }
                return true;
            }
            return false;
        }

        private void setValueFromMouse(double mouseX) {
            setValue((mouseX - sliderBounds.x()) / (sliderBounds.width() - 1.0D));
        }

        private void setValue(double value) {
            thumbPosition = clamp(0.0D, 1.0D, value);
            int intValue = getIntValue();
            if (!option.getValue().equals(intValue)) {
                option.setValue(intValue);
            }
        }

        @Override
        public boolean mouseDragged(InteractionContext context, double mouseX, double mouseY, int button,
                                    double deltaX, double deltaY) {
            if (option.isAvailable() && button == 0 && sliderBounds.containsCursor(mouseX, mouseY)) {
                if (sliderHeld) {
                    setValueFromMouse(mouseX);
                }
                return true;
            }
            return false;
        }

        private static double clamp(double min, double max, double value) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
