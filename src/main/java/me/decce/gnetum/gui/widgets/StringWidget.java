package me.decce.gnetum.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class StringWidget extends GuiButton {
    public StringWidget(int buttonId, int x, int y, String text) {
        super(buttonId, x, y, 0, 0, text);
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        FontRenderer fontrenderer = mc.fontRenderer;
        this.drawCenteredString(fontrenderer, displayString, x, y, 0xFFFFFF);
    }
}
