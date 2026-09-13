package com.dhj.actinium.compat.littletiles;

import com.creativemd.creativecore.client.rendering.model.BufferBuilderUtils;
import com.creativemd.littletiles.client.render.cache.IRenderDataCache;
import com.creativemd.littletiles.client.render.cache.LayeredRenderBufferCache;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Compatibility for {@code LittleTiles} (mod id {@code littletiles}).
 *
 * <p>LittleTiles does not render its tiles through the block's baked model. Each
 * {@link TileEntityLittleTiles} owns a {@link LayeredRenderBufferCache}: raw vertex bytes in
 * vanilla {@code DefaultVertexFormats.BLOCK} layout, relative to the tile entity's own 16^3
 * section, built asynchronously by LittleTiles' {@code RenderingThread}. Vanilla Minecraft only
 * appends those bytes to the chunk upload buffer inside {@code ChunkRenderDispatcher.uploadChunk}
 * (an ASM hook installed by LittleTiles), a method Actinium's chunk pipeline never calls, so every
 * non-full-block LittleTiles block is invisible (issue #133).
 *
 * <p>The vanilla {@code ViewFrustum} still exists under Actinium (created with zero render
 * distance, so it only holds placeholder render chunks), which is enough for LittleTiles' own
 * cache-build queue to function. What is missing is (a) consuming the caches during section
 * meshing and (b) translating "cache build finished" into a celeritas section rebuild, since the
 * vanilla {@code RenderChunk.setNeedsUpdate} flag LittleTiles flips is never polled.
 */
public final class LittleTilesCompat {
    private LittleTilesCompat() {
    }

    /**
     * Appends the cached vertex data of every loaded {@link TileEntityLittleTiles} in the section
     * to the vanilla-format per-layer buffers of the in-flight section build. Called from the
     * meshing task right before {@code convertVanillaDataToCeleritasData}, so the appended quads
     * are converted to celeritas vertex format like any other vanilla-fallback geometry.
     *
     * <p>{@code blockEntities} are the TESR-bearing tile entities already collected by the
     * meshing task; LittleTiles registers its TESR exactly for the tile entity variants that carry
     * renderable tiles, so the lists double as the "has visible content" filter.
     */
    public static void appendSectionGeometry(VintageChunkBuildContext buildContext,
                                             List<TileEntity> globalBlockEntities,
                                             List<TileEntity> culledBlockEntities) {
        appendForLayer(buildContext, globalBlockEntities);
        appendForLayer(buildContext, culledBlockEntities);
    }

    private static void appendForLayer(VintageChunkBuildContext buildContext, List<TileEntity> blockEntities) {
        for (TileEntity blockEntity : blockEntities) {
            if (!(blockEntity instanceof TileEntityLittleTiles te) || !te.hasLoaded()) {
                continue;
            }
            // Picks up light/neighbour dirty flags and re-queues the cache build, mirroring what
            // the vanilla uploadChunk hook does on every chunk upload. The chunk argument is
            // unused by LittleTiles beyond its signature.
            te.updateQuadCache(null);
            synchronized (te.render) {
                LayeredRenderBufferCache cache = te.render.getBufferCache();
                for (BlockRenderLayer layer : VintageChunkBuildContext.LAYERS) {
                    IRenderDataCache data = cache.get(layer.ordinal());
                    if (data == null) {
                        continue;
                    }
                    ByteBuffer source = data.byteBuffer();
                    if (source == null || data.vertexCount() == 0) {
                        continue;
                    }
                    BufferBuilder buffer = buildContext.getBufferForLayer(layer);
                    // Same append mechanics LittleTiles itself uses on the vanilla upload buffer:
                    // grow first, then raw-copy the bytes and bump the vertex count.
                    BufferBuilderUtils.growBufferSmall(buffer, data.length() + buffer.getVertexFormat().getSize());
                    BufferBuilderUtils.addBuffer(buffer, source.duplicate(), data.length(), data.vertexCount());
                }
            }
        }
    }

    /**
     * Handles a finished asynchronous LittleTiles cache build (fired from
     * {@code TileEntityRenderManager.finishBuildingCache} on LittleTiles' rendering thread).
     * Vanilla would mark the render chunk for an update through {@code RenderChunk.setNeedsUpdate},
     * a flag the celeritas pipeline never polls, so schedule the section rebuild directly.
     */
    public static void onCacheBuildFinished(TileEntityLittleTiles te) {
        if (te.getWorld() != Minecraft.getMinecraft().world) {
            // SubWorld tile entities belong to animated structures, which render through
            // LittleTiles' own animation path rather than section geometry.
            return;
        }
        ActiniumWorldRenderer renderer = ActiniumWorldRenderer.instanceNullable();
        if (renderer == null) {
            return;
        }
        BlockPos pos = te.getPos();
        renderer.getRenderSectionManager().scheduleRebuild(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4, false);
    }
}
