package com.dhj.actinium.compat.hbm;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HbmRenderStateCompatTest {
    @Test
    void mapsHbmDepthScopeToGlsmDepthAndShadeState() {
        assertEquals(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT,
            HbmRenderStateCompat.toGlMask(0x00100));
    }

    @Test
    void mapsHbmGuiScopeToEquivalentGlsmGroups() {
        assertEquals(GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT,
            HbmRenderStateCompat.toGlMask(0x46000));
    }

    @Test
    void mapsHbmColorScopeToCurrentColorState() {
        assertEquals(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT,
            HbmRenderStateCompat.toGlMask(0x04000));
    }

    @Test
    void expandsHbmAllBitsBeforeMapping() {
        assertEquals(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_TEXTURE_BIT
                | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT
                | GL11.GL_FOG_BIT,
            HbmRenderStateCompat.toGlMask(0xFFFFF));
    }

    @Test
    void rejectsUnknownHbmAttributeBits() {
        assertThrows(IllegalArgumentException.class,
            () -> HbmRenderStateCompat.toGlMask(0x00001));
    }

    @Test
    void splitsCombinedLightUsingVanillaPacking() {
        int combinedLight = 0xABCD1234;

        assertEquals(0x1234, HbmRenderStateCompat.blockLight(combinedLight));
        assertEquals(0xABCD, HbmRenderStateCompat.skyLight(combinedLight));
    }
}
