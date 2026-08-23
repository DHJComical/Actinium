package org.embeddedt.embeddium.impl.render.chunk.sorting;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the {@code GlBufferSegment len <= 0} crash (issue: translucent sorting
 * allocated a zero-length index buffer).
 *
 * <p>A section that mixes a dynamically-sorted pass (camera-dependent) with a statically-sorted
 * one (quads on the same plane) triggers a sort task for the whole section. The statically-sorted
 * state is compacted for storage, which drops its quad centers ({@code centersLength == 0}). The
 * sort task must only re-generate index buffers for states that require dynamic sorting; otherwise
 * it would allocate a zero-length index buffer that later breaks the GL buffer arena when freed.</p>
 */
class TranslucentQuadAnalyzerSortStateTest {
    @Test
    void dynamicSortStateKeepsCentersAfterCompaction() {
        TranslucentQuadAnalyzer.SortState state = new TranslucentQuadAnalyzer.SortState(
                TranslucentQuadAnalyzer.Level.DYNAMIC,
                new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f },
                6,
                new BitSet(),
                new Vector3f());

        assertTrue(state.requiresDynamicSorting());
        assertEquals(6, state.centersLength());

        TranslucentQuadAnalyzer.SortState compacted = state.compactForStorage();
        assertEquals(6, compacted.centersLength(), "dynamic sort state must keep its centers");
        assertTrue(compacted.requiresDynamicSorting());
    }

    @Test
    void staticSortStateDropsCentersAfterCompaction() {
        TranslucentQuadAnalyzer.SortState state = new TranslucentQuadAnalyzer.SortState(
                TranslucentQuadAnalyzer.Level.STATIC,
                new float[] { 1.0f, 2.0f, 3.0f },
                3,
                new BitSet(),
                new Vector3f());

        assertFalse(state.requiresDynamicSorting(), "static sort does not depend on the camera");
        assertEquals(3, state.centersLength());

        TranslucentQuadAnalyzer.SortState compacted = state.compactForStorage();
        assertEquals(0, compacted.centersLength(), "static sort state is compacted to zero centers");
        assertFalse(compacted.requiresDynamicSorting());
    }
}
