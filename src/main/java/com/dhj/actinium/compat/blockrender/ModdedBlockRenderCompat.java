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
import net.minecraftforge.fml.common.Loader;

import java.util.List;
import java.util.function.Supplier;

/**
 * Serializes chunk-builder access to known modded block renderers with non-thread-safe caches.
 *
 * <p>Extra Utilities 2 stores lazy model and render-layer caches on each block instance. AgriCraft
 * stores crop quads in a renderer-wide cache. Both renderers are normally called from vanilla's
 * single-threaded chunk builder, while Actinium calls them from multiple worker threads.</p>
 */
public final class ModdedBlockRenderCompat {
    private static final String EXTRA_UTILITIES_2_MOD_ID = "extrautils2";
    private static final String EXTRA_UTILITIES_2_PACKAGE = "com.rwtema.extrautils2.";
    private static final String AGRICRAFT_MOD_ID = "agricraft";
    private static final String AGRICRAFT_PACKAGE = "com.infinityraider.agricraft.";

    /** AgriCraft's RenderCrop cache is shared by all crop block instances. */
    private static final Object AGRICRAFT_RENDER_LOCK = new Object();

    private ModdedBlockRenderCompat() {
    }

    /**
     * Runs a complete block-render lifecycle under the lock required by the block's renderer.
     */
    public static void renderBlock(Block block, Runnable render) {
        if (isUnsafeBlock(block)) {
            runWithBlockLock(lockFor(block), render);
        } else {
            render.run();
        }
    }

    /**
     * Calls a block-render operation while preserving its return value and exception behavior.
     */
    public static <T> T callWithBlockLock(Block block, Supplier<T> operation) {
        if (isUnsafeBlock(block)) {
            return callWithLock(lockFor(block), operation);
        }
        return operation.get();
    }

    /**
     * Serializes a block layer query for a known unsafe renderer.
     */
    public static boolean canRenderInLayer(Block block, IBlockState state, BlockRenderLayer layer) {
        return callWithBlockLock(block, () -> block.canRenderInLayer(state, layer));
    }

    /**
     * Serializes a baked-model query for a known unsafe renderer.
     */
    public static List<BakedQuad> getQuads(Block block, IBakedModel model, IBlockState state,
                                           EnumFacing side, long rand) {
        return callWithBlockLock(block, () -> model.getQuads(state, side, rand));
    }

    /**
     * Renders through the vanilla dispatcher while holding the block renderer's compatibility lock.
     */
    public static void renderBlock(BlockRendererDispatcher dispatcher, IBlockState state,
                                   BlockPos pos, IBlockAccess access, BufferBuilder buffer) {
        renderBlock(state.getBlock(), () -> dispatcher.renderBlock(state, pos, access, buffer));
    }

    /**
     * Provides the deterministic locking seam used by the concurrency regression test.
     */
    static void runWithBlockLock(Object lock, Runnable operation) {
        synchronized (lock) {
            operation.run();
        }
    }

    static boolean isUnsafeBlockClassName(String name) {
        return name != null && (name.startsWith(EXTRA_UTILITIES_2_PACKAGE)
                || name.startsWith(AGRICRAFT_PACKAGE));
    }

    private static boolean isUnsafeBlock(Block block) {
        String className = block.getClass().getName();
        if (className.startsWith(EXTRA_UTILITIES_2_PACKAGE)) {
            return Loader.isModLoaded(EXTRA_UTILITIES_2_MOD_ID);
        }
        if (className.startsWith(AGRICRAFT_PACKAGE)) {
            return Loader.isModLoaded(AGRICRAFT_MOD_ID);
        }
        return false;
    }

    private static Object lockFor(Block block) {
        return block.getClass().getName().startsWith(AGRICRAFT_PACKAGE)
                ? AGRICRAFT_RENDER_LOCK
                : block;
    }

    private static <T> T callWithLock(Object lock, Supplier<T> operation) {
        synchronized (lock) {
            return operation.get();
        }
    }
}
