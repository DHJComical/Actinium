package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/** Tick-box control with mouse, Space, Enter, and arrow-key support. */
public final class BooleanControl extends OptionControl<BooleanOption> {
    public BooleanControl(BooleanOption option, ColorTheme theme, FontRenderer font) {
        super(option, theme, font);
    }

    @Override
    protected int controlWidth() {
        return 30;
    }

    @Override
    protected void renderControl(GuiRect control, boolean enabled, int mouseX, int mouseY) {
        int size = 10;
        int x = control.right() - size;
        int y = control.y() + (control.height() - size) / 2;
        int color = enabled ? (this.option.getPendingValue() ? this.theme.theme() : Colors.FOREGROUND)
                : Colors.FOREGROUND_DISABLED;
        if (this.option.getPendingValue()) Gui.drawRect(x + 2, y + 2, x + size - 2, y + size - 2, color);
        Gui.drawRect(x, y, x + (enabled ? size : 3), y + 1, color);
        Gui.drawRect(x, y, x + 1, y + (enabled ? size : 3), color);
        Gui.drawRect(x + size - (enabled ? size : 3), y + size - 1, x + size, y + size, color);
        Gui.drawRect(x + size - 1, y + size - (enabled ? size : 3), x + size, y + size, color);
        Gui.drawRect(x + size - 3, y, x + size, y + 1, color);
        Gui.drawRect(x + size - 1, y, x + size, y + 3, color);
        Gui.drawRect(x, y + size - 1, x + 3, y + size, color);
        Gui.drawRect(x, y + size - 3, x + 1, y + size, color);
    }

    @Override
    protected boolean onMouseClicked(GuiRect control, int mouseX, int mouseY) {
        if (!control.contains(mouseX, mouseY)) {
            return false;
        }
        this.option.modifyValue(!this.option.getPendingValue());
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode) {
        if (keyCode == 28 || keyCode == 57 || keyCode == 203 || keyCode == 205) {
            this.option.modifyValue(!this.option.getPendingValue());
            return true;
        }
        return false;
    }
}
