package me.decce.gnetum.compat.quark;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.Loader;
import vazkii.quark.management.feature.ChangeHotbarKeybind;

public class QuarkCompat {
    public static final boolean INSTALLED = Loader.isModLoaded("quark");
    public static float shift;
    public static boolean movingHotbar;

    public static void preShift() {
        if (shift < 0) {
            GlStateManager.translate(0, shift, 0);
        }
    }

    public static void postShift() {
        if (shift < 0) {
            GlStateManager.translate(0, -shift, 0);
        }
    }
}
