package org.embeddedt.embeddium.impl.render.chunk.map;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkTrackerReconcileTest {
    @Test
    void worldChunksUnknownToTrackerBecomeReadyOnceNeighborsSatisfyRadius() {
        ChunkTracker tracker = new ChunkTracker(1);
        ChunkTracker.Subscription subscription = tracker.subscribe();

        tracker.reconcile(loadedArea(10, 20));

        assertEquals(9, tracker.getReadyChunks().size());
        assertEquals(readyEvents("load:", 10, 20), sortedDrain(subscription));
    }

    @Test
    void trackerChunksMissingFromWorldAreRemoved() {
        ChunkTracker tracker = new ChunkTracker(1);
        ChunkTracker.Subscription subscription = tracker.subscribe();

        tracker.reconcile(loadedArea(10, 20));
        drain(subscription);
        assertEquals(9, tracker.getReadyChunks().size());

        tracker.reconcile(new LongOpenHashSet());

        assertTrue(tracker.getReadyChunks().isEmpty());
        assertEquals(readyEvents("unload:", 10, 20), sortedDrain(subscription));
    }

    @Test
    void reconcileIsIdempotent() {
        ChunkTracker tracker = new ChunkTracker(1);
        ChunkTracker.Subscription subscription = tracker.subscribe();

        LongSet loaded = loadedArea(10, 20);
        tracker.reconcile(loaded);
        assertEquals(9, drain(subscription).size());

        tracker.reconcile(loaded);
        tracker.reconcile(loaded);

        assertEquals(List.of(), drain(subscription));
        assertEquals(9, tracker.getReadyChunks().size());
    }

    @Test
    void isolatedChunkDoesNotBecomeReadyWithoutNeighbors() {
        ChunkTracker tracker = new ChunkTracker(1);
        ChunkTracker.Subscription subscription = tracker.subscribe();

        LongSet isolated = new LongOpenHashSet();
        isolated.add(PositionUtil.packChunk(10, 20));
        tracker.reconcile(isolated);

        assertTrue(tracker.getReadyChunks().isEmpty());
        assertEquals(List.of(), drain(subscription));

        // The isolated chunk was still registered: once its neighbors appear, readiness resolves normally.
        tracker.reconcile(loadedArea(10, 20));
        assertEquals(9, tracker.getReadyChunks().size());
        assertEquals(9, drain(subscription).size());
    }

    /**
     * A 5x5 square of loaded chunks around the center; with neighbor radius 1 exactly the
     * inner 3x3 chunks satisfy the readiness gate.
     */
    private static LongSet loadedArea(int centerX, int centerZ) {
        LongSet keys = new LongOpenHashSet();
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                keys.add(PositionUtil.packChunk(x, z));
            }
        }
        return keys;
    }

    private static List<String> readyEvents(String prefix, int centerX, int centerZ) {
        List<String> events = new ArrayList<>();
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                events.add(prefix + x + "," + z);
            }
        }
        events.sort(null);
        return events;
    }

    private static List<String> sortedDrain(ChunkTracker.Subscription subscription) {
        List<String> events = drain(subscription);
        events.sort(null);
        return events;
    }

    private static List<String> drain(ChunkTracker.Subscription subscription) {
        List<String> events = new ArrayList<>();
        subscription.forEachEvent(
            (x, z) -> events.add("load:" + x + "," + z),
            (x, z) -> events.add("unload:" + x + "," + z)
        );
        return events;
    }
}
