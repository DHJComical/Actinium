package com.dhj.actinium.compat.neofontrender;

import com.gtnewhorizon.gtnhlib.compat.Mods;
import neofontrender.api.NeoFontRenderApi;
import neofontrender.api.color.TextColorPaletteProvider;

/**
 * Runtime bridge to Neo Font Render when it is installed.
 */
public final class NeoFontRenderCompat {
    private NeoFontRenderCompat() {
    }

    public static boolean isLoaded() {
        return Mods.NEOFONTRENDER;
    }

    /**
     * Registers Actinium as the preferred legacy text color palette source so NFR follows the
     * current vanilla FontRenderer color codes exposed by Actinium's rendering pipeline.
     */
    public static void initialize() {
        if (!isLoaded()) {
            return;
        }

        NeoFontRenderApi.registerTextColorPaletteProvider(new TextColorPaletteProvider() {
            @Override
            public String id() {
                return "actinium:runtime";
            }

            @Override
            public String displayName() {
                return "Actinium";
            }

            @Override
            public int priority() {
                return 200;
            }

            @Override
            public int[] colorCodes(int[] runtimeColorCodes) {
                return runtimeColorCodes;
            }
        });
    }
}
