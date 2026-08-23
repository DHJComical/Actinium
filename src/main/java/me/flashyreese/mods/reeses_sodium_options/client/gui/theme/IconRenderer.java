package me.flashyreese.mods.reeses_sodium_options.client.gui.theme;

import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import net.minecraft.util.ResourceLocation;

public final class IconRenderer {
    public static int renderIconWithSpacing(GuiGraphicsExtractor guiGraphics, ResourceLocation icon, int color, boolean monochrome, int x, int y, int height, int spacing) {
        int iconSize = height - spacing * 2;
        int iconX = x + spacing;
        int iconY = y + height / 2 - iconSize / 2;

        // The 1.12.2 custom-texture draw maps the UV range 0..1 onto the
        // whole texture when the textureWidth/Height arguments equal the
        // draw size, so the full icon is scaled down to the icon slot.
        if (monochrome) {
            guiGraphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize, color);
        } else {
            guiGraphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        }

        return spacing * 2 + iconSize;
    }
}
