package org.taumc.celeritas.impl.render.terrain.compile.pipeline;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.light.LightDataCache;
import com.dhj.actinium.render.terrain.compile.pipeline.VintageBlockRenderer;

/**
 * Keeps the imported Actinium renderer type separate from the legacy binary name.
 */
public class ActiniumVintageBlockRenderer extends VintageBlockRenderer {
    ActiniumVintageBlockRenderer(VintageChunkBuildContext context, LightDataCache cache) {
        super(context, cache);
    }
}
