package me.decce.gnetum.hud;

import me.decce.gnetum.mixins.early.GuiIngameAccessor;
import me.decce.gnetum.mixins.early.GuiIngameForgeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class SharedValues {
    public static final Minecraft mc = Minecraft.getMinecraft();
    public static float partialTicks;

    public static void defaultBlendFunc() {
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static GuiIngameForgeAccessor getAccessor() {
        return (GuiIngameForgeAccessor)mc.ingameGUI;
    }

    public static GuiIngameAccessor getAccessorVanilla() {
        return (GuiIngameAccessor)mc.ingameGUI;
    }
}
