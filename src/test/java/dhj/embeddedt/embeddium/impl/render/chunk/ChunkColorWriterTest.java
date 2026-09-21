package dhj.embeddedt.embeddium.impl.render.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the vertex-colour path that is actually consumed by the chunk meshing pipeline: {@code EMBEDDIUM} writes the
 * baked ambient-occlusion factor into the vertex colour through {@link dhj.embeddedt.embeddium.api.util.ColorMixer}.
 * Multiplying by full AO must be lossless, and small AO factors must round to the nearest step instead of collapsing
 * to zero.
 */
class ChunkColorWriterTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MID_GREY = 0xFF808080;

    @Test
    void fullAoLeavesColoursUnchanged() {
        assertEquals(WHITE, ChunkColorWriter.EMBEDDIUM.writeColor(WHITE, 1.0f));
        assertEquals(MID_GREY, ChunkColorWriter.EMBEDDIUM.writeColor(MID_GREY, 1.0f));
    }

    @Test
    void zeroAoOnlyDarkensRgb() {
        assertEquals(0xFF000000, ChunkColorWriter.EMBEDDIUM.writeColor(WHITE, 0.0f));
    }

    @Test
    void smallAoRoundsToTheNearestStep() {
        // One step of AO (1/255) must survive as one step of colour; truncation would drop it to zero.
        assertEquals(0xFF010101, ChunkColorWriter.EMBEDDIUM.writeColor(WHITE, 1.0f / 255.0f));
    }
}
