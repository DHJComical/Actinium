package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Non-stateful command control which transfers to an integration-owned screen. */
public final class ExternalButtonControl extends OptionControl<ExternalButtonOption> {
    private final GuiScreen parent;

    public ExternalButtonControl(ExternalButtonOption option, ColorTheme theme, FontRenderer font, GuiScreen parent) {
        super(option, theme, font);
        this.parent = parent;
    }

    @Override
    protected int controlWidth() {
        return 64;
    }

    @Override
    protected void renderControl(GuiRect control, boolean enabled, int mouseX, int mouseY) {
        String label = this.font.trimStringToWidth(I18n.format("sodium.options.open_external_page_button"),
                Math.max(1, control.width() - 8));
        label = enabled ? "\u00a7n" + label : "\u00a77\u00a7m" + label;
        this.font.drawString(label, control.right() - this.font.getStringWidth(label),
                control.y() + (control.height() - this.font.FONT_HEIGHT) / 2,
                enabled ? Colors.FOREGROUND : Colors.FOREGROUND_DISABLED);
        if (enabled) {
            this.font.drawString(">", control.right() - this.font.getStringWidth(">"),
                    control.y() + (control.height() - this.font.FONT_HEIGHT) / 2, this.theme.theme());
        }
    }

    @Override
    protected boolean onMouseClicked(GuiRect control, int mouseX, int mouseY) {
        if (!control.contains(mouseX, mouseY)) {
            return false;
        }
        this.option.getScreenConsumer().accept(this.parent);
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode) {
        if (keyCode != 28 && keyCode != 57) {
            return false;
        }
        this.option.getScreenConsumer().accept(this.parent);
        return true;
    }
}
