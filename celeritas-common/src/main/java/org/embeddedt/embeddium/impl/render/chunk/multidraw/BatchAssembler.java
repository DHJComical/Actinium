package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.api.render.chunk.ChunkAnimationProvider;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.util.BitwiseMath;
import org.jetbrains.annotations.Nullable;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * Assembles the visible sections of one region into the shared multi-draw command arrays.
 */
public final class BatchAssembler {
    private static final int MASK_NOT_UNIFORM = -1;

    private static final int MODEL_UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
    private static final int MODEL_POS_X = ModelQuadFacing.POS_X.ordinal();
    private static final int MODEL_POS_Y = ModelQuadFacing.POS_Y.ordinal();
    private static final int MODEL_POS_Z = ModelQuadFacing.POS_Z.ordinal();
    private static final int MODEL_NEG_X = ModelQuadFacing.NEG_X.ordinal();
    private static final int MODEL_NEG_Y = ModelQuadFacing.NEG_Y.ordinal();
    private static final int MODEL_NEG_Z = ModelQuadFacing.NEG_Z.ordinal();

    private static final boolean DEBUG_BLOCK_FACE_CULLING = false;
    private static final int RUN_COUNT_SHIFT = 32;
    private static final int POINTER_SHIFT = Integer.numberOfTrailingZeros(LWJGL.getPointerSize());

    private BatchAssembler() {
    }

    @FunctionalInterface
    public interface AnimatedSectionConsumer {
        void accept(SectionRenderDataStorage storage, int sectionIndex, int slices,
                    float offsetX, float offsetY, float offsetZ);
    }

    /**
     * Appends the visible facings of one section to {@code batch}. FULL rows retain their
     * per-facing index offsets, while COMPACT rows use consecutive facing runs against the
     * shared index buffer at pointer zero.
     */
    public static void fillSection(MultiDrawBatch batch,
                                   SectionRenderDataStorage storage,
                                   int sectionIndex,
                                   int facingMask) {
        int slices = facingMask & storage.getSliceMask(sectionIndex);
        if (slices == 0) {
            return;
        }

        ChunkPrimitiveType primitiveType = storage.getPrimitiveType();
        SectionRenderDataUnsafe.Strategy strategy = storage.getStrategy();
        long meshData = storage.getDataPointer(sectionIndex);

        if (strategy == SectionRenderDataUnsafe.Strategy.FULL) {
            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                if ((slices & (1 << facing)) != 0) {
                    write(batch,
                            strategy.getVertexOffset(meshData, facing),
                            strategy.getElementCount(meshData, facing, primitiveType),
                            strategy.getIndexOffset(meshData, facing));
                }
            }
            return;
        }

