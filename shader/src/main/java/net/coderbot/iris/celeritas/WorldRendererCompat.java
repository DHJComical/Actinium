package net.coderbot.iris.celeritas;

import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;

import java.util.Collection;

/**
 * World-terrain rendering surface needed by the shadow pass.
 *
 * <p>Implemented by the host mod's world renderer and registered via
 * {@link WorldRendererCompatBridge}, so the shader module does not depend on
 * the host's rendering implementation.
 */
public interface WorldRendererCompat {

    int getVisibleChunkCount();

    String getChunksDebugString();

    void setCurrentViewport(Viewport viewport);

    void drawChunkLayer(BlockRenderLayer renderLayer, double x, double y, double z);

    void drawChunkLayersDeduplicated(Collection<BlockRenderLayer> renderLayers, double x, double y, double z);

    /** Marks the terrain section graph dirty for re-culling (used by the shadow pass). */
    void markSectionGraphDirty();

    void setupTerrain(Viewport viewport, SimpleWorldRenderer.CameraState cameraState, int frame, boolean spectator, boolean updateChunksImmediately);
}
