package org.embeddedt.embeddium.impl.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionNode;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcclusionCullerProvisionalVisibilityTest {
    @Test
    void provisionalNodeAllowsTraversalToAdjacentSection() {
        TestGraph graph = createLinearGraph();

        Set<OcclusionNode> visibleNodes = findVisibleNodes(graph, 1);

        assertTrue(visibleNodes.contains(graph.destination));
    }

    @Test
    void nullVisibilityStopsTraversalToAdjacentSection() {
        TestGraph graph = createLinearGraph();
        graph.provisional.setVisibilityData(VisibilityEncoding.NULL);

        Set<OcclusionNode> visibleNodes = findVisibleNodes(graph, 2);

        assertFalse(visibleNodes.contains(graph.destination));
    }

    private static TestGraph createLinearGraph() {
        RenderRegion region = new RenderRegion(0, 0, 0, 0, null);
        OcclusionNode origin = createNode(region, 0, 0, 0);
        OcclusionNode provisional = createNode(region, 1, 0, 0);
        OcclusionNode destination = createNode(region, 2, 0, 0);

        connect(origin, GraphDirection.EAST, provisional);
        connect(provisional, GraphDirection.EAST, destination);

        Long2ReferenceOpenHashMap<OcclusionNode> nodes = new Long2ReferenceOpenHashMap<>();
        nodes.put(origin.positionAsLong(), origin);
        nodes.put(provisional.positionAsLong(), provisional);
        nodes.put(destination.positionAsLong(), destination);

        return new TestGraph(new OcclusionCuller(nodes, 0, 1), provisional, destination);
    }

    private static OcclusionNode createNode(RenderRegion region, int x, int y, int z) {
        return new OcclusionNode(new RenderSection(region, x, y, z));
    }

    private static void connect(OcclusionNode from, int direction, OcclusionNode to) {
        from.setAdjacentNode(direction, to);
        to.setAdjacentNode(GraphDirection.opposite(direction), from);
    }

    private static Set<OcclusionNode> findVisibleNodes(TestGraph graph, int frame) {
        Set<OcclusionNode> visibleNodes = new HashSet<>();
        Viewport viewport = new Viewport(
            (minX, minY, minZ, maxX, maxY, maxZ) -> true,
            new Vector3d(8.0, 8.0, 8.0)
        );

        graph.culler.findVisible((node, visible) -> {
            if (visible) {
                visibleNodes.add(node);
            }
        }, viewport, 64.0F, true, frame);

        return visibleNodes;
    }

    private record TestGraph(OcclusionCuller culler, OcclusionNode provisional, OcclusionNode destination) {
    }
}
