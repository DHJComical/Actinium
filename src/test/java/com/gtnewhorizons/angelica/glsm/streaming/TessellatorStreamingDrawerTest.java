package com.gtnewhorizons.angelica.glsm.streaming;

import org.lwjgl.opengl.GL11;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.COLOR_BIT;
import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.NORMAL_BIT;
import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.TEXTURE_BIT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TessellatorStreamingDrawerTest {
    @Test
    void restartsRepackCapacityAfterDrawerDestroy() {
        assertEquals(0x10000, TessellatorStreamingDrawer.nextRepackCapacity(0, 96));
    }

    @Test
    void growsRepackCapacityFromTheExistingPowerOfTwo() {
        assertEquals(0x20000, TessellatorStreamingDrawer.nextRepackCapacity(0x10000, 0x10001));
    }

    @Test
    void packsPositionColorRawVertices() {
        final int[] raw = {
            1, 2, 3, 4, 5, 6, 7, 8,
            11, 12, 13, 14, 15, 16, 17, 18
        };
        final IntBuffer packed = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asIntBuffer();

        assertEquals(32, TessellatorStreamingDrawer.packRawVertices(packed, raw, raw.length, COLOR_BIT));
        assertArrayEquals(new int[] { 1, 2, 3, 6, 11, 12, 13, 16 }, read(packed));
    }

    @Test
    void packsPositionTextureColorRawVerticesAsOneContiguousCopy() {
        final int[] raw = {
            1, 2, 3, 4, 5, 6, 7, 8,
            11, 12, 13, 14, 15, 16, 17, 18
        };
        final IntBuffer packed = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder()).asIntBuffer();

        assertEquals(48, TessellatorStreamingDrawer.packRawVertices(
            packed,
            raw,
            raw.length,
            TEXTURE_BIT | COLOR_BIT));
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 11, 12, 13, 14, 15, 16 }, read(packed));
    }

    @Test
    void fallsBackForFormatsWithNormalData() {
        final IntBuffer packed = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asIntBuffer();

        assertEquals(-1, TessellatorStreamingDrawer.packRawVertices(packed, new int[8], 8, NORMAL_BIT));
    }

    @Test
    void acceptsAConstantAttributeRectangularQuad() {
        final int[] raw = rectangularQuad(0x11223344);

        assertTrue(TessellatorStreamingDrawer.isSingleQuadFastPath(
            GL11.GL_QUADS,
            4,
            TEXTURE_BIT | COLOR_BIT,
            raw,
            raw.length));
    }

    @Test
    void rejectsAQuadWithDifferentFlatSensitiveAttributes() {
        final int[] raw = rectangularQuad(0x11223344);
        raw[5 + 8] = 0x01020304;

        assertFalse(TessellatorStreamingDrawer.isSingleQuadFastPath(
            GL11.GL_QUADS,
            4,
            TEXTURE_BIT | COLOR_BIT,
            raw,
            raw.length));
    }

    @Test
    void rejectsNonQuadAndNonRectangularInput() {
        final int[] raw = rectangularQuad(0x11223344);
        raw[16] = Float.floatToRawIntBits(3.0f);

        assertFalse(TessellatorStreamingDrawer.isSingleQuadFastPath(GL11.GL_LINES, 4, COLOR_BIT, raw, raw.length));
        assertFalse(TessellatorStreamingDrawer.isSingleQuadFastPath(GL11.GL_QUADS, 4, COLOR_BIT, raw, raw.length));
    }

    private static int[] read(IntBuffer buffer) {
        final int[] result = new int[buffer.position()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

    private static int[] rectangularQuad(int color) {
        return new int[] {
            Float.floatToRawIntBits(0.0f), Float.floatToRawIntBits(0.0f), 0, Float.floatToRawIntBits(0.0f), Float.floatToRawIntBits(0.0f), color, 0, 220,
            Float.floatToRawIntBits(2.0f), Float.floatToRawIntBits(0.0f), 0, Float.floatToRawIntBits(1.0f), Float.floatToRawIntBits(0.0f), color, 0, 220,
            Float.floatToRawIntBits(2.0f), Float.floatToRawIntBits(2.0f), 0, Float.floatToRawIntBits(1.0f), Float.floatToRawIntBits(1.0f), color, 0, 220,
            Float.floatToRawIntBits(0.0f), Float.floatToRawIntBits(2.0f), 0, Float.floatToRawIntBits(0.0f), Float.floatToRawIntBits(1.0f), color, 0, 220
        };
    }
}
