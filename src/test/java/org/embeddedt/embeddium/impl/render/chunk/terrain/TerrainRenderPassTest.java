package org.embeddedt.embeddium.impl.render.chunk.terrain;

import net.coderbot.iris.celeritas.IrisTerrainPass;
import net.coderbot.iris.celeritas.IrisCeleritasChunkShaderInterface;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the public configuration contract used to classify terrain render passes.
 */
class TerrainRenderPassTest {
    /**
     * Explicit depth writes must remain observable independently from the legacy pass shape.
     */
    @ParameterizedTest(name = "{0} keeps explicit writesDepth={1}")
    @MethodSource("explicitDepthWriteCases")
    void preservesExplicitDepthWrite(
            String name,
            boolean writesDepth,
            boolean reverseOrder,
            boolean fragmentDiscard
    ) {
        TerrainRenderPass pass = TerrainRenderPass.builder()
                .name(name)
                .writesDepth(writesDepth)
                .useReverseOrder(reverseOrder)
                .fragmentDiscard(fragmentDiscard)
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .vertexType(ChunkMeshFormats.COMPACT)
                .build();

        assertEquals(writesDepth, pass.writesDepth());
    }

    /**
     * Legacy pass definitions derive their depth policy from the draw order.
     */
    @ParameterizedTest(name = "legacy {0} derives writesDepth={1}")
    @MethodSource("legacyDepthWriteCases")
    void derivesLegacyDepthWrite(String name, boolean writesDepth, boolean reverseOrder) {
        TerrainRenderPass pass = TerrainRenderPass.builder()
                .name(name)
                .useReverseOrder(reverseOrder)
                .fragmentDiscard(false)
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .vertexType(ChunkMeshFormats.COMPACT)
                .build();

        assertEquals(writesDepth, pass.writesDepth());
    }

    /**
     * Main terrain pass classification must preserve opaque, cutout, translucent, and water semantics.
     */
    @ParameterizedTest(name = "main {0} maps to {1}")
    @MethodSource("mainPassSemantics")
    void classifiesMainPassSemantics(
            String name,
            boolean writesDepth,
            boolean reverseOrder,
            boolean fragmentDiscard,
            TerrainRenderPass.Semantic semantic,
            IrisTerrainPass expected,
            String expectedShaderPass
    ) {
        TerrainRenderPass pass = pass(name, writesDepth, reverseOrder, fragmentDiscard, semantic);

        assertEquals(semantic, pass.semantic());
        assertEquals(expected, IrisTerrainPass.fromTerrainPass(pass, false));
        assertEquals(expectedShaderPass, expected.getName());
    }

    /**
     * Shadow pass classification must use explicit depth writes, including shadow water.
     */
    @ParameterizedTest(name = "shadow {0} maps to {1}")
    @MethodSource("shadowPassSemantics")
    void classifiesShadowPassSemantics(
            String name,
            boolean writesDepth,
            boolean reverseOrder,
            boolean fragmentDiscard,
            TerrainRenderPass.Semantic semantic,
            IrisTerrainPass expected,
            String expectedShaderPass
    ) {
        TerrainRenderPass pass = pass(name, writesDepth, reverseOrder, fragmentDiscard, semantic);

        assertEquals(semantic, pass.semantic());
        assertEquals(expected, IrisTerrainPass.fromTerrainPass(pass, true));
        assertEquals(expectedShaderPass, expected.getName());
    }

    private static Stream<Arguments> mainPassSemantics() {
        return Stream.of(
                Arguments.of("opaque", true, false, false, TerrainRenderPass.Semantic.SOLID, IrisTerrainPass.GBUFFER_SOLID, "gbuffers_terrain"),
                Arguments.of("cutout", true, false, true, TerrainRenderPass.Semantic.CUTOUT, IrisTerrainPass.GBUFFER_CUTOUT, "gbuffers_terrain_cutout"),
                Arguments.of("translucent", false, true, false, TerrainRenderPass.Semantic.TRANSLUCENT, IrisTerrainPass.GBUFFER_TRANSLUCENT, "gbuffers_water"),
                Arguments.of("water", true, true, false, TerrainRenderPass.Semantic.WATER, IrisTerrainPass.GBUFFER_TRANSLUCENT, "gbuffers_water")
        );
    }

