package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.GlStateManager;

/** Shift-hover reset affordance shared by stateful option rows. */
public final class ResetButton {
    private static final ResourceLocation ICON = new ResourceLocation("sodium", "textures/gui/reset_button.png");
    private static final int SIZE = 10;
    private static final int COLOR = 0xFFFF8C30;

    private ResetButton() {
    }

    public static boolean isActive(boolean changed, boolean hovered) {
        return isActive(changed, hovered, GuiScreenShift.isHeld());
    }

    public static boolean isActive(boolean changed, boolean hovered, boolean shiftHeld) {
        return changed && hovered && shiftHeld;
    }

    public static void render(GuiRect bounds) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(ICON);
        GlStateManager.color(1.0F, 0.55F, 0.19F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(bounds.right() - SIZE - 5,
                bounds.y() + (bounds.height() - SIZE) / 2, 0, 0, SIZE, SIZE, SIZE, SIZE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static final class GuiScreenShift {
        private static boolean isHeld() {
            return GuiScreen.isShiftKeyDown();
        }
    }
}
