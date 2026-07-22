package org.taumc.celeritas.compat;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.light.LightDataCache;
import org.taumc.celeritas.impl.render.terrain.compile.pipeline.ActiniumVintageBlockRenderer;
import org.taumc.celeritas.impl.render.terrain.compile.pipeline.VintageBlockRenderer;

/**
 * Creates the live compatibility renderer while keeping its legacy name outside Actinium code.
 */
public final class LegacyRendererFactory {
    private LegacyRendererFactory() {
    }

    /**
     * Returns a renderer whose declared methods retain the Celeritas 2.4.0 Mixin targets.
     */
    public static ActiniumVintageBlockRenderer create(VintageChunkBuildContext context, LightDataCache cache) {
        return new VintageBlockRenderer(context, cache);
    }
}
