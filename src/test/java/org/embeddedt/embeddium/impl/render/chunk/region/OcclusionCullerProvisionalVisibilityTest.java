package org.embeddedt.embeddium.impl.render.chunk.region;

import org.embeddedt.embeddium.impl.render.chunk.PackedSectionMetadata;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcclusionCullerProvisionalVisibilityTest {
    @Test
    void provisionalSectionVisibilityAllowsTraversalToAdjacentSection() {
        TestGraph graph = createLinearLattice(false);

        TraversalResult result = findVisibleSections(graph, 1);

        Set<Integer> expectedSections = Set.of(
                graph.origin.getSectionIndex(),
                graph.provisional.getSectionIndex(),
                graph.destination.getSectionIndex()
        );
        assertEquals(expectedSections, result.visitedSections());
        assertEquals(expectedSections, result.visibleSections());
    }

    @Test
    void nullVisibilityStopsTraversalToAdjacentSection() {
        TestGraph graph = createLinearLattice(true);

        TraversalResult result = findVisibleSections(graph, 2);

        Set<Integer> expectedSections = Set.of(
                graph.origin.getSectionIndex(),
                graph.provisional.getSectionIndex()
        );
        assertEquals(expectedSections, result.visitedSections());
        assertEquals(expectedSections, result.visibleSections());
        assertFalse(result.visitedSections().contains(graph.destination.getSectionIndex()));
    }

    private static TestGraph createLinearLattice(boolean nullProvisionalVisibility) {
        RenderRegion region = new RenderRegion(0, 0, 0, 0, null);
        RenderSection origin = createSection(region, 0, 0, 0, VisibilityEncoding.EVERYTHING);
        RenderSection provisional = createSection(region, 1, 0, 0, VisibilityEncoding.EVERYTHING);
        RenderSection destination = createSection(region, 2, 0, 0, VisibilityEncoding.EVERYTHING);

        if (nullProvisionalVisibility) {
            setVisibility(provisional, VisibilityEncoding.NULL);
        }

        SectionLattice lattice = new SectionLattice(0, 1, false);
        lattice.attach(origin);
        lattice.attach(provisional);
        lattice.attach(destination);
        lattice.ensureWindowCovers(new Vector3i(0, 0, 0), 64.0F);

        return new TestGraph(lattice, origin, provisional, destination);
    }

    private static RenderSection createSection(RenderRegion region, int x, int y, int z, long visibilityData) {
        RenderSection section = new RenderSection(region, x, y, z);
        setVisibility(section, visibilityData);
        return section;
    }

    private static void setVisibility(RenderSection section, long visibilityData) {
        BuiltRenderSectionData data = new BuiltRenderSectionData();
        data.visibilityData = visibilityData;
        section.setInfo(data);
    }

    private static TraversalResult findVisibleSections(TestGraph graph, int frame) {
        Map<Integer, RenderSection> sectionsByIndex = Map.of(
                graph.origin.getSectionIndex(), graph.origin,
                graph.provisional.getSectionIndex(), graph.provisional,
                graph.destination.getSectionIndex(), graph.destination
        );
        Set<Integer> visitedSections = new HashSet<>();
        Set<Integer> visibleSections = new HashSet<>();
        Viewport viewport = new Viewport(
            (minX, minY, minZ, maxX, maxY, maxZ) -> true,
            new Vector3d(8.0, 8.0, 8.0)
        );

        graph.lattice.findVisible((latticeIndex, regionId, sectionIndex, compactMeta, visible) -> {
            RenderSection section = graph.lattice.sectionAt(latticeIndex);
            assertNotNull(section);
            assertSame(sectionsByIndex.get(sectionIndex), section);
            assertEquals(section.getRegion().getId(), regionId);
            assertEquals(PackedSectionMetadata.toCompactMeta(section.getPackedMetadata()), compactMeta);
            assertTrue(visitedSections.add(sectionIndex));
            assertTrue(visible);
            visibleSections.add(sectionIndex);
        }, viewport, 64.0F, 1, true, true, frame);

        return new TraversalResult(Set.copyOf(visitedSections), Set.copyOf(visibleSections));
    }

    private record TestGraph(SectionLattice lattice,
                             RenderSection origin,
                             RenderSection provisional,
                             RenderSection destination) {
    }

    private record TraversalResult(Set<Integer> visitedSections, Set<Integer> visibleSections) {
    }
}
