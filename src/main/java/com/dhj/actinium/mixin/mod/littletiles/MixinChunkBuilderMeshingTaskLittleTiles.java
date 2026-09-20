package com.dhj.actinium.mixin.mod.littletiles;

import com.dhj.actinium.compat.littletiles.LittleTilesCompat;
import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.task.ChunkBuilderMeshingTask;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Feeds LittleTiles tile entity vertex caches into the section mesh build. LittleTiles appends
 * those caches to the vanilla chunk upload buffer inside {@code ChunkRenderDispatcher.uploadChunk}
 * via ASM; Actinium never calls that method, so without this hook the tiles are invisible.
 */
@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class MixinChunkBuilderMeshingTaskLittleTiles {
    @Unique
    private static final String EXECUTE_METHOD =
            "execute(Ldhj/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;"
                    + "Ldhj/embeddedt/embeddium/impl/util/task/CancellationToken;"
                    + ")Ldhj/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;";

    @WrapOperation(
            method = EXECUTE_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dhj/actinium/render/terrain/compile/VintageChunkBuildContext;"
                            + "convertVanillaDataToCeleritasData("
                            + "Ldhj/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;)V",
                    remap = false
            )
    )
    private void actinium$appendLittleTilesGeometry(
            VintageChunkBuildContext instance,
            ChunkBuildBuffers buffers,
            Operation<Void> original
    ) {
        LittleTilesCompat.appendSectionGeometry(instance);
        original.call(instance, buffers);
    }
}
