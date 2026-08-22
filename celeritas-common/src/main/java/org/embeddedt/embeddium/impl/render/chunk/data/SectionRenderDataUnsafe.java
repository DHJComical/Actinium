package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

import java.util.Map;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

// This code is a terrible hack to get around the fact that we are so incredibly memory bound, and that we
// have no control over memory layout. The chunk rendering code spends an astronomical amount of time chasing
// object pointers that are scattered across the heap. Worse yet, because render state is initialized over a long
// period of time as the world loads, those objects are never even remotely close to one another in heap, so
// you also have to pay the penalty of a DTLB miss on every other access.
//
// Unfortunately, Hotspot *still* produces abysmal machine code for the chunk rendering code paths, since any usage of
// unsafe memory intrinsics seems to cause it to become paranoid about memory aliasing. Well, that, and it just produces
// terrible machine code in pretty much every critical code path we seem to have...
//
// Please never try to write performance critical code in Java. This is what it will do to you. And you will still be
// three times slower than the most naive solution in literally any other language that LLVM can compile.
public class SectionRenderDataUnsafe {
    private static final int NUM_FACINGS = ModelQuadFacing.COUNT;

    /**
     * {@return the number of index buffer elements needed to draw a span of {@code vertexSpan} vertices}
     */
    public static int elementsForVertices(int vertexSpan, ChunkPrimitiveType primitiveType) {
        return (vertexSpan / primitiveType.getVerticesPerPrimitive()) * primitiveType.getIndexBufferElementsPerPrimitive();
    }

    /**
     * The inverse of {@link #elementsForVertices}.
     */
    public static int verticesForElements(int elementCount, ChunkPrimitiveType primitiveType) {
        return (elementCount / primitiveType.getIndexBufferElementsPerPrimitive()) * primitiveType.getVerticesPerPrimitive();
    }

    /**
     * Memory layouts for one section's mesh metadata. The selected strategy is fixed for a storage instance and is
     * determined by whether its render pass requires translucency sorting.
     */
    public enum Strategy {
        /**
         * Stores an explicit vertex offset, element count, and index offset for every facing.
         *
         * <pre>
         * u64 slice_mask;
         * struct { u32 vertex_offset; u32 element_count; u32 index_offset; } facings[7];
         * </pre>
         */
        FULL {
            private static final long OFFSET_SLICE_RANGES = 8;
            private static final long DATA_PER_FACING_SIZE = 12;

            private static long pVertexOffset(long pointer, int facing) {
                return pointer + OFFSET_SLICE_RANGES + ((long) facing * DATA_PER_FACING_SIZE);
            }

            private static long pElementCount(long pointer, int facing) {
                return pVertexOffset(pointer, facing) + 4L;
            }

            private static long pIndexOffset(long pointer, int facing) {
                return pVertexOffset(pointer, facing) + 8L;
            }

            @Override
            public long getStride() {
                return OFFSET_SLICE_RANGES + (DATA_PER_FACING_SIZE * NUM_FACINGS);
            }

            @Override
            public int getVertexOffset(long pointer, int facing) {
                return LWJGL.memGetInt(pVertexOffset(pointer, facing));
            }

            @Override
            public int getElementCount(long pointer, int facing, ChunkPrimitiveType primitiveType) {
                return LWJGL.memGetInt(pElementCount(pointer, facing));
            }

            @Override
            public int getIndexOffset(long pointer, int facing) {
                return LWJGL.memGetInt(pIndexOffset(pointer, facing));
            }

            @Override
            public int getRunVertexEnd(long pointer, int lastFacing, ChunkPrimitiveType primitiveType) {
                return LWJGL.memGetInt(pVertexOffset(pointer, lastFacing))
                        + verticesForElements(LWJGL.memGetInt(pElementCount(pointer, lastFacing)), primitiveType);
            }

            @Override
            public int getSliceMask(long heap, int index) {
                return LWJGL.memGetInt(this.heapPointer(heap, index));
            }

            @Override
            protected void setSliceMask(long heap, int index, int value) {
                LWJGL.memPutInt(this.heapPointer(heap, index), value);
            }

            @Override
            protected int writeMeshes(long pointer, int vertexOffset, int indexOffset,
                                      Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType) {
                int sliceMask = 0;

                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    int vertexCount = vertexCountOf(ranges, facing);
                    int elementCount = elementsForVertices(vertexCount, primitiveType);

                    LWJGL.memPutInt(pVertexOffset(pointer, facing), vertexOffset);
                    LWJGL.memPutInt(pElementCount(pointer, facing), elementCount);
                    LWJGL.memPutInt(pIndexOffset(pointer, facing), indexOffset);

                    if (vertexCount > 0) {
                        sliceMask |= 1 << facing;
                    }

                    vertexOffset += vertexCount;
                    indexOffset += elementCount * 4;
                }

                return sliceMask;
            }

