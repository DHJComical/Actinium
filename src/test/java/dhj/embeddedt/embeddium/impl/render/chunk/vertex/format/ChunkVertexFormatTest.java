package dhj.embeddedt.embeddium.impl.render.chunk.vertex.format;

import dhj.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
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
    void compactFormatPacksTextureAndLightCoordsAsIntegers() {
        var format = ChunkMeshFormats.COMPACT.getVertexFormat();

        // The rebalanced layout splits the texture coordinate and the light/draw-parameter word across the same
        // 24 byte stride, so both are read as integers and the low bits are reassembled in the shader.
        var texCoord = format.getAttribute("a_TexCoord");
        assertEquals(12, texCoord.getPointer());
        assertEquals(GlVertexAttributeFormat.UNSIGNED_SHORT, texCoord.getFormat());
        assertEquals(2, texCoord.getCount());
        assertTrue(texCoord.isIntType());

        var lightCoord = format.getAttribute("a_LightCoord");
        assertEquals(16, lightCoord.getPointer());
        assertEquals(GlVertexAttributeFormat.UNSIGNED_INT, lightCoord.getFormat());
        assertEquals(1, lightCoord.getCount());
        assertTrue(lightCoord.isIntType());
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
