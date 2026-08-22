package com.dhj.actinium.compat.blockrender;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.List;

/**
 * Serializes {@link IBakedModel#getQuads} calls per block instance.
 *
 * <p>Some modded blocks lazily fill plain {@link java.util.HashMap} caches inside
 * {@code getQuads} (e.g. Extra Utilities 2's {@code XUBlockStatic$3}), which is not
 * thread-safe. Vanilla's chunk builder is single-threaded, but Actinium builds chunk meshes
 * on multiple worker threads, so two workers can enter {@code computeIfAbsent} on the same
 * map concurrently and trigger a {@code ConcurrentModificationException}.</p>
 *
 * <p>Actinium's own block-render path is thread-safe ({@code ChunkModelBuilder} writes
 * vertices directly and touches no main-thread GL objects), so the only non-thread-safe
 * surface is the mod's lazy cache. Serializing {@code getQuads} on the block instance makes
 * every renderer safe without special-casing any mod. Vanilla {@code getQuads} is a pure
 * read, so the lock is uncontended in the common case.</p>
 *
 * <p>Both the root {@code com.dhj.actinium.render.terrain.compile.pipeline.VintageBlockRenderer}
 * and the compat-bridge {@code org.taumc.celeritas...VintageBlockRenderer} must funnel their
 * {@code getQuads} calls through this class so the same lock covers every worker path.</p>
 */
public final class ModdedBlockRenderCompat {
    private ModdedBlockRenderCompat() {
    }

    /**
     * Serializes a baked-model query for the given block instance.
     */
    public static List<BakedQuad> getQuads(Block block, IBakedModel model, IBlockState state,
                                           EnumFacing side, long rand) {
        synchronized (block) {
            return model.getQuads(state, side, rand);
        }
    }

    /**
     * Serializes a vanilla {@link BlockRendererDispatcher#renderBlock} call, which internally
     * queries the baked model's {@code getQuads}.
     */
    public static void renderBlock(BlockRendererDispatcher dispatcher, IBlockState state,
                                   BlockPos pos, IBlockAccess access, BufferBuilder buffer) {
        synchronized (state.getBlock()) {
            dispatcher.renderBlock(state, pos, access, buffer);
        }
    }

    /**
     * Serializes a {@link Block#canRenderInLayer} call on the given block instance.
     */
    public static boolean canRenderInLayer(Block block, IBlockState state, BlockRenderLayer layer) {
        synchronized (block) {
            return block.canRenderInLayer(state, layer);
        }
    }

    /**
     * Provides the deterministic locking seam used by the concurrency regression test.
     */
    static void runWithBlockLock(Object lock, Runnable operation) {
        synchronized (lock) {
            operation.run();
        }
    }
}
