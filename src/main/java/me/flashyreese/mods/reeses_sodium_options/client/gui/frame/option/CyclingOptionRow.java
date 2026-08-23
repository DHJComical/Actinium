package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import com.dhj.actinium.gui.rso.compat.KeyEvent;
import com.dhj.actinium.gui.rso.compat.MouseButtonEvent;
import com.dhj.actinium.gui.rso.compat.NarratedElementType;
import com.dhj.actinium.gui.rso.compat.NarrationElementOutput;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

final class CyclingOptionRow extends AbstractOptionRow {
    private static final int MAX_CONTENT_WIDTH = 70;

    private final RsoOption option;

    CyclingOptionRow(LayoutBounds dim, GuiTheme theme, OptionStateStore optionStateStore, RsoOption option) {
        super(dim, theme, optionStateStore, option);
        this.option = option;
    }

    @Override
    public RsoOption getOption() {
        return this.option;
    }

    @Override
    protected int controlContentWidth() {
        return Math.min(MAX_CONTENT_WIDTH, this.font.width(this.displayValue()));
    }

    @Override
    public List<ControlGuide> controlGuides() {
        return this.canShowControlGuide() ? List.of(ControlGuide.press(Component.translatable("rso.controller.guide.next_value"))) : List.of();
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.option.shouldHideControl()) {
            return;
        }

        Component value = this.displayValue();
        int valueWidth = this.font.width(value);
        int x = this.rightAlignedControlX(valueWidth);
        int y = this.centeredTextY();

        this.drawString(guiGraphics, value, x, y, 0xFFFFFFFF);

        if (this.option.isEnabled()) {
            this.requestPointerCursorIfHovered(guiGraphics);
        }
    }

    @Override
    protected boolean controlMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean reverse = GuiScreen.isShiftKeyDown();
        if (event.button() == 1) {
            if (!ReeseSodiumOptionsConfig.config().isReverseCyclingControls()) {
                return false;
            }

            reverse = true;
        } else if (event.button() != 0) {
            return false;
        }

        if (!this.option.isEnabled()
                || this.option.shouldHideControl()
                || !this.isMouseOverRow(event.x(), event.y())) {
            return false;
        }

        this.cycleControl(reverse);

        return true;
    }

    @Override
    protected boolean controlKeyPressed(KeyEvent event) {
        if (!this.isRowFocused() || !event.isSelection()) {
            return false;
        }

        this.cycleControl(GuiScreen.isShiftKeyDown());

        return true;
    }

    @Override
    protected boolean activateControl() {
        this.cycleControl(GuiScreen.isShiftKeyDown());

        return true;
    }

    private Component displayValue() {
        Component value = Component.from(this.option.getElementName(this.option.getPendingValue()));

        return this.option.isEnabled() ? value : this.formatDisabledControlValue(value);
    }

    @Override
    protected Component narrationValue() {
        return !this.option.shouldHideControl() ? Component.from(this.option.getElementName(this.option.getPendingValue())) : null;
    }

    @Override
    protected void updateControlNarration(NarrationElementOutput builder) {
        if (!this.option.isEnabled()) {
            builder.add(NarratedElementType.HINT, Component.translatable("rso.narration.option_unavailable"));
            return;
        }

        if (this.option.shouldHideControl()) {
            return;
        }

        Component nextValue = Component.from(this.option.getElementName(this.nextValue(false)));
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.focused", nextValue));
        } else if (this.isHovered()) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.hovered", nextValue));
        }
    }

    private void cycleControl(boolean reverse) {
        Object nextValue = this.nextValue(reverse);
        if (java.util.Objects.equals(nextValue, this.option.getPendingValue())) {
            return;
        }

        this.option.modifyValue(nextValue);
        this.playClickSound();
    }

    private Object nextValue(boolean reverse) {
        Object[] values = this.option.getAllowedValues();
        Object currentValue = this.option.getPendingValue();
        int valueIndex = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) {
                valueIndex = i;
                break;
            }
        }

        for (int i = 0; i < values.length; i++) {
            valueIndex = reverse
                    ? (valueIndex + values.length - 1) % values.length
                    : (valueIndex + 1) % values.length;
            Object nextValue = values[valueIndex];

            if (this.option.isValueAllowed(nextValue)) {
                return nextValue;
            }
        }

        return currentValue;
    }
}
