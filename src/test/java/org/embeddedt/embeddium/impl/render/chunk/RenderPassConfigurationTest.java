package org.embeddedt.embeddium.impl.render.chunk;

import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderPassConfigurationTest {
    @Test
    void detectsRenderTypesConsolidatedIntoOnePass() {
        TerrainRenderPass sharedPass = pass("cutout_mipped");
        RenderPassConfiguration<BlockRenderLayer> configuration = configuration(
            material(sharedPass, false),
            material(sharedPass, true)
        );

        assertTrue(configuration.usesSameRenderPass(BlockRenderLayer.CUTOUT, BlockRenderLayer.CUTOUT_MIPPED));
    }

    @Test
    void preservesSeparateRenderPasses() {
        RenderPassConfiguration<BlockRenderLayer> configuration = configuration(
            material(pass("cutout"), false),
            material(pass("cutout_mipped"), true)
        );

        assertFalse(configuration.usesSameRenderPass(BlockRenderLayer.CUTOUT, BlockRenderLayer.CUTOUT_MIPPED));
    }

    @Test
    void rejectsMissingRenderTypes() {
        Material cutout = material(pass("cutout"), false);
        RenderPassConfiguration<BlockRenderLayer> configuration = new RenderPassConfiguration<>(
            Map.of(BlockRenderLayer.CUTOUT, cutout),
            Map.of(BlockRenderLayer.CUTOUT, List.of(cutout.pass)),
            cutout,
            cutout,
            cutout
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> configuration.usesSameRenderPass(BlockRenderLayer.CUTOUT, BlockRenderLayer.CUTOUT_MIPPED)
        );
    }

    private static RenderPassConfiguration<BlockRenderLayer> configuration(Material cutout, Material cutoutMipped) {
        Map<BlockRenderLayer, Material> materials = Map.of(
            BlockRenderLayer.CUTOUT, cutout,
            BlockRenderLayer.CUTOUT_MIPPED, cutoutMipped
        );
        Map<BlockRenderLayer, Collection<TerrainRenderPass>> stages = Map.of(
            BlockRenderLayer.CUTOUT, List.of(cutout.pass),
            BlockRenderLayer.CUTOUT_MIPPED, List.of(cutoutMipped.pass)
        );
        return new RenderPassConfiguration<>(materials, stages, cutout, cutoutMipped, cutoutMipped);
    }

    private static TerrainRenderPass pass(String name) {
        return TerrainRenderPass.builder()
            .name(name)
            .fragmentDiscard(true)
            .primitiveType(QuadPrimitiveType.TRIANGULATED)
            .vertexType(ChunkMeshFormats.COMPACT)
            .build();
    }

    private static Material material(TerrainRenderPass pass, boolean mipped) {
        return new Material(pass, AlphaCutoffParameter.ZERO, mipped);
    }
}
