package com.dhj.actinium.render.vertex;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FastVertexLayoutCalculatorTest {
    private static VertexFormatElement element(VertexFormatElement.EnumType type,
                                               VertexFormatElement.EnumUsage usage,
                                               int count) {
        return new VertexFormatElement(0, type, usage, count);
    }

    /**
     * Reproduces the original private element advance: one step with wraparound,
     * repeated past PADDING elements. The computed ring must agree with it for every
     * element position.
     */
    private static int originalAdvance(List<VertexFormatElement> elements, int index) {
        int next = (index + 1) % elements.size();
        while (elements.get(next).getUsage() == VertexFormatElement.EnumUsage.PADDING) {
            next = (next + 1) % elements.size();
        }
        return next;
    }

    private static void assertRingMatchesOriginalAdvance(List<VertexFormatElement> elements) {
        FastVertexLayoutCalculator.Layout layout = FastVertexLayoutCalculator.compute(elements);
        for (int i = 0; i < elements.size(); i++) {
            assertEquals(originalAdvance(elements, i), layout.nextIndices()[i],
                "advance ring mismatch at element " + i);
        }
    }

    @Test
    void offsetsAccumulateElementSizes() {
        List<VertexFormatElement> elements = DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL.getElements();
        FastVertexLayoutCalculator.Layout layout = FastVertexLayoutCalculator.compute(elements);

        assertEquals(5, layout.offsets().length);
        assertArrayEquals(new int[] {0, 12, 20, 24, 27}, layout.offsets());
        assertEquals(28, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL.getSize());
    }

    @Test
    void advanceRingSkipsPaddingElements() {
        List<VertexFormatElement> elements = DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL.getElements();
        FastVertexLayoutCalculator.Layout layout = FastVertexLayoutCalculator.compute(elements);

        assertArrayEquals(new int[] {1, 2, 3, 0, 0}, layout.nextIndices());
    }

    @Test
    void advanceRingWrapsAroundTrailingPadding() {
        List<VertexFormatElement> elements = DefaultVertexFormats.POSITION_NORMAL.getElements();
        FastVertexLayoutCalculator.Layout layout = FastVertexLayoutCalculator.compute(elements);

        // POSITION_NORMAL is position + normal + 1 byte of trailing PADDING, so advancing
        // from the normal element skips the PADDING slot and wraps back to the position.
        assertArrayEquals(new int[] {1, 0, 0}, layout.nextIndices());
    }

    @Test
    void ringMatchesOriginalAdvanceOnEveryBuiltInFormat() {
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.BLOCK.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.ITEM.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_COLOR.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_TEX.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_NORMAL.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_TEX_COLOR.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_TEX_NORMAL.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_TEX_LMAP_COLOR.getElements());
        assertRingMatchesOriginalAdvance(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL.getElements());
    }

    @Test
    void ringPointsToFirstNonPaddingAfterPaddingOnlyStart() {
        // A format whose first element is PADDING has no advance through the regular
        // path because begin selects it directly; the ring still resolves from it.
        List<VertexFormatElement> elements = List.of(
            element(VertexFormatElement.EnumType.BYTE, VertexFormatElement.EnumUsage.PADDING, 1),
            element(VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3)
        );

        FastVertexLayoutCalculator.Layout layout = FastVertexLayoutCalculator.compute(elements);

        assertArrayEquals(new int[] {1, 1}, layout.nextIndices());
        assertArrayEquals(new int[] {0, 1}, layout.offsets());
    }

    @Test
    void rejectsFormatsWithoutNonPaddingElements() {
        List<VertexFormatElement> elements = List.of(
            element(VertexFormatElement.EnumType.BYTE, VertexFormatElement.EnumUsage.PADDING, 1)
        );

        assertThrows(IllegalArgumentException.class, () -> FastVertexLayoutCalculator.compute(elements));
    }

    @Test
    void emptyFormatYieldsEmptyArrays() {
        FastVertexLayoutCalculator.Layout layout = FastVertexLayoutCalculator.compute(List.of());

        assertEquals(0, layout.offsets().length);
        assertEquals(0, layout.nextIndices().length);
    }
}
