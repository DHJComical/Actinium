package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkVertexFormatTest {
    @Test
    void compactFormatStoresNormalizedRdhFactorAfterLight() {
        var attribute = ChunkMeshFormats.COMPACT.getVertexFormat().getAttribute("a_RdhFactor");

        assertEquals(24, ChunkMeshFormats.COMPACT.getVertexFormat().getStride());
        assertEquals(20, attribute.getPointer());
        assertEquals(GlVertexAttributeFormat.BYTE, attribute.getFormat());
        assertEquals(4, attribute.getCount());
        assertEquals(4, attribute.getSize());
        assertFalse(attribute.isIntType());
        assertTrue(attribute.isNormalized());
    }

    @Test
    void vanillaLikeFormatStoresNormalizedRdhFactorAfterPackedLight() {
        var attribute = ChunkMeshFormats.VANILLA_LIKE.getVertexFormat().getAttribute("a_RdhFactor");

        assertEquals(32, ChunkMeshFormats.VANILLA_LIKE.getVertexFormat().getStride());
        assertEquals(28, attribute.getPointer());
        assertEquals(GlVertexAttributeFormat.BYTE, attribute.getFormat());
        assertEquals(4, attribute.getCount());
        assertEquals(4, attribute.getSize());
        assertFalse(attribute.isIntType());
        assertTrue(attribute.isNormalized());
    }
}
