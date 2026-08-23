package org.embeddedt.embeddium.impl.render.chunk.shader;

import net.coderbot.iris.celeritas.vertices.ExtendedChunkVertexType;
import net.coderbot.iris.celeritas.vertices.TerrainVertexFormatRequirements;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkShaderOptionsTest {
    @Test
    void disablesCorrectionShaderForLegacyGlslPath() {
        TerrainRenderPass pass = TerrainRenderPass.builder()
                .name("test")
                .vertexType(ChunkMeshFormats.VANILLA_LIKE)
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .build();
        ChunkShaderOptions options = new ChunkShaderOptions(List.of(), pass);

        assertTrue(options.constants().getDefineStrings().contains("#define USE_BILINEAR_CORRECTION"));
        assertFalse(options.constants(false).getDefineStrings().contains("#define USE_BILINEAR_CORRECTION"));

        TerrainRenderPass extendedPass = TerrainRenderPass.builder()
                .name("extended")
                .vertexType(new ExtendedChunkVertexType(TerrainVertexFormatRequirements.of()))
                .primitiveType(QuadPrimitiveType.TRIANGULATED)
                .build();
        ChunkShaderOptions extendedOptions = new ChunkShaderOptions(List.of(), extendedPass);
        assertFalse(extendedOptions.constants().getDefineStrings().contains("#define USE_BILINEAR_CORRECTION"));
    }
}
