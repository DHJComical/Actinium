package me.flashyreese.mods.reeses_sodium_options.client.gui.theme;

import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class IconRenderer {
    private static final int FALLBACK_SIZE = 16;

    public static int renderIconWithSpacing(GuiGraphicsExtractor guiGraphics, ResourceLocation icon, int color, boolean monochrome, int x, int y, int height, int spacing) {
        int iconSize = height - spacing * 2;
        int[] dimensions = readTextureDimensions(icon);
        int textureWidth = dimensions[0];
        int textureHeight = dimensions[1];
        int iconX = x + spacing;
        int iconY = y + height / 2 - iconSize / 2;

        if (monochrome) {
            guiGraphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, textureWidth, textureHeight, color);
        } else {
            guiGraphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, textureWidth, textureHeight);
        }

        return spacing * 2 + iconSize;
    }

    /**
     * Reads the icon texture dimensions through GL11. The 1.12.2 texture
     * object API does not expose size directly, so the bound texture level
     * is queried instead.
     */
    private static int[] readTextureDimensions(ResourceLocation icon) {
        Minecraft mc = Minecraft.getMinecraft();
        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            mc.getTextureManager().bindTexture(icon);
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            return new int[]{Math.max(1, width), Math.max(1, height)};
        } catch (RuntimeException e) {
            return new int[]{FALLBACK_SIZE, FALLBACK_SIZE};
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousBinding);
        }
    }
}
