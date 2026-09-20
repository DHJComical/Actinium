package dhj.embeddedt.embeddium.impl.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongHeaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import dhj.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import dhj.embeddedt.embeddium.impl.render.chunk.PackedSectionMetadata;
import dhj.embeddedt.embeddium.impl.render.chunk.RenderSection;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumMap;
import dhj.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import dhj.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import dhj.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.joml.Vector3ic;

public class VisibleChunkCollector implements OcclusionCuller.Visitor {
    @Getter(AccessLevel.PACKAGE)
    private final ObjectArrayList<ChunkRenderList> sortedRenderLists;
    private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> sortedRebuildLists;
    private final int[] rebuildQueueOverflowCounts;
    private final ChunkRenderList[] renderListsByRegion;

    private final LongArrayList[] rebuildCandidates;

    private final int frame;

    private final int targetQueueSize;

    private final SectionLattice lattice;

    // Camera position in blocks, used as the origin for the rebuild ordering
    private final int cameraX, cameraY, cameraZ;

    private boolean hasAdditionalUpdates;
    private boolean rebuildListsFinished;

    public VisibleChunkCollector(SectionLattice lattice, int frame, int regionIdsLength, int targetQueueSize, Vector3ic cameraBlockPos) {
        this.lattice = lattice;
        this.frame = frame;

        this.sortedRenderLists = new ObjectArrayList<>();
        this.sortedRebuildLists = new EnumMap<>(ChunkUpdateType.class);
        this.rebuildQueueOverflowCounts = new int[ChunkUpdateType.VALUES.length];
        this.rebuildCandidates = new LongArrayList[ChunkUpdateType.VALUES.length];
        this.targetQueueSize = targetQueueSize;
        this.renderListsByRegion = new ChunkRenderList[regionIdsLength];
        this.cameraX = cameraBlockPos.x();
        this.cameraY = cameraBlockPos.y();
        this.cameraZ = cameraBlockPos.z();

        for (var type : ChunkUpdateType.VALUES) {
            this.sortedRebuildLists.put(type, new ArrayDeque<>());
        }
    }

    private ChunkRenderList createRenderList(RenderRegion region) {
        ChunkRenderList renderList = new ChunkRenderList(region);
        this.sortedRenderLists.add(renderList);
        this.renderListsByRegion[region.getId()] = renderList;
        return renderList;
    }

    @Override
    public void visit(int latticeIndex, int regionId, int sectionIndex, int chunkX, int chunkY, int chunkZ, int meta, boolean visible) {
        // Note: even if a section does not have render objects, we must ensure the render list is initialized and put
        // into the sorted queue of lists, so that we maintain the correct order of draw calls.
        ChunkRenderList renderList = this.renderListsByRegion[regionId];

        if (renderList == null) {
            renderList = this.createRenderList(this.lattice.sectionAt(latticeIndex).getRegion());
        }

        if (visible) {
            int visualsFlags = PackedSectionMetadata.getCompactVisualsFlags(meta);
            if (visualsFlags != 0) {
                renderList.add(sectionIndex, visualsFlags);
            }

            ChunkUpdateType type = PackedSectionMetadata.getCompactPendingUpdate(meta);

            // Skip sections with an in-flight build to avoid redundant work. This is an advisory
            // check only: submitRebuildTasks() will validate getPendingUpdate() independently before
            // scheduling, so a stale read here cannot cause a double submission.
            if (type != null && !PackedSectionMetadata.isCompactBuildInFlight(meta)) {
                this.addRebuildCandidate(latticeIndex, chunkX, chunkY, chunkZ, type);
            }
        }
    }

    private void addRebuildCandidate(int latticeIndex, int chunkX, int chunkY, int chunkZ, ChunkUpdateType type) {
        long dx = ((long) chunkX << 4) + 8 - this.cameraX;
        long dy = ((long) chunkY << 4) + 8 - this.cameraY;
        long dz = ((long) chunkZ << 4) + 8 - this.cameraZ;
        long distanceSq = Math.min(dx * dx + dy * dy + dz * dz, Integer.MAX_VALUE);

        var candidates = this.rebuildCandidates[type.ordinal()];

        if (candidates == null) {
            candidates = this.rebuildCandidates[type.ordinal()] = new LongArrayList();
        }

        candidates.add((distanceSq << 32) | (latticeIndex & 0xFFFFFFFFL));
    }

    public void finishRebuildLists() {
        if (this.rebuildListsFinished) {
            throw new IllegalStateException("Rebuild lists already finished");
        }

        this.rebuildListsFinished = true;

        var types = ChunkUpdateType.VALUES;

        for (int i = 0; i < types.length; i++) {
            var candidates = this.rebuildCandidates[i];

            if (candidates == null) {
                continue;
            }

            var type = types[i];
            long[] keys = candidates.elements();
            int count = candidates.size();

            // Do not limit the queue size for rebuilds
            int limit = type == ChunkUpdateType.INITIAL_BUILD ? Math.min(count, this.targetQueueSize) : count;

            // Only use max-heap selection when we need 1/3 or less of the full list
            if (limit * 3 < count) {
                keys = selectSmallest(keys, count, limit);
            } else {
                Arrays.sort(keys, 0, count);
            }

            var queue = this.sortedRebuildLists.get(type);

            for (int j = 0; j < limit; j++) {
                queue.add(this.lattice.sectionAt((int) keys[j]));
            }

            if (limit < count) {
                this.rebuildQueueOverflowCounts[i] += count - limit;
                this.hasAdditionalUpdates = true;
            }

            this.rebuildCandidates[i] = null;
        }
    }

    /**
     * {@return the {@code k} smallest of the first {@code count} keys, in ascending order}
     *
     * <p>Uses a bounded max-heap: once it holds {@code k} keys, a candidate larger than the heap's root is rejected
     * with a single comparison, so for the common case where most candidates are farther than the cutoff this runs
     * in roughly linear time instead of the {@code n log n} of a full sort.</p>
     */
    private static long[] selectSmallest(long[] keys, int count, int k) {
        var comparator = LongComparators.OPPOSITE_COMPARATOR;

        long[] heap = Arrays.copyOf(keys, k);
        LongHeaps.makeHeap(heap, k, comparator);

        for (int i = k; i < count; i++) {
            long key = keys[i];

            if (key < heap[0]) {
                heap[0] = key;
                LongHeaps.downHeap(heap, k, 0, comparator);
            }
        }

        Arrays.sort(heap);

        return heap;
    }

    public SortedRenderLists createRenderLists() {
        return new SortedRenderLists(this.sortedRenderLists);
    }

    public ChunkRebuildLists getRebuildLists() {
        if (!this.rebuildListsFinished) {
            throw new IllegalStateException("finishRebuildLists() must be called on the search thread first");
        }

        EnumMap<ChunkUpdateType, Integer> overflowCounts = new EnumMap<>(ChunkUpdateType.class);
        if (this.hasAdditionalUpdates) {
            var values = ChunkUpdateType.VALUES;
            for (int i = 0; i < values.length; i++) {
                if (this.rebuildQueueOverflowCounts[i] != 0) {
                    overflowCounts.put(values[i], this.rebuildQueueOverflowCounts[i]);
                }
            }
        }
        return new ChunkRebuildLists(this.sortedRebuildLists, this.hasAdditionalUpdates, overflowCounts);
    }
}
