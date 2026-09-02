package com.dhj.actinium.render;

import net.minecraft.tileentity.TileEntityEndPortal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EndPortalRenderPolicyTest {

    @Test
    void worldLessBlockEntityKeepsLegacyVanillaPath() {
        // Synthetic render calls (e.g. BetterPortals' starfield overlay) use a dummy block
        // entity that never joined a world; the replacement renderer must not hijack them.
        assertFalse(EndPortalRenderPolicy.shouldUseReplacementRenderer(new TileEntityEndPortal()));
    }
}
