package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.shader.pack.ActiniumShaderPack;
import com.dhj.actinium.shader.pack.ActiniumShaderPackResources;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ActiniumTerrainPhaseTest {
    @Test
    void resolveDrawBuffersReadsPerProgramTerrainDirectives() throws Exception {
        Path root = Files.createTempDirectory("actinium-terrain-phase-test");
        try {
            Path shaders = Files.createDirectories(root.resolve("shaders"));
            Files.writeString(shaders.resolve("gbuffers_terrain.fsh"), shaderWithDrawBuffers("234"), StandardCharsets.UTF_8);
            Files.writeString(shaders.resolve("gbuffers_terrain_cutout.fsh"), shaderWithDrawBuffers("56"), StandardCharsets.UTF_8);
            Files.writeString(shaders.resolve("gbuffers_terrain_cutout_mip.fsh"), shaderWithDrawBuffers("67"), StandardCharsets.UTF_8);
            Files.writeString(shaders.resolve("gbuffers_water.fsh"), shaderWithDrawBuffers("17"), StandardCharsets.UTF_8);

            try (ActiniumShaderPackResources resources = ActiniumShaderPackResources.load(new ActiniumShaderPack("test-pack", root, false))) {
                assertArrayEquals(new int[]{2, 3, 4}, ActiniumTerrainPhase.resolveDrawBuffers(resources, pass("solid", false)));
                assertArrayEquals(new int[]{5, 6}, ActiniumTerrainPhase.resolveDrawBuffers(resources, pass("cutout", false)));
                assertArrayEquals(new int[]{6, 7}, ActiniumTerrainPhase.resolveDrawBuffers(resources, pass("cutout_mipped", false)));
                assertArrayEquals(new int[]{1, 7}, ActiniumTerrainPhase.resolveDrawBuffers(resources, pass("translucent", true)));
            }
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void resolveDrawBuffersFallsBackToColortex1WhenProgramHasNoDirective() throws Exception {
        Path root = Files.createTempDirectory("actinium-terrain-phase-default-test");
        try {
            Path shaders = Files.createDirectories(root.resolve("shaders"));
            Files.writeString(shaders.resolve("gbuffers_terrain.fsh"), "#version 120\nvoid main() {}\n", StandardCharsets.UTF_8);

            try (ActiniumShaderPackResources resources = ActiniumShaderPackResources.load(new ActiniumShaderPack("test-pack", root, false))) {
                assertArrayEquals(new int[]{1}, ActiniumTerrainPhase.resolveDrawBuffers(resources, pass("solid", false)));
            }
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void resolveDrawBuffersUsesLegacyFallbackWithoutPackResources() {
        assertArrayEquals(
                new int[]{ActiniumPostTargets.TARGET_COLORTEX1, ActiniumPostTargets.TARGET_GAUX4},
                ActiniumTerrainPhase.resolveDrawBuffers(null, pass("solid", false))
        );
        assertArrayEquals(
                new int[]{ActiniumPostTargets.TARGET_COLORTEX1},
                ActiniumTerrainPhase.resolveDrawBuffers(null, null)
        );
    }

    private static TerrainRenderPass pass(String name, boolean reverseOrder) {
        return TerrainRenderPass.builder()
                .name(name)
                .useReverseOrder(reverseOrder)
                .fragmentDiscard(false)
                .useTranslucencySorting(reverseOrder)
                .hasNoLightmap(false)
                .vertexType(new VanillaLikeChunkVertex())
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .extraDefines(Map.of())
                .build();
    }

    private static String shaderWithDrawBuffers(String directive) {
        return """
                #version 120
                /* DRAWBUFFERS:%s */
                void main() {}
                """.formatted(directive);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }
}
