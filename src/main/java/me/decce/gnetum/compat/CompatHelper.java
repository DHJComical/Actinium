package me.decce.gnetum.compat;

import me.decce.gnetum.compat.valkyrie.ValkyrieCompat;
import net.minecraft.client.Minecraft;

public class CompatHelper {
    public static boolean isVignetteEnabled() {
        return Minecraft.getMinecraft().gameSettings.fancyGraphics;
    }

    public static int getHudOffset() {
        return ValkyrieCompat.modInstalled ? ValkyrieCompat.getOffset() : 0;
    }
}
