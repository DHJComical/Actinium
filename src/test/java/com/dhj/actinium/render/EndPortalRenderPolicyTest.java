package com.dhj.actinium.render;

import net.minecraft.tileentity.TileEntityEndPortal;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndPortalRenderPolicyTest {

    @Test
    void worldLessBlockEntityKeepsLegacyVanillaPathWithoutShaderPack() {
        // Synthetic render calls (e.g. BetterPortals' starfield overlay) use a dummy block
        // entity that never joined a world; without a shader pack the replacement renderer
        // must not hijack them, so the legacy blend state hooks keep applying through glsm.
        assertFalse(EndPortalRenderPolicy.shouldUseReplacementRenderer(new TileEntityEndPortal(), false));
    }

    @Test
    void worldLessBlockEntityUsesReplacementUnderShaderPack() {
        // Under a shader pack an Iris gbuffers program owns the draw and glsm defers to it,
        // so the legacy projective texgen path can never execute; synthetic overlay calls go
        // through the shader-compatible replacement renderer instead.
        assertTrue(EndPortalRenderPolicy.shouldUseReplacementRenderer(new TileEntityEndPortal(), true));
    }

    @Test
    void recognizesLegacyFadeHookBlendFactors() {
        assertTrue(EndPortalRenderPolicy.isLegacyFadeHookBlend(
            GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA));
        assertFalse(EndPortalRenderPolicy.isLegacyFadeHookBlend(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA));
        assertFalse(EndPortalRenderPolicy.isLegacyFadeHookBlend(GL11.GL_ONE, GL11.GL_ONE));
    }
}
