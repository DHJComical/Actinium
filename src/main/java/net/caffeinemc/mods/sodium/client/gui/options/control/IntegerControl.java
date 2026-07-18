package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/** Stepped slider control whose click and arrow navigation always emit validator-approved values. */
public final class IntegerControl extends OptionControl<IntegerOption> {
    public IntegerControl(IntegerOption option, ColorTheme theme, FontRenderer font) {
        super(option, theme, font);
    }

    @Override
    protected int controlWidth() {
        return 90;
    }

    @Override
    protected void renderControl(GuiRect control, boolean enabled, int mouseX, int mouseY) {
        SteppedValidator range = this.option.getSteppedValidator();
        int trackY = control.y() + control.height() / 2;
        boolean drawSlider = enabled;
        double progress = (double) (this.option.getPendingValue() - range.min())
                / Math.max(1, range.max() - range.min());
        if (drawSlider) {
            int knobX = control.x() + (int) Math.round(progress * Math.max(0, control.width() - 4));
            Gui.drawRect(control.x(), trackY, control.right(), trackY + 1, this.theme.themeHighlight());
            Gui.drawRect(knobX, control.y(), knobX + 4, control.bottom(), Colors.FOREGROUND);
        }

        String value = this.option.getValueFormatter() == null ? Integer.toString(this.option.getPendingValue())
                : this.option.formatValue(this.option.getPendingValue()).getUnformattedText();
        value = this.font.trimStringToWidth(value, control.width());
        int valueX = drawSlider ? control.x() - this.font.getStringWidth(value) - 6
                : control.right() - this.font.getStringWidth(value);
        this.font.drawString(value, valueX,
                control.y() + (control.height() - this.font.FONT_HEIGHT) / 2,
                enabled ? Colors.FOREGROUND : Colors.FOREGROUND_DISABLED);
    }

    @Override
    protected boolean onMouseClicked(GuiRect control, int mouseX, int mouseY) {
        if (!control.contains(mouseX, mouseY)) {
            return false;
        }
        SteppedValidator range = this.option.getSteppedValidator();
        this.option.modifyValue(ControlMath.sliderValue(range.min(), range.max(), range.step(),
                mouseX - control.x(), control.width()));
        return true;
    }

    @Override
    protected boolean onMouseDragged(GuiRect control, int mouseX, int mouseY) {
        SteppedValidator range = this.option.getSteppedValidator();
        this.option.modifyValue(ControlMath.sliderValue(range.min(), range.max(), range.step(),
                mouseX - control.x(), control.width()));
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode) {
        int direction = keyCode == 203 || keyCode == 200 ? -1 : keyCode == 205 || keyCode == 208 ? 1 : 0;
        if (direction == 0) {
            return false;
        }
        SteppedValidator range = this.option.getSteppedValidator();
        int value = Math.max(range.min(), Math.min(range.max(),
                this.option.getPendingValue() + direction * range.step()));
        this.option.modifyValue(value);
        return true;
    }
}
