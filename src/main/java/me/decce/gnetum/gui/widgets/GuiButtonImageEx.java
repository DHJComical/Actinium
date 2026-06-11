package me.decce.gnetum.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class GuiButtonImageEx extends GuiButtonEx {
    private final int textureWidth;
    private final int textureHeight;
    private final int offsetX;
    private final int offsetY;
    private final ResourceLocation icon;

    public GuiButtonImageEx(int buttonId, int x, int y, int widthIn, int heightIn, int textureWidth, int textureHeight, int offsetX, int offsetY, ResourceLocation icon, Runnable onClick) {
        super(buttonId, x, y, widthIn, heightIn, "", onClick);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.icon = icon;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);

        if (icon != null) {
            mc.getTextureManager().bindTexture(icon);
            drawModalRectWithCustomSizedTexture(x + offsetX, y + offsetY, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        }
    }
}
