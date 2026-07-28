package org.embeddedt.embeddium.impl.render.chunk.map;

import it.unimi.dsi.fastutil.longs.LongCollection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkTrackerSubscriptionTest {
    @Test
    void broadcastsEventsToRenderersRegardlessOfConsumptionOrder() {
        ChunkTracker tracker = new ChunkTracker(0);
        ChunkTracker.Subscription first = tracker.subscribe();
        ChunkTracker.Subscription second = tracker.subscribe();

        tracker.onChunkStatusAdded(2, 3, ChunkStatus.FLAG_ALL);
        assertEquals(List.of("load:2,3"), drain(first));
        assertEquals(List.of("load:2,3"), drain(second));

        tracker.onChunkStatusRemoved(2, 3, ChunkStatus.FLAG_ALL);
        assertEquals(List.of("unload:2,3"), drain(second));
        assertEquals(List.of("unload:2,3"), drain(first));
    }

    @Test
    void reloadSnapshotRebasesOnlyItsOwnPendingEvents() {
        ChunkTracker tracker = new ChunkTracker(0);
        ChunkTracker.Subscription reloading = tracker.subscribe();
        ChunkTracker.Subscription other = tracker.subscribe();

        tracker.onChunkStatusAdded(4, 5, ChunkStatus.FLAG_ALL);
        assertEquals(List.of("chunk:4,5"), chunks(reloading.snapshotReadyChunks()));
        assertEquals(List.of(), drain(reloading));
        assertEquals(List.of("load:4,5"), drain(other));

        tracker.onChunkStatusAdded(6, 7, ChunkStatus.FLAG_ALL);
        assertEquals(List.of("chunk:4,5", "chunk:6,7"), chunks(reloading.snapshotReadyChunks()).stream().sorted().toList());
        assertEquals(List.of(), drain(reloading));
        assertEquals(List.of("load:6,7"), drain(other));

        tracker.onChunkStatusRemoved(4, 5, ChunkStatus.FLAG_ALL);
        assertEquals(List.of("unload:4,5"), drain(reloading));
        assertEquals(List.of("unload:4,5"), drain(other));
    }

    @Test
    void unsubscribeStopsDeliveryWithoutAffectingOtherRenderers() {
        ChunkTracker tracker = new ChunkTracker(0);
        ChunkTracker.Subscription removed = tracker.subscribe();
        ChunkTracker.Subscription active = tracker.subscribe();

        removed.unsubscribe();
        tracker.onChunkStatusAdded(-2, 8, ChunkStatus.FLAG_ALL);

        assertThrows(IllegalStateException.class, () -> drain(removed));
        assertEquals(List.of("load:-2,8"), drain(active));
    }

    private static List<String> drain(ChunkTracker.Subscription subscription) {
        List<String> events = new ArrayList<>();
        subscription.forEachEvent(
            (x, z) -> events.add("load:" + x + "," + z),
            (x, z) -> events.add("unload:" + x + "," + z)
        );
        return events;
    }

    private static List<String> chunks(LongCollection chunks) {
        List<String> positions = new ArrayList<>();
        ChunkTracker.forEachChunk(chunks, (x, z) -> positions.add("chunk:" + x + "," + z));
        return positions;
    }
}
