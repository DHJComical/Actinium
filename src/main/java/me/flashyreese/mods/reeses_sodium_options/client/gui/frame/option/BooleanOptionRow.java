package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import com.dhj.actinium.gui.rso.compat.NarratedElementType;
import com.dhj.actinium.gui.rso.compat.NarrationElementOutput;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;

import java.util.List;

final class BooleanOptionRow extends AbstractOptionRow {
    private static final int CONTENT_WIDTH = 30;
    private static final int CHECKBOX_SIZE = 10;
    private static final int DISABLED_CORNER_SIZE = 3;
    private static final int DISABLED_COLOR = 0xFFAAAAAA;

    private final RsoOption option;

    BooleanOptionRow(LayoutBounds dim, GuiTheme theme, OptionStateStore optionStateStore, RsoOption option) {
        super(dim, theme, optionStateStore, option);
        this.option = option;
    }

    @Override
    public RsoOption getOption() {
        return this.option;
    }

    @Override
    protected int controlContentWidth() {
        return CONTENT_WIDTH;
    }

    @Override
    public List<ControlGuide> controlGuides() {
        return this.canShowControlGuide() ? List.of(ControlGuide.press(Component.translatable("rso.controller.guide.toggle"))) : List.of();
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.option.shouldHideControl()) {
            return;
        }

        LayoutBounds checkboxDim = this.checkboxDim();
        boolean enabled = this.option.isEnabled();
        boolean checked = (Boolean) this.option.getPendingValue();
        int color = this.checkboxColor(enabled, checked);

        if (checked) {
            this.drawRect(
                    guiGraphics,
                    checkboxDim.x() + 2,
                    checkboxDim.y() + 2,
                    checkboxDim.getLimitX() - 2,
                    checkboxDim.getLimitY() - 2,
                    color
            );
        }

        if (enabled) {
            this.drawBorder(guiGraphics, checkboxDim.x(), checkboxDim.y(), checkboxDim.getLimitX(), checkboxDim.getLimitY(), color);
        } else {
            this.drawDisabledCorners(guiGraphics, checkboxDim, color);
        }

        this.requestPointerCursorIfHovered(guiGraphics);
    }

    @Override
    protected boolean activateControl() {
        if (!this.option.isEnabled() || this.option.shouldHideControl()) {
            return false;
        }

        this.option.modifyValue(!((Boolean) this.option.getPendingValue()));
        this.playClickSound();

        return true;
    }

    @Override
    protected Component narrationValue() {
        return !this.option.shouldHideControl() ? com.dhj.actinium.gui.rso.compat.CommonComponents.optionStatus((Boolean) this.option.getPendingValue()) : null;
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

        boolean checked = (Boolean) this.option.getPendingValue();
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable(checked
                    ? "narration.checkbox.usage.focused.uncheck"
                    : "narration.checkbox.usage.focused.check"));
        } else if (this.isHovered()) {
            builder.add(NarratedElementType.USAGE, Component.translatable(checked
                    ? "narration.checkbox.usage.hovered.uncheck"
                    : "narration.checkbox.usage.hovered.check"));
        }
    }

    private LayoutBounds checkboxDim() {
        int x = this.rightAlignedControlX(CHECKBOX_SIZE);
        int y = this.getDimensions().getCenterY() - CHECKBOX_SIZE / 2;

        return new LayoutBounds(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE);
    }

    private int checkboxColor(boolean enabled, boolean checked) {
        if (!enabled) {
            return DISABLED_COLOR;
        }

        return checked ? this.theme.theme : 0xFFFFFFFF;
    }

    private void drawDisabledCorners(GuiGraphicsExtractor guiGraphics, LayoutBounds dim, int color) {
        int size = DISABLED_CORNER_SIZE;
        this.drawRect(guiGraphics, dim.x(), dim.y(), dim.x() + size, dim.y() + 1, color);
        this.drawRect(guiGraphics, dim.x(), dim.y(), dim.x() + 1, dim.y() + size, color);
        this.drawRect(guiGraphics, dim.getLimitX() - size, dim.y(), dim.getLimitX(), dim.y() + 1, color);
        this.drawRect(guiGraphics, dim.getLimitX() - 1, dim.y(), dim.getLimitX(), dim.y() + size, color);
        this.drawRect(guiGraphics, dim.x(), dim.getLimitY() - 1, dim.x() + size, dim.getLimitY(), color);
        this.drawRect(guiGraphics, dim.x(), dim.getLimitY() - size, dim.x() + 1, dim.getLimitY(), color);
        this.drawRect(guiGraphics, dim.getLimitX() - size, dim.getLimitY() - 1, dim.getLimitX(), dim.getLimitY(), color);
        this.drawRect(guiGraphics, dim.getLimitX() - 1, dim.getLimitY() - size, dim.getLimitX(), dim.getLimitY(), color);
    }
}
