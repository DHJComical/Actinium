package com.gtnewhorizons.angelica.client.font;

import net.minecraft.client.gui.FontRenderer;

public final class FontStrategist {
    private FontStrategist() {}

    public static FontProvider getFontProvider(BatchingFontRenderer renderer, char chr, boolean customFontEnabled, boolean forceUnicode) {
        if (renderer.isSGA && FontProviderMC.get(true).isGlyphAvailable(chr)) {
            return FontProviderMC.get(true);
        }
        if (renderer.bookMode) {
            return FontProviderUnicode.get();
        }
        if (!forceUnicode && FontProviderMC.get(false).isGlyphAvailable(chr)) {
            return FontProviderMC.get(false);
        }
        return FontProviderUnicode.get();
    }

    public static boolean isSplashFontRendererActive(FontRenderer fontRenderer) {
        boolean active = false;
        try {
            Class<?> splashClass = Class.forName("cpw.mods.fml.client.SplashProgress$SplashFontRenderer");
            active = splashClass.isInstance(fontRenderer);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class<?> forgeSplashClass = Class.forName("net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer");
            active = active || forgeSplashClass.isInstance(fontRenderer);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class<?> customSplashClass = Class.forName("gkappa.modernsplash.CustomSplash$SplashFontRenderer");
            active = active || customSplashClass.isInstance(fontRenderer);
        } catch (ClassNotFoundException ignored) {
        }
        return active;
    }
}
