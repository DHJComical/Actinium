package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Allowed-value cycling control without enum discovery or reflection. */
public final class EnumControl<E extends Enum<E>> extends OptionControl<EnumOption<E>> {
    private final Config config;

    public EnumControl(EnumOption<E> option, ColorTheme theme, FontRenderer font, Config config) {
        super(option, theme, font);
        this.config = config;
    }

    @Override
    protected int controlWidth() {
        return 74;
    }

    @Override
    protected void renderControl(GuiRect control, boolean enabled, int mouseX, int mouseY) {
        String value = this.option.getElementName(this.option.getPendingValue()).getUnformattedText();
        value = this.font.trimStringToWidth(value, Math.max(1, control.width() - 8));
        this.font.drawString(value, control.right() - this.font.getStringWidth(value),
                control.y() + (control.height() - this.font.FONT_HEIGHT) / 2,
                enabled ? Colors.FOREGROUND : Colors.FOREGROUND_DISABLED);
    }

    @Override
    protected boolean onMouseClicked(GuiRect control, int mouseX, int mouseY) {
        if (!control.contains(mouseX, mouseY)) {
            return false;
        }
        this.cycle(1);
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode) {
        if (keyCode == 203 || keyCode == 200) {
            this.cycle(-1);
            return true;
        }
        if (keyCode == 205 || keyCode == 208 || keyCode == 28 || keyCode == 57) {
            this.cycle(1);
            return true;
        }
        return false;
    }

    private void cycle(int direction) {
        List<E> values = new ArrayList<>(this.option.getAllowedValues().get(this.config));
        values.sort(Comparator.comparingInt(Enum::ordinal));
        if (values.isEmpty()) {
            throw new IllegalStateException("Enum option has no allowed values: " + this.option.getId());
        }
        int current = values.indexOf(this.option.getPendingValue());
        if (current < 0) {
            throw new IllegalStateException("Enum option pending value is not allowed: " + this.option.getId());
        }
        this.option.modifyValue(values.get(ControlMath.cycleIndex(values.size(), current, direction)));
    }
}
