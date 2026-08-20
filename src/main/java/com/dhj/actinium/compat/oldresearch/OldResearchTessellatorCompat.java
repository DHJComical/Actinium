package com.dhj.actinium.compat.oldresearch;

import com.gtnewhorizons.angelica.glsm.ITessellatorData;
import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;

/**
 * Routes Old Research's legacy client-array tessellator through Actinium's streaming renderer.
 *
 * <p>Old Research keeps a private copy of the 1.12 tessellator. Its original draw method exposes
 * client-side vertex pointers, which Actinium must upload to a temporary VBO before every draw.
 * The streaming drawer repacks the same legacy vertex layout once and renders it through the
 * persistent or orphan VBO path.</p>
 */
public final class OldResearchTessellatorCompat {
    private OldResearchTessellatorCompat() {
    }

    /** Draws one Old Research tessellator batch with Actinium's streaming path. */
    public static int draw(ITessellatorData tessellator) {
        return TessellatorStreamingDrawer.draw(tessellator);
    }
}
