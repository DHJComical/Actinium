package me.decce.gnetum.compat.scalingguis;

import me.decce.gnetum.MutableScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import spazley.scalingguis.handlers.ConfigHandler;

public class ScalingGuisCompat {
    public static boolean modInstalled = Loader.isModLoaded("scalingguis");
    private static int oldScale;

    public static int getHudScale() {
        return ConfigHandler.getHudScale();
    }

    public static int getMinecraftScale() {
        return oldScale;
    }

    public static void switchToHudScale(MutableScaledResolution res) {
        var mc = Minecraft.getMinecraft();
        oldScale = mc.gameSettings.guiScale;
        mc.gameSettings.guiScale = getHudScale();
        mc.entityRenderer.setupOverlayRendering();
        res.update();
    }

    public static void restoreGameScale(MutableScaledResolution res) {
        var mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = getMinecraftScale();
        mc.entityRenderer.setupOverlayRendering();
        res.update();
    }
}
