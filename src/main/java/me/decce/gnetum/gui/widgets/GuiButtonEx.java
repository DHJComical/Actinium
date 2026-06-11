package me.decce.gnetum.gui.widgets;

import me.decce.gnetum.gui.GuiConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.config.HoverChecker;

import java.util.Collections;
import java.util.List;

public class GuiButtonEx extends GuiButtonExt implements ITooltipWidget {
    private final Runnable onClick;
    private final HoverChecker hoverChecker = new HoverChecker(this, 0);
    private List<String> tooltip = Collections.emptyList();

    public GuiButtonEx(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this(buttonId, x, y, widthIn, heightIn, buttonText, () -> {});
    }

    public GuiButtonEx(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, Runnable onClick) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.onClick = onClick;
    }

    public void onPress() {
        if (this.onClick != null) {
            this.onClick.run();
        }
    }

    public void setTooltip(String tooltip) {
        this.tooltip = TooltipHelper.createTooltip(tooltip);
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
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
