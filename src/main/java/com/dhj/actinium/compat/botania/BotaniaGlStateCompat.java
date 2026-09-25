package com.dhj.actinium.compat.botania;

import com.gtnewhorizons.angelica.glsm.GLStateManager;

/**
 * Restores GL_TEXTURE_2D enablement after Botania's RenderWorldLastEvent handlers run.
 *
 * <p>Botania's {@code BlockHighlightRenderHandler} and {@code BoundTileRenderer} switch
 * {@code GL_TEXTURE_2D} off to draw wireframe overlays, pairing each
 * {@code disableTexture} with an {@code enableTexture}. Under the GLSM state cache the
 * attrib-stack interaction can leave the active texture unit disabled when the handler
 * returns, so the held-item pass that follows (RenderWorldLastEvent fires just before
 * hand rendering) samples no texture and renders flat white. Item rendering always
 * samples the block atlas, so re-enable the active texture unit after the handler.</p>
 */
public final class BotaniaGlStateCompat {

    private BotaniaGlStateCompat() {
    }

    /** Re-enables GL_TEXTURE_2D on the active texture unit so item rendering samples the atlas. */
    public static void restoreTextureEnablement() {
        GLStateManager.enableTexture();
    }
}