    private static Stream<Arguments> explicitDepthWriteCases() {
        return Stream.of(
                Arguments.of("opaque", false, false, false),
                Arguments.of("cutout", false, false, true),
                Arguments.of("translucent", true, true, false),
                Arguments.of("water", true, true, false),
                Arguments.of("shadow", false, false, true)
        );
    }

    private static Stream<Arguments> legacyDepthWriteCases() {
        return Stream.of(
                Arguments.of("solid", true, false),
                Arguments.of("translucent", false, true)
        );
    }

    private static Stream<Arguments> shadowPassSemantics() {
        return Stream.of(
                Arguments.of("shadow", true, false, false, TerrainRenderPass.Semantic.SOLID, IrisTerrainPass.SHADOW, "shadow"),
                Arguments.of("shadow_cutout", true, false, true, TerrainRenderPass.Semantic.CUTOUT, IrisTerrainPass.SHADOW_CUTOUT, "shadow"),
                Arguments.of("shadow_water", true, true, false, TerrainRenderPass.Semantic.WATER, IrisTerrainPass.SHADOW_TRANSLUCENT, "shadow_water")
        );
    }

    @ParameterizedTest(name = "{0} shadow={1} writes depth={2}")
    @MethodSource("depthWritePolicies")
    void appliesDepthWritePolicy(String name, boolean shadowPass, boolean expected) {
        TerrainRenderPass pass = pass(name, expected, shadowPass, false);

        assertEquals(expected, IrisCeleritasChunkShaderInterface.shouldWriteDepth(pass, shadowPass));
    }

    private static Stream<Arguments> depthWritePolicies() {
        return Stream.of(
                Arguments.of("translucent", false, false),
                Arguments.of("water", false, true),
                Arguments.of("shadow translucent", true, true)
        );
    }

    @Test
    void sharedTranslucentShaderUsesDefaultTranslucentVertexContract() {
        TerrainRenderPass translucentPass = pass(
                "translucent",
                false,
                true,
                false,
                TerrainRenderPass.Semantic.TRANSLUCENT,
                ChunkMeshFormats.COMPACT
        );
        TerrainRenderPass fluidPass = pass(
                "water",
                true,
                true,
                false,
                TerrainRenderPass.Semantic.WATER,
                ChunkMeshFormats.VANILLA_LIKE
        );
        Material translucent = new Material(translucentPass, AlphaCutoffParameter.ZERO, true);
        Material fluid = new Material(fluidPass, AlphaCutoffParameter.ZERO, true);
        RenderPassConfiguration<BlockRenderLayer> configuration = new RenderPassConfiguration<>(
                Map.of(BlockRenderLayer.TRANSLUCENT, translucent),
                Map.of(BlockRenderLayer.TRANSLUCENT, List.of(translucentPass, fluidPass)),
                translucent,
                translucent,
                translucent,
                fluid
        );

        assertSame(translucentPass, IrisTerrainPass.GBUFFER_TRANSLUCENT.toTerrainPass(configuration));
        assertSame(translucentPass, IrisTerrainPass.SHADOW_TRANSLUCENT.toTerrainPass(configuration));
    }

    private static TerrainRenderPass pass(
            String name,
            boolean writesDepth,
            boolean reverseOrder,
            boolean fragmentDiscard
    ) {
        return pass(name, writesDepth, reverseOrder, fragmentDiscard, null, ChunkMeshFormats.COMPACT);
    }

    private static TerrainRenderPass pass(
            String name,
            boolean writesDepth,
            boolean reverseOrder,
            boolean fragmentDiscard,
            TerrainRenderPass.Semantic semantic
    ) {
        return pass(name, writesDepth, reverseOrder, fragmentDiscard, semantic, ChunkMeshFormats.COMPACT);
    }

    private static TerrainRenderPass pass(
            String name,
            boolean writesDepth,
            boolean reverseOrder,
            boolean fragmentDiscard,
            TerrainRenderPass.Semantic semantic,
            ChunkVertexType vertexType
    ) {
        TerrainRenderPass.TerrainRenderPassBuilder builder = TerrainRenderPass.builder()
                .name(name)
                .writesDepth(writesDepth)
                .useReverseOrder(reverseOrder)
                .fragmentDiscard(fragmentDiscard)
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .vertexType(vertexType);
        if (semantic != null) {
            builder.semantic(semantic);
        }
        return builder.build();
    }
}
