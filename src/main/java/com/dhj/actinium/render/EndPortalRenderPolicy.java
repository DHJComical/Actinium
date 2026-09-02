package com.dhj.actinium.render;

import net.minecraft.tileentity.TileEntityEndPortal;

/**
 * Decides whether a TileEntityEndPortalRenderer render call is answered by the core-profile
 * replacement renderer or left on the legacy vanilla path (executed through the glsm
 * fixed-function simulation).
 */
public final class EndPortalRenderPolicy {
    private EndPortalRenderPolicy() {
    }

    /**
     * The dispatcher only renders block entities that belong to a world. A world-less block
     * entity is a synthetic render call issued by another renderer — e.g. BetterPortals draws
     * its starfield overlay through a dummy block entity and relies on the exact legacy blend
     * state hooks (CONSTANT_ALPHA via shouldRenderFace) and projective texgen behavior, which
     * the replacement renderer does not reproduce.
     *
     * @param portal block entity passed to the renderer
     * @return true to use the core-profile replacement renderer; false to keep the legacy path
     */
    public static boolean shouldUseReplacementRenderer(TileEntityEndPortal portal) {
        return portal.getWorld() != null;
    }
}
