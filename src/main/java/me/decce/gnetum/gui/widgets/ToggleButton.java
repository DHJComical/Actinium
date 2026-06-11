package me.decce.gnetum.gui.widgets;

import me.decce.gnetum.util.AnyBoolean;

import java.util.function.Supplier;

public class ToggleButton extends GuiButtonEx {
    private Supplier<String> text;
    private Supplier<String> tooltip;
    private AnyBoolean value;

    public ToggleButton(int id, int left, int top, int width, int height, AnyBoolean value, Supplier<String> text) {
        super(id, left, top, width, height, "");
        this.text = text;
        this.value = value;
        this.updateMessage();
    }

    public void setTooltip(Supplier<String> tooltip) {
        this.tooltip = tooltip;
        this.updateTooltip();
    }

    @Override
    public void onPress() {
        super.onPress();
        this.next();
        this.updateMessage();
    }

    protected void next() {
        value.next();
    }

    private void updateMessage() {
        this.displayString = String.format(text.get(), value.text());
        this.updateTooltip();
    }

    private void updateTooltip() {
        if (tooltip == null) {
            super.setTooltip(null);
            return;
        }
        String str = tooltip.get();
        if (str != null && !str.isEmpty()) {
            super.setTooltip(str);
        }
        else super.setTooltip(null);
    }
}
