package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import com.mitchej123.lwjgl.LWJGLServiceProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SectionRenderDataUnsafeTest {
    private static boolean nativeLwjglAvailable;

    @BeforeAll
    static void detectNativeLwjgl() {
        try {
            nativeLwjglAvailable = LWJGLServiceProvider.LWJGL != null;
        } catch (Throwable failure) {
            System.err.println("Skipping native metadata tests because OpenGL capabilities are unavailable: " + failure);
            nativeLwjglAvailable = false;
        }
    }

    @Test
    void convertsVertexSpansAndElementCountsForBothQuadLayouts() {
        assertEquals(8, SectionRenderDataUnsafe.elementsForVertices(8, QuadPrimitiveType.DIRECT));
        assertEquals(8, SectionRenderDataUnsafe.verticesForElements(8, QuadPrimitiveType.DIRECT));

        assertEquals(12, SectionRenderDataUnsafe.elementsForVertices(8, QuadPrimitiveType.TRIANGULATED));
        assertEquals(8, SectionRenderDataUnsafe.verticesForElements(12, QuadPrimitiveType.TRIANGULATED));
    }

    @Test
    void clearsMultiDrawStateAndReleasesNativeArrays() {
        assumeNativeLwjgl();
        MultiDrawBatch batch = new MultiDrawBatch(4);
        try {
            assertEquals(4, batch.capacity());
            assertEquals(0, batch.size());
            assertTrue(batch.isEmpty());

            batch.size = 2;
            batch.maxElementCount = 17;

            assertEquals(2, batch.size());
            assertEquals(17, batch.getIndexBufferSize());
            assertFalse(batch.isEmpty());

            batch.clear();

            assertEquals(0, batch.size());
            assertEquals(0, batch.maxElementCount);
            assertEquals(0, batch.getIndexBufferSize());
            assertTrue(batch.isEmpty());
            assertEquals(4, batch.capacity());
        } finally {
            batch.delete();
        }
    }

    @Test
    void writesAndRebasesFullMetadata() {
        assumeNativeLwjgl();
        SectionRenderDataUnsafe.Strategy strategy = SectionRenderDataUnsafe.Strategy.FULL;
        ChunkPrimitiveType primitiveType = QuadPrimitiveType.TRIANGULATED;
        long heap = strategy.allocateHeap();
        try {
            assertNotEquals(0L, heap);

            int sectionIndex = 7;
            strategy.writeMeshesAndSliceMask(heap, sectionIndex, 100, 200, ranges(), primitiveType);

            assertEquals(0b1101011, strategy.getSliceMask(heap, sectionIndex));

            long row = strategy.heapPointer(heap, sectionIndex);
            int[] expectedVertexOffsets = {100, 104, 112, 112, 116, 116, 120};
            int[] expectedElementCounts = {6, 12, 0, 6, 0, 6, 12};
            int[] expectedIndexOffsets = {200, 224, 272, 272, 296, 296, 320};

            assertFullMetadata(strategy, row, primitiveType,
                    expectedVertexOffsets, expectedElementCounts, expectedIndexOffsets);

            strategy.rebase(row, 1000, 4000, primitiveType);

            assertFullMetadata(strategy, row, primitiveType,
                    new int[] {1000, 1004, 1012, 1012, 1016, 1016, 1020},
                    expectedElementCounts,
                    new int[] {4000, 4024, 4072, 4072, 4096, 4096, 4120});
        } finally {
            strategy.freeHeap(heap);
        }
    }

    @Test
    void writesAndRebasesCompactMetadata() {
        assumeNativeLwjgl();
        SectionRenderDataUnsafe.Strategy strategy = SectionRenderDataUnsafe.Strategy.COMPACT;
        ChunkPrimitiveType primitiveType = QuadPrimitiveType.TRIANGULATED;
        long heap = strategy.allocateHeap();
        try {
            assertNotEquals(0L, heap);

            int sectionIndex = 9;
            strategy.writeMeshesAndSliceMask(heap, sectionIndex, 300, 900, ranges(), primitiveType);

            assertEquals(0b1101011, strategy.getSliceMask(heap, sectionIndex));

            long row = strategy.heapPointer(heap, sectionIndex);
            int[] expectedFencePosts = {300, 304, 312, 312, 316, 316, 320, 328};
            int[] expectedElementCounts = {6, 12, 0, 6, 0, 6, 12};

            assertCompactMetadata(strategy, row, primitiveType, expectedFencePosts, expectedElementCounts);

            strategy.rebase(row, 700, 1234, primitiveType);

            assertCompactMetadata(strategy, row, primitiveType,
                    new int[] {700, 704, 712, 712, 716, 716, 720, 728},
                    expectedElementCounts);
        } finally {
            strategy.freeHeap(heap);
        }
    }

    private static void assertFullMetadata(SectionRenderDataUnsafe.Strategy strategy,
                                           long row,
                                           ChunkPrimitiveType primitiveType,
                                           int[] expectedVertexOffsets,
                                           int[] expectedElementCounts,
                                           int[] expectedIndexOffsets) {
        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            assertEquals(expectedVertexOffsets[facing], strategy.getVertexOffset(row, facing));
            assertEquals(expectedElementCounts[facing], strategy.getElementCount(row, facing, primitiveType));
            assertEquals(expectedIndexOffsets[facing], strategy.getIndexOffset(row, facing));
            assertEquals(expectedVertexOffsets[facing]
                            + SectionRenderDataUnsafe.verticesForElements(expectedElementCounts[facing], primitiveType),
                    strategy.getRunVertexEnd(row, facing, primitiveType));
        }
    }

    private static void assertCompactMetadata(SectionRenderDataUnsafe.Strategy strategy,
                                              long row,
                                              ChunkPrimitiveType primitiveType,
                                              int[] expectedFencePosts,
                                              int[] expectedElementCounts) {
        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            assertEquals(expectedFencePosts[facing], strategy.getVertexOffset(row, facing));
            assertEquals(expectedElementCounts[facing], strategy.getElementCount(row, facing, primitiveType));
            assertEquals(0, strategy.getIndexOffset(row, facing));
            assertEquals(expectedFencePosts[facing + 1], strategy.getRunVertexEnd(row, facing, primitiveType));
        }
    }

    private static void assumeNativeLwjgl() {
        assumeTrue(nativeLwjglAvailable, "OpenGL capabilities are unavailable in the test JVM");
    }

    private static Map<ModelQuadFacing, VertexRange> ranges() {
        Map<ModelQuadFacing, VertexRange> ranges = new EnumMap<>(ModelQuadFacing.class);
        ranges.put(ModelQuadFacing.POS_X, new VertexRange(0, 4));
        ranges.put(ModelQuadFacing.POS_Y, new VertexRange(4, 8));
        ranges.put(ModelQuadFacing.NEG_X, new VertexRange(12, 4));
        ranges.put(ModelQuadFacing.NEG_Z, new VertexRange(16, 4));
        ranges.put(ModelQuadFacing.UNASSIGNED, new VertexRange(20, 8));
        return ranges;
    }
}