        while (slices != 0) {
            int firstFacing = Integer.numberOfTrailingZeros(slices);
            int runLength = Integer.numberOfTrailingZeros(~(slices >>> firstFacing));
            int lastFacing = firstFacing + runLength - 1;
            int start = strategy.getVertexOffset(meshData, firstFacing);
            int end = strategy.getRunVertexEnd(meshData, lastFacing, primitiveType);

            write(batch, start, SectionRenderDataUnsafe.elementsForVertices(end - start, primitiveType), 0L);
            slices &= ~(((1 << runLength) - 1) << firstFacing);
        }
    }

    /**
     * Clears {@code batch} and assembles all commands for the supplied region and render pass.
     * Sorted passes consume FULL rows and retain their real index offsets. Unsorted passes consume COMPACT rows and
     * use the shared index buffer at pointer zero.
     */
    public static void fillRegion(MultiDrawBatch batch,
                                  RenderRegion region,
                                  SectionRenderDataStorage storage,
                                  ChunkRenderList renderList,
                                  CameraTransform camera,
                                  TerrainRenderPass pass,
                                  boolean useBlockFaceCulling) {
        fillRegion(batch, region, storage, renderList, camera, pass, useBlockFaceCulling,
                null, null, null);
    }

    /**
     * Assembles a region while diverting sections with an active animation to the supplied consumer.
     */
    public static void fillRegion(MultiDrawBatch batch,
                                  RenderRegion region,
                                  SectionRenderDataStorage storage,
                                  ChunkRenderList renderList,
                                  CameraTransform camera,
                                  TerrainRenderPass pass,
                                  boolean useBlockFaceCulling,
                                  @Nullable ChunkAnimationProvider animationProvider,
                                  @Nullable float[] animationOffsetBuffer,
                                  @Nullable AnimatedSectionConsumer animatedSectionConsumer) {
        batch.clear();

        int sectionCount = renderList.getSectionsWithGeometryCount();
        if (sectionCount == 0) {
            return;
        }

        byte[] sections = renderList.getSectionsWithGeometry();
        int cursor = pass.isReverseOrder() ? sectionCount - 1 : 0;
        int step = pass.isReverseOrder() ? -1 : 1;

        ChunkPrimitiveType primitiveType = storage.getPrimitiveType();

        if (animationProvider != null) {
            if (animationOffsetBuffer == null || animatedSectionConsumer == null) {
                throw new IllegalArgumentException("Animation assembly requires an offset buffer and consumer");
            }

            fillAnimatedRegion(batch, region, storage, sections, cursor, step, sectionCount, camera,
                    useBlockFaceCulling, animationProvider, animationOffsetBuffer, animatedSectionConsumer);
            return;
        }

        if (pass.isSorted()) {
            fillSorted(batch, storage, sections, cursor, step, sectionCount, primitiveType);
            return;
        }

        if (!useBlockFaceCulling) {
            fillSingleRun(batch, storage, sections, cursor, step, sectionCount, primitiveType,
                    0, ModelQuadFacing.COUNT - 1);
            return;
        }

        int uniformMask = uniformCullMask(region, camera);
        if (uniformMask == MASK_NOT_UNIFORM) {
            fillPerSectionRuns(batch, region, storage, sections, cursor, step, sectionCount, primitiveType, camera);
            return;
        }

        long runs = packRuns(uniformMask);
        int runCount = runCount(runs);
        if (runCount == 0) {
            return;
        }

        if (runCount == 1) {
            fillSingleRun(batch, storage, sections, cursor, step, sectionCount, primitiveType,
                    runFirst(runs, 0), runLast(runs, 0));
        } else {
            fillUniformRuns(batch, storage, sections, cursor, step, sectionCount, primitiveType, runs, runCount);
        }
    }

    private static void fillAnimatedRegion(MultiDrawBatch batch,
                                           RenderRegion region,
                                           SectionRenderDataStorage storage,
                                           byte[] sections,
                                           int cursor,
                                           int step,
                                           int sectionCount,
                                           CameraTransform camera,
                                           boolean useBlockFaceCulling,
                                           ChunkAnimationProvider animationProvider,
                                           float[] animationOffsetBuffer,
                                           AnimatedSectionConsumer animatedSectionConsumer) {
        int originX = region.getChunkX();
        int originY = region.getChunkY();
        int originZ = region.getChunkZ();

        for (int i = 0; i < sectionCount; i++) {
            int sectionIndex = Byte.toUnsignedInt(sections[cursor]);
            cursor += step;
            int slices = useBlockFaceCulling
                    ? getVisibleFaces(camera.intX, camera.intY, camera.intZ,
                    originX + LocalSectionIndex.unpackX(sectionIndex),
                    originY + LocalSectionIndex.unpackY(sectionIndex),
                    originZ + LocalSectionIndex.unpackZ(sectionIndex))
                    : ModelQuadFacing.ALL;
            slices &= storage.getSliceMask(sectionIndex);

            if (slices == 0) {
                continue;
            }

            var section = region.getSection(sectionIndex);
            if (section != null && animationProvider.getSectionOffset(section, animationOffsetBuffer)) {
                animatedSectionConsumer.accept(storage, sectionIndex, slices,
                        animationOffsetBuffer[0], animationOffsetBuffer[1], animationOffsetBuffer[2]);
            } else {
                fillSection(batch, storage, sectionIndex, slices);
            }
        }
    }

    /**
     * Returns one cull mask for the entire region when the two monotonic region corners agree.
     */
    private static int uniformCullMask(RenderRegion region, CameraTransform camera) {
        int minX = region.getChunkX();
        int minY = region.getChunkY();
        int minZ = region.getChunkZ();
        int maxX = minX + RenderRegion.REGION_WIDTH - 1;
        int maxY = minY + RenderRegion.REGION_HEIGHT - 1;
        int maxZ = minZ + RenderRegion.REGION_LENGTH - 1;

        int atMin = getVisibleFaces(camera.intX, camera.intY, camera.intZ, minX, minY, minZ);
        int atMax = getVisibleFaces(camera.intX, camera.intY, camera.intZ, maxX, maxY, maxZ);

        return atMin == atMax ? atMin : MASK_NOT_UNIFORM;
    }

    /**
     * Packs maximal consecutive set-bit runs as two four-bit facing values per run, followed by the run count.
     */
    private static long packRuns(int mask) {
        long runs = 0L;
        int count = 0;

        while (mask != 0) {
            int first = Integer.numberOfTrailingZeros(mask);
            int length = Integer.numberOfTrailingZeros(~(mask >>> first));
            int last = first + length - 1;
            int shift = count << 3;

            runs |= (long) first << shift;
            runs |= (long) last << (shift + 4);
            count++;
            mask &= ~(((1 << length) - 1) << first);
        }

        return runs | ((long) count << RUN_COUNT_SHIFT);
    }

    private static int runCount(long runs) {
        return (int) (runs >>> RUN_COUNT_SHIFT) & 0xF;
    }

    private static int runFirst(long runs, int run) {
        return (int) (runs >>> (run << 3)) & 0xF;
    }

    private static int runLast(long runs, int run) {
        return (int) (runs >>> ((run << 3) + 4)) & 0xF;
    }

    /**
     * Computes the visible model facings for one section. UNASSIGNED is always visible.
     */
    public static int getVisibleFaces(int originX, int originY, int originZ, int chunkX, int chunkY, int chunkZ) {
        int boundsMinX = chunkX << 4;
        int boundsMaxX = boundsMinX + 16;
        int boundsMinY = chunkY << 4;
        int boundsMaxY = boundsMinY + 16;
        int boundsMinZ = chunkZ << 4;
        int boundsMaxZ = boundsMinZ + 16;

        int planes = 1 << MODEL_UNASSIGNED;

        if (DEBUG_BLOCK_FACE_CULLING) {
            planes |= BitwiseMath.lessThan(originX, boundsMaxX + 3) << MODEL_POS_X;
            planes |= BitwiseMath.lessThan(originY, boundsMaxY + 3) << MODEL_POS_Y;
            planes |= BitwiseMath.lessThan(originZ, boundsMaxZ + 3) << MODEL_POS_Z;
            planes |= BitwiseMath.greaterThan(originX, boundsMinX - 3) << MODEL_NEG_X;
            planes |= BitwiseMath.greaterThan(originY, boundsMinY - 3) << MODEL_NEG_Y;
            planes |= BitwiseMath.greaterThan(originZ, boundsMinZ - 3) << MODEL_NEG_Z;
        } else {
            planes |= BitwiseMath.greaterThan(originX, boundsMinX - 3) << MODEL_POS_X;
            planes |= BitwiseMath.greaterThan(originY, boundsMinY - 3) << MODEL_POS_Y;
            planes |= BitwiseMath.greaterThan(originZ, boundsMinZ - 3) << MODEL_POS_Z;
            planes |= BitwiseMath.lessThan(originX, boundsMaxX + 3) << MODEL_NEG_X;
            planes |= BitwiseMath.lessThan(originY, boundsMaxY + 3) << MODEL_NEG_Y;
            planes |= BitwiseMath.lessThan(originZ, boundsMaxZ + 3) << MODEL_NEG_Z;
        }

        return planes;
    }

    private static void fillSingleRun(MultiDrawBatch batch,
                                      SectionRenderDataStorage storage,
                                      byte[] sections,
                                      int cursor,
                                      int step,
                                      int sectionCount,
                                      ChunkPrimitiveType primitiveType,
                                      int firstFacing,
                                      int lastFacing) {
        long rowBase = storage.getRowBasePointer();
        long stride = SectionRenderDataUnsafe.Strategy.COMPACT.getStride();

        for (int i = 0; i < sectionCount; i++) {
            int sectionIndex = Byte.toUnsignedInt(sections[cursor]);
            cursor += step;
            long meshData = rowBase + (long) sectionIndex * stride;
            int start = SectionRenderDataUnsafe.Strategy.COMPACT.getVertexOffset(meshData, firstFacing);
            int end = SectionRenderDataUnsafe.Strategy.COMPACT.getRunVertexEnd(meshData, lastFacing, primitiveType);

            write(batch, start, SectionRenderDataUnsafe.elementsForVertices(end - start, primitiveType), 0L);
        }
    }

    private static void fillUniformRuns(MultiDrawBatch batch,
                                        SectionRenderDataStorage storage,
                                        byte[] sections,
                                        int cursor,
                                        int step,
                                        int sectionCount,
                                        ChunkPrimitiveType primitiveType,
                                        long runs,
                                        int runCount) {
        long rowBase = storage.getRowBasePointer();
        long stride = SectionRenderDataUnsafe.Strategy.COMPACT.getStride();

        for (int i = 0; i < sectionCount; i++) {
            int sectionIndex = Byte.toUnsignedInt(sections[cursor]);
            cursor += step;
            long meshData = rowBase + (long) sectionIndex * stride;
            int sectionStart = batch.size;
            int previousEnd = 0;

            for (int run = 0; run < runCount; run++) {
                int firstFacing = runFirst(runs, run);
                int lastFacing = runLast(runs, run);
                int start = SectionRenderDataUnsafe.Strategy.COMPACT.getVertexOffset(meshData, firstFacing);
                int end = SectionRenderDataUnsafe.Strategy.COMPACT.getRunVertexEnd(meshData, lastFacing, primitiveType);

                previousEnd = writeUnsorted(batch, start, end, primitiveType, sectionStart, previousEnd);
            }
        }
    }

    private static void fillPerSectionRuns(MultiDrawBatch batch,
                                           RenderRegion region,
                                           SectionRenderDataStorage storage,
                                           byte[] sections,
                                           int cursor,
                                           int step,
                                           int sectionCount,
                                           ChunkPrimitiveType primitiveType,
                                           CameraTransform camera) {
        long rowBase = storage.getRowBasePointer();
        long stride = SectionRenderDataUnsafe.Strategy.COMPACT.getStride();
        int originX = region.getChunkX();
        int originY = region.getChunkY();
        int originZ = region.getChunkZ();

        for (int i = 0; i < sectionCount; i++) {
            int sectionIndex = Byte.toUnsignedInt(sections[cursor]);
            cursor += step;
            int mask = getVisibleFaces(camera.intX, camera.intY, camera.intZ,
                    originX + LocalSectionIndex.unpackX(sectionIndex),
                    originY + LocalSectionIndex.unpackY(sectionIndex),
                    originZ + LocalSectionIndex.unpackZ(sectionIndex));
            long meshData = rowBase + (long) sectionIndex * stride;
            int sectionStart = batch.size;
            int previousEnd = 0;

            do {
                int firstFacing = Integer.numberOfTrailingZeros(mask);
                int lastFacing = firstFacing + Integer.numberOfTrailingZeros(~(mask >>> firstFacing)) - 1;
                int start = SectionRenderDataUnsafe.Strategy.COMPACT.getVertexOffset(meshData, firstFacing);
                int end = SectionRenderDataUnsafe.Strategy.COMPACT.getRunVertexEnd(meshData, lastFacing, primitiveType);

                previousEnd = writeUnsorted(batch, start, end, primitiveType, sectionStart, previousEnd);
                mask &= -1 << (lastFacing + 1);
            } while (mask != 0);
        }
    }

    private static void fillSorted(MultiDrawBatch batch,
                                   SectionRenderDataStorage storage,
                                   byte[] sections,
                                   int cursor,
                                   int step,
                                   int sectionCount,
                                   ChunkPrimitiveType primitiveType) {
        long rowBase = storage.getRowBasePointer();
        long stride = SectionRenderDataUnsafe.Strategy.FULL.getStride();

        for (int i = 0; i < sectionCount; i++) {
            int sectionIndex = Byte.toUnsignedInt(sections[cursor]);
            cursor += step;
            long meshData = rowBase + (long) sectionIndex * stride;

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                write(batch,
                        SectionRenderDataUnsafe.Strategy.FULL.getVertexOffset(meshData, facing),
                        SectionRenderDataUnsafe.Strategy.FULL.getElementCount(meshData, facing, primitiveType),
                        SectionRenderDataUnsafe.Strategy.FULL.getIndexOffset(meshData, facing));
            }
        }
    }

    private static int writeUnsorted(MultiDrawBatch batch,
                                     int baseVertex,
                                     int endVertex,
                                     ChunkPrimitiveType primitiveType,
                                     int sectionStart,
                                     int previousEnd) {
        if (endVertex < baseVertex) {
            throw new IllegalStateException("Compact section vertex fences are not ordered");
        }

        int elementCount = SectionRenderDataUnsafe.elementsForVertices(endVertex - baseVertex, primitiveType);
        if (elementCount == 0) {
            return previousEnd;
        }

        if (batch.size > sectionStart && previousEnd == baseVertex) {
            long countPointer = batch.pElementCount + ((long) (batch.size - 1) << 2);
            int mergedCount = LWJGL.memGetInt(countPointer) + elementCount;
            LWJGL.memPutInt(countPointer, mergedCount);
            batch.maxElementCount = Math.max(batch.maxElementCount, mergedCount);
        } else {
            write(batch, baseVertex, elementCount, 0L);
        }

        return endVertex;
    }

    private static void write(MultiDrawBatch batch, int baseVertex, int elementCount, long elementPointer) {
        if (elementCount < 0) {
            throw new IllegalStateException("Negative multidraw element count");
        }
        if (elementCount == 0) {
            return;
        }

        int index = batch.size;
        if (index < 0 || index >= batch.capacity()) {
            throw new IllegalStateException("MultiDrawBatch capacity exceeded");
        }

        LWJGL.memPutInt(batch.pBaseVertex + ((long) index << 2), baseVertex);
        LWJGL.memPutInt(batch.pElementCount + ((long) index << 2), elementCount);
        LWJGL.memPutAddress(batch.pElementPointer + ((long) index << POINTER_SHIFT), elementPointer);

        batch.size = index + 1;
        batch.maxElementCount = Math.max(batch.maxElementCount, elementCount);
    }
}
