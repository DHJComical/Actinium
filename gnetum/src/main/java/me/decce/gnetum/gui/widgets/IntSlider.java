package me.decce.gnetum.gui.widgets;

import me.decce.gnetum.gui.GuiConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.config.HoverChecker;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IntSlider extends GuiSlider implements ITooltipWidget {
    private final HoverChecker hoverChecker = new HoverChecker(this, 0);
    private List<String> tooltip = Collections.emptyList();
    private final Supplier<String> text;
    private final Supplier<String> specialText;
    private final Consumer<Integer> setter;
    private final Predicate<Integer> special;
    private final int step;

    public IntSlider(int id, int x, int y, int width, int height, Supplier<String> text, int minValue, int maxValue, int currentValue, boolean drawString, Consumer<Integer> setter) {
        this(id, x, y, width, height, text, minValue, maxValue, currentValue, 1, drawString, setter);
    }

    public IntSlider(int id, int x, int y, int width, int height, Supplier<String> text, int minValue, int maxValue, int currentValue, int step, boolean drawString, Consumer<Integer> setter) {
        this(id, x, y, width, height, text, minValue, maxValue, currentValue, step, drawString, setter, null, null);
    }

    public IntSlider(int id, int x, int y, int width, int height, Supplier<String> text, int minValue, int maxValue, int currentValue, int step, boolean drawString, Consumer<Integer> setter, Predicate<Integer> special, Supplier<String> specialText) {
        super(id, x, y, width, height, "", "", minValue, maxValue, currentValue, false, drawString);
        this.step = step;
        this.setter = setter;
        this.text = text;
        this.special = special;
        this.specialText = specialText;
        super.showDecimal = false;
        this.updateSlider();
    }

    public void setTooltip(String tooltip) {
        this.tooltip = TooltipHelper.createTooltip(tooltip);
    }

    @Override
    public void updateSlider() {
        super.updateSlider();
        if (this.special == null || !special.test(this.getValueInt())) {
            if (this.text != null) {
                super.displayString = String.format(text.get(), this.getValueInt());
            }
        } else {
            super.displayString = String.format(specialText.get(), this.getValueInt());
        }
        this.setter.accept(this.getValueInt());
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {
        super.drawButton(mc, mouseX, mouseY, partial);
    }

    @Override
    public int getValueInt() {
        return step * Math.round(super.getValueInt() * 1.0f / step);
    }

    @Override
    public void drawTooltip(int mouseX, int mouseY) {
        if (hoverChecker.checkHover(mouseX, mouseY)) {
            var screen = Minecraft.getMinecraft().currentScreen;
            if (screen != null) {
                GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, screen.width, screen.height, GuiConstants.MAX_TOOLTIP_WIDTH, Minecraft.getMinecraft().fontRenderer);
                RenderHelper.disableStandardItemLighting();
            }
        }
    }
}
