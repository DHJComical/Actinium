package dhj.embeddedt.embeddium.impl.render.chunk.sorting;

import dhj.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the v3 translucency sort model: which states survive compaction, which variant the sorter picks for a given
 * set of quads, and that a partition tree's readout really is a permutation of the quads it was built from.
 *
 * <p>Every case drives the real sorter with a hand-built {@link QuadSet}; no reflection and no source-text matching.
 */
class SortStateCompactionTest {
    private static final SortState.Resortable DYNAMIC = new SortState.Dynamic(new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f});

    @Test
    void staticStateCompactsAwayButKeepsItsDebugIndex() {
        var state = new SortState.Static(new int[]{1, 0});

        assertEquals(1, state.debugIndex());
        assertEquals(2, state.quadCount());
        assertEquals(SortState.NONE, state.compactForStorage());
        assertTrue(!state.requiresDynamicSorting());
    }

    @Test
    void resortableStatesSurviveCompaction() {
        assertSame(DYNAMIC, DYNAMIC.compactForStorage());
        assertTrue(DYNAMIC.requiresDynamicSorting());
        assertEquals(3, DYNAMIC.debugIndex());
        assertEquals(3, DYNAMIC.quadCount());
    }

    @Test
    void emptyGeometryNeedsNoSorting() {
        assertEquals(SortState.NONE, TranslucentSorter.analyze(quads()));
    }

    @Test
    void coplanarQuadsNeedNoSorting() {
        QuadSet set = quads(
                quad(0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.0f, ModelQuadFacing.POS_Y),
                quad(1.0f, 0.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.0f, ModelQuadFacing.POS_Y));

        assertEquals(SortState.NONE, TranslucentSorter.analyze(set));
    }

    @Test
    void parallelQuadsOnDistinctPlanesGetOneStaticOrder() {
        QuadSet set = quads(
                quad(0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.0f, ModelQuadFacing.POS_Y),
                quad(0.0f, 2.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.0f, ModelQuadFacing.POS_Y));

        var state = assertInstanceOf(SortState.Static.class, TranslucentSorter.analyze(set));

        assertEquals(2, state.quadCount());
        assertTrue(isPermutation(state.order(), 2), "static order must visit every quad exactly once");
        assertArrayEquals(state.order(), TranslucentSorter.order(state, 12.0f, 5.0f, -3.0f));
        assertEquals(SortState.NONE, state.compactForStorage());
    }

    @Test
    void crossingQuadsGetAPartitionTreeWhoseOrderIsAPermutation() {
        QuadSet set = quads(
                quad(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, ModelQuadFacing.POS_X),
                quad(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, ModelQuadFacing.POS_Y));

        var tree = assertInstanceOf(PartitionTree.class, TranslucentSorter.analyze(set));

        assertEquals(2, tree.quadCount());
        assertTrue(isPermutation(tree.order(4.0f, 4.0f, 4.0f), 2));
        assertTrue(isPermutation(tree.order(-4.0f, 3.0f, 0.0f), 2));
        assertSame(tree, tree.compactForStorage());
    }

    private static boolean isPermutation(int[] order, int count) {
        if (order.length != count) {
            return false;
        }

        boolean[] seen = new boolean[count];

        for (int value : order) {
            if (value < 0 || value >= count || seen[value]) {
                return false;
            }

            seen[value] = true;
        }

        return true;
    }

    /** Builds a {@link QuadSet} from {@link #quad} descriptors. */
    private static QuadSet quads(float[]... specs) {
        int count = specs.length;
        float[] centers = new float[count * 3];
        float[] bounds = new float[count * 6];
        float[] normals = new float[count * 3];
        float[] dots = new float[count];
        byte[] facings = new byte[count];

        for (int quad = 0; quad < count; quad++) {
            float[] spec = specs[quad];

            System.arraycopy(spec, 0, centers, quad * 3, 3);
            System.arraycopy(spec, 3, bounds, quad * 6, 6);
            System.arraycopy(spec, 9, normals, quad * 3, 3);

            dots[quad] = spec[9] * spec[0] + spec[10] * spec[1] + spec[11] * spec[2];
            facings[quad] = (byte) spec[12];
        }

        return new QuadSet(count, centers, bounds, normals, dots, facings);
    }

    /** An axis-aligned cube of the given half extent, centred on {@code (cx, cy, cz)}, with the given normal. */
    private static float[] quad(float cx, float cy, float cz, float halfSize, float nx, float ny, float nz,
                                ModelQuadFacing facing) {
        return new float[]{
                cx, cy, cz,
                cx - halfSize, cy - halfSize, cz - halfSize,
                cx + halfSize, cy + halfSize, cz + halfSize,
                nx, ny, nz, facing.ordinal()
        };
    }
}
