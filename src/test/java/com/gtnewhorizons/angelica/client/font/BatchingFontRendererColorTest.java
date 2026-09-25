package com.gtnewhorizons.angelica.client.font;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchingFontRendererColorTest {
    @Test
    void whiteComponentsProduceOpaqueWhite() {
        assertEquals(0xFFFFFFFF, BatchingFontRenderer.floatsToArgb(1.0F, 1.0F, 1.0F, 1.0F));
    }

    @Test
    void zeroComponentsProduceOpaqueBlack() {
        assertEquals(0xFF000000, BatchingFontRenderer.floatsToArgb(0.0F, 0.0F, 0.0F, 1.0F));
    }

    @Test
    void alphaIsPreserved() {
        assertEquals(0x80FFFFFF, BatchingFontRenderer.floatsToArgb(1.0F, 1.0F, 1.0F, 0.5F));
    }

    @Test
    void typicalNightModeFontColor() {
        // Modern Splash fontDark 0xF3F5F8 normalized
        assertEquals(0xFFF3F5F8,
            BatchingFontRenderer.floatsToArgb(243 / 255.0F, 245 / 255.0F, 248 / 255.0F, 1.0F));
    }

    @Test
    void channelOrderIsArgbNotRgba() {
        // A dark-mode background red 0xEF323D must not be confused with a green-dominant value
        assertEquals(0xFFEF323D,
            BatchingFontRenderer.floatsToArgb(0xEF / 255.0F, 0x32 / 255.0F, 0x3D / 255.0F, 1.0F));
    }
}
