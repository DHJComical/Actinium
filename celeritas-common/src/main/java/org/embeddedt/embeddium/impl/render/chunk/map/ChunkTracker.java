package org.embeddedt.embeddium.impl.render.chunk.map;

import it.unimi.dsi.fastutil.longs.*;
import org.embeddedt.embeddium.impl.util.PositionUtil;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ChunkTracker implements ClientChunkEventListener {
    private final Long2IntOpenHashMap chunkStatus = new Long2IntOpenHashMap();
    private final LongOpenHashSet chunkReady = new LongOpenHashSet();

    private final Set<Subscription> subscriptions = Collections.newSetFromMap(new IdentityHashMap<>());

    private int requiredNeighborRadius;

    public ChunkTracker() {
        this(1);
    }

    /**
     * Constructs a new chunk tracker.
     * @param requiredNeighborRadius the radius of chunks around a given chunk that must be available before the chunk
     *                               itself is considered loaded (0 requires no chunks to be loaded, 1 requires
     *                               all adjacent chunks). Note that a radius of 0 is guaranteed to produce incorrect
     *                               rendering of the edge chunks for blocks that rely on data from the adjacent chunk
     *                               (e.g. fences, fluids). Vanilla handles this by updating the chunks again as
     *                               neighbors load, but this wastes CPU time and looks bad
     */
    public ChunkTracker(int requiredNeighborRadius) {
        if (requiredNeighborRadius < 0) {
            throw new IllegalArgumentException("requiredNeighborRadius must be nonnegative");
        }
        this.requiredNeighborRadius = requiredNeighborRadius;
    }

    public synchronized void setRequiredNeighborRadius(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be nonnegative");
        }
        if (this.requiredNeighborRadius == radius) {
            return;
        }
        boolean fullUpdate = radius > this.requiredNeighborRadius;
        if (fullUpdate) {
            // The requirement is now stricter; so we must clear chunkReady
            var readyIterator = this.chunkReady.iterator();
            while (readyIterator.hasNext()) {
                long key = readyIterator.nextLong();
                this.publishChunkRemoved(key);
            }
            this.chunkReady.clear();
        }
        this.requiredNeighborRadius = radius;
        // Recompute status of each chunk; this will repopulate chunkReady
        var trackedChunksIterator = this.chunkStatus.keySet().iterator();
        while (trackedChunksIterator.hasNext()) {
            long pos = trackedChunksIterator.nextLong();
            if (!fullUpdate && this.chunkReady.contains(pos)) {
                continue;
            }
            var x = PositionUtil.unpackChunkX(pos);
            var z = PositionUtil.unpackChunkZ(pos);
            this.updateMerged(x, z);
        }
    }

    @Override
    public void updateMapCenter(int chunkX, int chunkZ) {

    }

    @Override
    public void updateLoadDistance(int loadDistance) {

    }

    @Override
    public synchronized void onChunkStatusAdded(int x, int z, int flags) {
        var key = PositionUtil.packChunk(x, z);

        var prev = this.chunkStatus.get(key);
        var cur = prev | flags;

        if (prev == cur) {
            return;
        }

        this.chunkStatus.put(key, cur);

        this.updateNeighbors(x, z);
    }

    @Override
    public synchronized void onChunkStatusRemoved(int x, int z, int flags) {
        var key = PositionUtil.packChunk(x, z);

        var prev = this.chunkStatus.get(key);
        int cur = prev & ~flags;

        if (prev == cur) {
            return;
        }

        if (cur == this.chunkStatus.defaultReturnValue()) {
            this.chunkStatus.remove(key);
        } else {
            this.chunkStatus.put(key, cur);
        }

        this.updateNeighbors(x, z);
    }

    /**
     * Reconciles the tracked state with the chunks actually loaded by the world.
     *
     * The normal load/unload event stream can miss transitions (e.g. chunk data written to a
     * chunk the tracker never saw load, or mods mutating the world's chunk map directly),
     * which leaves the tracker permanently out of sync with no way to recover: affected chunks
     * never satisfy the neighbor-readiness gate and are never published to subscribers. This
     * method diffs the tracked set against the world's authoritative loaded set and replays
     * the difference through the regular event methods, so neighbor-readiness gating and
     * subscription notifications behave exactly as if the events had arrived normally.
     *
     * @param actuallyLoadedChunkKeys packed coordinates of the chunks currently loaded in the
     *                                world; not modified by this method
     */
    public synchronized void reconcile(LongSet actuallyLoadedChunkKeys) {
        int unknown = this.chunkStatus.defaultReturnValue();

        // Collect the diff first: replaying the events mutates chunkStatus, which must not
        // happen while either set is being iterated.
        LongList missingFromTracker = null;
        var loadedIterator = actuallyLoadedChunkKeys.iterator();
        while (loadedIterator.hasNext()) {
            long key = loadedIterator.nextLong();
            if (this.chunkStatus.get(key) == unknown) {
                if (missingFromTracker == null) {
                    missingFromTracker = new LongArrayList();
                }
                missingFromTracker.add(key);
            }
        }

        LongList missingFromWorld = null;
        var trackedIterator = this.chunkStatus.keySet().iterator();
        while (trackedIterator.hasNext()) {
            long key = trackedIterator.nextLong();
            if (!actuallyLoadedChunkKeys.contains(key)) {
                if (missingFromWorld == null) {
                    missingFromWorld = new LongArrayList();
                }
                missingFromWorld.add(key);
            }
        }

        if (missingFromTracker != null) {
            for (int i = 0; i < missingFromTracker.size(); i++) {
                long key = missingFromTracker.getLong(i);
                this.onChunkStatusAdded(PositionUtil.unpackChunkX(key), PositionUtil.unpackChunkZ(key), ChunkStatus.FLAG_ALL);
            }
        }

        if (missingFromWorld != null) {
            for (int i = 0; i < missingFromWorld.size(); i++) {
                long key = missingFromWorld.getLong(i);
                this.onChunkStatusRemoved(PositionUtil.unpackChunkX(key), PositionUtil.unpackChunkZ(key), ChunkStatus.FLAG_ALL);
            }
        }
    }

    private void updateNeighbors(int x, int z) {
        int r = this.requiredNeighborRadius;
        for (int ox = -r; ox <= r; ox++) {
            for (int oz = -r; oz <= r; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    private void updateMerged(int x, int z) {
        long key = PositionUtil.packChunk(x, z);

        int r = this.requiredNeighborRadius;
        int flags = this.chunkStatus.get(key);

        for (int ox = -r; ox <= r; ox++) {
            for (int oz = -r; oz <= r; oz++) {
                flags &= this.chunkStatus.get(PositionUtil.packChunk(ox + x, oz + z));
            }
        }

        if (flags == ChunkStatus.FLAG_ALL) {
            if (this.chunkReady.add(key)) {
                this.publishChunkAdded(key);
            }
        } else {
            if (this.chunkReady.remove(key)) {
                this.publishChunkRemoved(key);
            }
        }
    }

    private void publishChunkAdded(long key) {
        for (Subscription subscription : this.subscriptions) {
            subscription.onChunkAdded(key);
        }
    }

    private void publishChunkRemoved(long key) {
        for (Subscription subscription : this.subscriptions) {
            subscription.onChunkRemoved(key);
        }
    }

    public synchronized LongCollection getReadyChunks() {
        return LongSets.unmodifiable(new LongOpenHashSet(this.chunkReady));
    }

    /**
     * Creates an independent event stream for one renderer.
     */
    public synchronized Subscription subscribe() {
        Subscription subscription = new Subscription(this);
        this.subscriptions.add(subscription);
        return subscription;
    }

    private synchronized LongCollection snapshotReadyChunks(Subscription subscription) {
        this.requireSubscribed(subscription);
        subscription.clearPendingEvents();
        return LongSets.unmodifiable(new LongOpenHashSet(this.chunkReady));
    }

    private synchronized PendingEvents drainEvents(Subscription subscription) {
        this.requireSubscribed(subscription);
        PendingEvents events = new PendingEvents(
            new LongOpenHashSet(subscription.unloadQueue),
            new LongOpenHashSet(subscription.loadQueue)
        );
        subscription.clearPendingEvents();
        return events;
    }

    private synchronized void unsubscribe(Subscription subscription) {
        this.requireSubscribed(subscription);
        this.subscriptions.remove(subscription);
        subscription.clearPendingEvents();
        subscription.subscribed = false;
    }

    private void requireSubscribed(Subscription subscription) {
        if (subscription.owner != this || !subscription.subscribed || !this.subscriptions.contains(subscription)) {
            throw new IllegalStateException("Chunk tracker subscription is not active");
        }
    }

    private record PendingEvents(LongCollection unloads, LongCollection loads) {
    }

    /**
     * Holds pending chunk events for exactly one world renderer.
     */
    public static final class Subscription {
        private final ChunkTracker owner;
        private final LongSet unloadQueue = new LongOpenHashSet();
        private final LongSet loadQueue = new LongOpenHashSet();
        private boolean subscribed = true;

        private Subscription(ChunkTracker owner) {
            this.owner = owner;
        }

        private void onChunkAdded(long key) {
            if (!this.unloadQueue.remove(key)) {
                this.loadQueue.add(key);
            }
        }

        private void onChunkRemoved(long key) {
            if (!this.loadQueue.remove(key)) {
                this.unloadQueue.add(key);
            }
        }

        private void clearPendingEvents() {
            this.unloadQueue.clear();
            this.loadQueue.clear();
        }

        /**
         * Captures the owner's complete ready set and discards events already represented by that snapshot.
         */
        public LongCollection snapshotReadyChunks() {
            return this.owner.snapshotReadyChunks(this);
        }

        /**
         * Delivers this renderer's pending events without consuming another renderer's stream.
         */
        public void forEachEvent(ChunkEventHandler loadEventHandler, ChunkEventHandler unloadEventHandler) {
            PendingEvents events = this.owner.drainEvents(this);
            forEachChunk(events.unloads, unloadEventHandler);
            forEachChunk(events.loads, loadEventHandler);
        }

        /**
         * Stops event delivery and releases this subscription from its owner.
         */
        public void unsubscribe() {
            this.owner.unsubscribe(this);
        }
    }

    public static void forEachChunk(LongCollection queue, ChunkEventHandler handler) {
        var iterator = queue.iterator();

        while (iterator.hasNext()) {
            var pos = iterator.nextLong();

            var x = PositionUtil.unpackChunkX(pos);
            var z = PositionUtil.unpackChunkZ(pos);

            handler.apply(x, z);
        }
    }

    public interface ChunkEventHandler {
        void apply(int x, int z);
    }
}
