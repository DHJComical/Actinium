package me.decce.gnetum.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class MultilineStringWidget extends GuiButton {
    private final int maxWidth;

    public MultilineStringWidget(int buttonId, int x, int y, int maxWidth, String text) {
        super(buttonId, x, y, 0, 0, text);
        this.maxWidth = maxWidth;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        FontRenderer fontrenderer = mc.fontRenderer;
        fontrenderer.drawSplitString(displayString, x, y, maxWidth, 0xFFFFFF);
    }
}
