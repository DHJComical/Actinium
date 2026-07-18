package com.dhj.actinium.compat.modernui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import com.dhj.actinium.runtime.ActiniumRuntime;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Ugly hack to get around Modern UI overwriting calculateScaleFactor and not conforming to vanilla standards
 * by returning the max size when scale = 0.
 */
public class MuiGuiScaleHook {
    /**
     * Keep the manual GUI scale range useful on low-resolution displays. Vanilla
     * may clamp the effective scale, but the setting remains valid and can be
     * applied immediately when the window is resized.
     */
    static final int MIN_MANUAL_GUI_SCALE = 5;

    private static final Method calcGuiScalesMethod;

    static {
        calcGuiScalesMethod = Stream.of(
                "icyllis.modernui.forge.MForgeCompat",
                "icyllis.modernui.forge.MuiForgeApi",
                "icyllis.modernui.mc.forge.MuiForgeApi",
                "icyllis.modernui.mc.MuiModApi"
        ).flatMap(clzName -> {
            try {
                return Stream.of(Class.forName(clzName));
            } catch (Throwable e) {
                return Stream.of();
            }
        }).flatMap(clz -> {
            try {
                Method m = clz.getDeclaredMethod("calcGuiScales");
                m.setAccessible(true);
                return Stream.of(m);
            } catch (Throwable e) {
                return Stream.of();
            }
        }).findFirst().orElse(null);
        if (calcGuiScalesMethod != null)
            ActiniumRuntime.logger().info("Found ModernUI GUI scale hook");
    }

    public static int getMaxGuiScale() {
        boolean forceUnicode = Minecraft.getMinecraft().gameSettings.forceUnicodeFont;
        int vanillaMax = calculateScale(0, forceUnicode);

        if (calcGuiScalesMethod != null) {
            try {
                int modernUiMax = (int) calcGuiScalesMethod.invoke(null) & 0xf;
                return resolveMaximum(vanillaMax, modernUiMax);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return resolveMaximum(vanillaMax, 0);
    }

    static int resolveMaximum(int vanillaMax, int externalMax) {
        return Math.max(MIN_MANUAL_GUI_SCALE, Math.max(vanillaMax, externalMax));
    }

    public static int calculateScale(int guiScale, boolean forceUnicode) {
        int i;
        for (i = 1; i != guiScale && i < Minecraft.getMinecraft().getFramebuffer().framebufferWidth && i < Minecraft.getMinecraft().getFramebuffer().framebufferHeight && Minecraft.getMinecraft().getFramebuffer().framebufferWidth / (i + 1) >= 320 && Minecraft.getMinecraft().getFramebuffer().framebufferHeight / (i + 1) >= 240; ++i) {
        }

        if (forceUnicode && i % 2 != 0) {
            ++i;
        }

        return i;
    }

}
