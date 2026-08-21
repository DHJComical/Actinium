package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkBuilderSchedulingTest {
    @Test
    void startsOneWorkerWithWarmStartTarget() {
        ChunkBuilder builder = newBuilder();

        try {
            assertEquals(32, builder.getTargetQueueSize());
            assertEquals(32, builder.getSchedulingBudget());
        } finally {
            builder.shutdown();
        }
    }

    @Test
    void doublesTargetWhenWorkerStarvesAfterBudgetLimitedDispatch() {
        ChunkBuilder builder = newBuilder();

        try {
            builder.setDispatchBudgetLimited(true);

            long deadline = System.nanoTime() + 1_000_000_000L;
            boolean targetDoubled = false;
            while (System.nanoTime() < deadline) {
                builder.tickSchedulingBudget();
                if (builder.getTargetQueueSize() == 64) {
                    targetDoubled = true;
                    break;
                }
                Thread.yield();
            }

            assertTrue(targetDoubled, "The worker did not report starvation before the deadline");
        } finally {
            builder.shutdown();
        }
    }

    private static ChunkBuilder newBuilder() {
        Supplier<ChunkBuildContext> contextSupplier = () -> new ChunkBuildContext(configuration());
        return new ChunkBuilder(ChunkBuilder.ManagedBlocker.NONE, contextSupplier, 1);
    }

    private static RenderPassConfiguration<BlockRenderLayer> configuration() {
        TerrainRenderPass pass = TerrainRenderPass.builder()
                .name("test")
                .fragmentDiscard(true)
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .vertexType(ChunkMeshFormats.COMPACT)
                .build();
        Material material = new Material(pass, AlphaCutoffParameter.ZERO, false);
        Map<BlockRenderLayer, Material> materials = Map.of(BlockRenderLayer.SOLID, material);
        Map<BlockRenderLayer, Collection<TerrainRenderPass>> stages = Map.of(BlockRenderLayer.SOLID, List.of(pass));
        return new RenderPassConfiguration<>(materials, stages, material, material, material);
    }
}