            @Override
            public void writeIndexOffsets(long pointer, int indexOffset, ChunkPrimitiveType primitiveType) {
                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    LWJGL.memPutInt(pIndexOffset(pointer, facing), indexOffset);
                    indexOffset += LWJGL.memGetInt(pElementCount(pointer, facing)) * 4;
                }
            }

            @Override
            public void rebase(long pointer, int vertexOffset, int indexOffset, ChunkPrimitiveType primitiveType) {
                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    LWJGL.memPutInt(pVertexOffset(pointer, facing), vertexOffset);
                    LWJGL.memPutInt(pIndexOffset(pointer, facing), indexOffset);

                    int elementCount = LWJGL.memGetInt(pElementCount(pointer, facing));
                    vertexOffset += verticesForElements(elementCount, primitiveType);
                    indexOffset += elementCount * 4;
                }
            }
        },
        /**
         * Stores only vertex fence posts for unsorted passes. Slice masks are kept in a one-byte-per-section header.
         *
         * <pre>
         * header: u8 slice_mask[RenderRegion.REGION_SIZE];
         * row:    u32 posts[8];
         * </pre>
         */
        COMPACT {
            private static final long DATA_OFFSET = RenderRegion.REGION_SIZE;
            private static final int NUM_POSTS = NUM_FACINGS + 1;

            private static long pPost(long pointer, int post) {
                return pointer + ((long) post << 2);
            }

            @Override
            public long getStride() {
                return Integer.BYTES * (long) NUM_POSTS;
            }

            @Override
            protected long getHeaderSize() {
                return DATA_OFFSET;
            }

            @Override
            public int getSliceMask(long heap, int index) {
                return LWJGL.memGetByte(heap + index) & 0xFF;
            }

            @Override
            protected void setSliceMask(long heap, int index, int value) {
                LWJGL.memPutByte(heap + index, (byte) value);
            }

            @Override
            public int getVertexOffset(long pointer, int facing) {
                return LWJGL.memGetInt(pPost(pointer, facing));
            }

            @Override
            public int getElementCount(long pointer, int facing, ChunkPrimitiveType primitiveType) {
                int start = LWJGL.memGetInt(pPost(pointer, facing));
                int end = LWJGL.memGetInt(pPost(pointer, facing + 1));

                return elementsForVertices(end - start, primitiveType);
            }

            @Override
            public int getIndexOffset(long pointer, int facing) {
                return 0;
            }

            @Override
            public int getRunVertexEnd(long pointer, int lastFacing, ChunkPrimitiveType primitiveType) {
                return LWJGL.memGetInt(pPost(pointer, lastFacing + 1));
            }

            @Override
            protected int writeMeshes(long pointer, int vertexOffset, int indexOffset,
                                      Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType) {
                int sliceMask = 0;

                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    int vertexCount = vertexCountOf(ranges, facing);

                    LWJGL.memPutInt(pPost(pointer, facing), vertexOffset);

                    if (vertexCount > 0) {
                        sliceMask |= 1 << facing;
                    }

                    vertexOffset += vertexCount;
                }

                LWJGL.memPutInt(pPost(pointer, NUM_POSTS - 1), vertexOffset);

                return sliceMask;
            }

            @Override
            public void writeIndexOffsets(long pointer, int indexOffset, ChunkPrimitiveType primitiveType) {
                throw new UnsupportedOperationException("COMPACT metadata has no index offsets");
            }

            @Override
            public void rebase(long pointer, int vertexOffset, int indexOffset, ChunkPrimitiveType primitiveType) {
                int delta = vertexOffset - LWJGL.memGetInt(pPost(pointer, 0));

                if (delta == 0) {
                    return;
                }

                for (int post = 0; post < NUM_POSTS; post++) {
                    long postPointer = pPost(pointer, post);
                    LWJGL.memPutInt(postPointer, LWJGL.memGetInt(postPointer) + delta);
                }
            }
        };

        /**
         * {@return the byte stride of one metadata row}
         */
        public abstract long getStride();

        /**
         * {@return the vertex offset of one facing}
         */
        public abstract int getVertexOffset(long pointer, int facing);

        /**
         * @param primitiveType ignored by layouts which store explicit element counts
         * @return the number of index elements for one facing
         */
        public abstract int getElementCount(long pointer, int facing, ChunkPrimitiveType primitiveType);

        /**
         * {@return the byte offset of one facing's indices, or zero for the shared index buffer}
         */
        public abstract int getIndexOffset(long pointer, int facing);

        /**
         * {@return the exclusive vertex end of a facing run}
         */
        public abstract int getRunVertexEnd(long pointer, int lastFacing, ChunkPrimitiveType primitiveType);

        /**
         * Populates one metadata row and returns its slice mask. Ranges must be contiguous in facing order.
         */
        protected abstract int writeMeshes(long pointer, int vertexOffset, int indexOffset,
                                           Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType);

        /**
         * Rewrites all index offsets after a sorted index buffer is replaced.
         */
        public abstract void writeIndexOffsets(long pointer, int indexOffset, ChunkPrimitiveType primitiveType);

        /**
         * Rewrites a row after its vertex or sorted index allocation moves.
         */
        public abstract void rebase(long pointer, int vertexOffset, int indexOffset, ChunkPrimitiveType primitiveType);

        /**
         * Reads the slice mask for one section from the strategy-specific heap layout.
         */
        public abstract int getSliceMask(long heap, int index);

        /**
         * Writes the slice mask for one section to the strategy-specific heap layout.
         */
        protected abstract void setSliceMask(long heap, int index, int value);

        /**
         * {@return the number of header bytes before the row area}
         */
        protected long getHeaderSize() {
            return 0;
        }

        /**
         * {@return the first row address in the strategy-specific heap}
         */
        public final long getRowBasePointer(long heap) {
            return heap + this.getHeaderSize();
        }

        /**
         * Allocates and clears metadata for all sections in one render region.
         */
        public final long allocateHeap() {
            long size = this.getHeaderSize() + ((long) RenderRegion.REGION_SIZE * this.getStride());
            long pointer = LWJGL.nmemAlignedAlloc(64, size);

            if (pointer != 0) {
                LWJGL.memSet(pointer, 0x0, size);
            }

            return pointer;
        }

        /**
         * Releases a strategy-specific metadata allocation.
         */
        public final void freeHeap(long pointer) {
            LWJGL.nmemAlignedFree(pointer);
        }

        /**
         * {@return the address of one section's metadata row}
         */
        public final long heapPointer(long heap, int index) {
            return this.getRowBasePointer(heap) + ((long) index * this.getStride());
        }

        /**
         * Writes a row and its slice mask as one metadata update.
         */
        public final void writeMeshesAndSliceMask(long heap, int index, int vertexOffset, int indexOffset,
                                                  Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType) {
            int sliceMask = this.writeMeshes(this.heapPointer(heap, index), vertexOffset, indexOffset, ranges, primitiveType);
            this.setSliceMask(heap, index, sliceMask);
        }

        /**
         * Clears a row and its slice mask as one metadata update.
         */
        public final void clearRow(long heap, int index) {
            this.setSliceMask(heap, index, 0);
            LWJGL.memSet(this.heapPointer(heap, index), 0x0, this.getStride());
        }

        private static int vertexCountOf(Map<ModelQuadFacing, VertexRange> ranges, int facing) {
            VertexRange range = ranges.get(ModelQuadFacing.VALUES[facing]);
            return range != null ? range.vertexCount() : 0;
        }
    }
}
