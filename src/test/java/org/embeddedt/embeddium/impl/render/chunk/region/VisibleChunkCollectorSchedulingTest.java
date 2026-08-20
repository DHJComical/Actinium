package org.embeddedt.embeddium.impl.render.chunk.region;

import org.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.lists.VisibleChunkCollector;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibleChunkCollectorSchedulingTest {
    @Test
    void capsInitialBuildQueueAndReportsOverflow() {
        RenderRegion region = new RenderRegion(0, 0, 0, 0, null);
        VisibleChunkCollector collector = new VisibleChunkCollector(0, 1, 1);

        collector.visit(node(region, 0, 0, 0, ChunkUpdateType.INITIAL_BUILD), true);
        collector.visit(node(region, 1, 0, 0, ChunkUpdateType.INITIAL_BUILD), true);

        var rebuildLists = collector.getRebuildLists();

        assertEquals(1, rebuildLists.byUpdateType().get(ChunkUpdateType.INITIAL_BUILD).size());
        assertTrue(rebuildLists.hasAdditionalUpdates());
        assertEquals(1, rebuildLists.queueOverflowCounts().get(ChunkUpdateType.INITIAL_BUILD));
    }

    @Test
    void keepsRebuildQueueWhenTargetIsZero() {
        RenderRegion region = new RenderRegion(0, 0, 0, 0, null);
        VisibleChunkCollector collector = new VisibleChunkCollector(0, 1, 0);

        collector.visit(node(region, 0, 0, 0, ChunkUpdateType.REBUILD), true);
        collector.visit(node(region, 1, 0, 0, ChunkUpdateType.REBUILD), true);

        var rebuildLists = collector.getRebuildLists();

        assertEquals(2, rebuildLists.byUpdateType().get(ChunkUpdateType.REBUILD).size());
        assertFalse(rebuildLists.hasAdditionalUpdates());
        assertEquals(0, rebuildLists.queueOverflowCounts().getOrDefault(ChunkUpdateType.REBUILD, 0));
    }

    private static OcclusionNode node(RenderRegion region, int x, int y, int z, ChunkUpdateType updateType) {
        RenderSection section = new RenderSection(region, x, y, z);
        section.setPendingUpdate(updateType);
        return new OcclusionNode(section);
    }
}
