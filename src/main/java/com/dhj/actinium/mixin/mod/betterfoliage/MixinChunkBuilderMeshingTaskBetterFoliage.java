package com.dhj.actinium.mixin.mod.betterfoliage;

import betterfoliage.render.feature.RenderingHandler;
import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.pipeline.VintageBlockRenderer;
import com.dhj.actinium.render.terrain.compile.task.ChunkBuilderMeshingTask;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes RLFoliage block features through Actinium's current chunk meshing task.
 * RLFoliage still ships a Celeritas Mixin against the legacy org.taumc task name,
 * so without this adapter only its particle effects are active.
 */
@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class MixinChunkBuilderMeshingTaskBetterFoliage {
    @Unique
    private static final String EXECUTE_METHOD =
            "execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;"
                    + "Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;"
                    + ")Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;";

    @Redirect(
            method = EXECUTE_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dhj/actinium/compat/blockrender/ModdedBlockRenderCompat;canRenderInLayer("
                            + "Lnet/minecraft/block/Block;Lnet/minecraft/block/state/IBlockState;"
                            + "Lnet/minecraft/util/BlockRenderLayer;)Z",
                    remap = false
            )
    )
    private boolean actinium$betterFoliageCanRenderInLayer(
            Block block,
            IBlockState state,
            BlockRenderLayer layer
    ) {
        return RenderingHandler.canRenderBlockInLayer(block, state, layer);
    }

    @WrapOperation(
            method = EXECUTE_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dhj/actinium/render/terrain/compile/pipeline/VintageBlockRenderer;renderBlock("
                            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;"
                            + "Lcom/dhj/actinium/world/cloned/ActiniumBlockAccess;"
                            + "Lnet/minecraft/util/BlockRenderLayer;)V",
                    remap = false
            )
    )
    private void actinium$betterFoliageWrapNewRenderBlock(
            VintageBlockRenderer dispatcher,
            IBlockState state,
            BlockPos pos,
            ActiniumBlockAccess blockAccess,
            BlockRenderLayer layer,
            Operation<Void> original,
            @Local(name = "buildContext") VintageChunkBuildContext buildContext
    ) {
        Boolean result = RenderingHandler.wrapRenderBlock(
                () -> {
                    original.call(dispatcher, state, pos, blockAccess, layer);
                    return Boolean.TRUE;
                },
                state,
                pos,
                blockAccess,
                () -> buildContext.getBufferForLayer(layer),
                layer
        );
        if (result == null) {
            original.call(dispatcher, state, pos, blockAccess, layer);
        }
    }

    @WrapOperation(
            method = EXECUTE_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dhj/actinium/render/terrain/compile/pipeline/VintageBlockRenderer;renderBlock("
                            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;"
                            + "Lcom/dhj/actinium/world/cloned/ActiniumBlockAccess;"
                            + "Lnet/minecraft/util/BlockRenderLayer;Z)V",
                    remap = false
            )
    )
    private void actinium$betterFoliageWrapNewRenderBlockWithOptimization(
            VintageBlockRenderer dispatcher,
            IBlockState state,
            BlockPos pos,
            ActiniumBlockAccess blockAccess,
            BlockRenderLayer layer,
            boolean allowRenderPassOptimization,
            Operation<Void> original,
            @Local(name = "buildContext") VintageChunkBuildContext buildContext
    ) {
        Boolean result = RenderingHandler.wrapRenderBlock(
                () -> {
                    original.call(dispatcher, state, pos, blockAccess, layer, allowRenderPassOptimization);
                    return Boolean.TRUE;
                },
                state,
                pos,
                blockAccess,
                () -> buildContext.getBufferForLayer(layer),
                layer
        );
        if (result == null) {
            original.call(dispatcher, state, pos, blockAccess, layer, allowRenderPassOptimization);
        }
    }

    @WrapOperation(
            method = EXECUTE_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dhj/actinium/compat/blockrender/ModdedBlockRenderCompat;renderBlock("
                            + "Lnet/minecraft/client/renderer/BlockRendererDispatcher;"
                            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/world/IBlockAccess;"
                            + "Lnet/minecraft/client/renderer/BufferBuilder;)V",
                    remap = false
            )
    )
    private void actinium$betterFoliageWrapVanillaRenderBlock(
            BlockRendererDispatcher dispatcher,
            IBlockState state,
            BlockPos pos,
            IBlockAccess blockAccess,
            BufferBuilder buffer,
            Operation<Void> original,
            @Local(name = "layer") BlockRenderLayer layer
    ) {
        Boolean result = RenderingHandler.wrapRenderBlock(
                () -> {
                    original.call(dispatcher, state, pos, blockAccess, buffer);
                    return Boolean.TRUE;
                },
                state,
                pos,
                blockAccess,
                () -> buffer,
                layer
        );
        if (result == null) {
            original.call(dispatcher, state, pos, blockAccess, buffer);
        }
    }
}
