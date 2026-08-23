package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackedSectionMetadataTest {
    @Test
    void writesAndDecodesAllFieldsWhilePreservingOthers() {
        long packed = 0L;
        packed = PackedSectionMetadata.withVisibilityData(packed, VisibilityEncoding.EVERYTHING);
        packed = PackedSectionMetadata.withVisualsFlags(packed, 0b101);
        packed = PackedSectionMetadata.withPendingUpdate(packed, ChunkUpdateType.IMPORTANT_REBUILD);
        packed = PackedSectionMetadata.withBuildInFlight(packed, true);

        assertEquals(VisibilityEncoding.EVERYTHING, PackedSectionMetadata.getVisibilityData(packed));
        assertEquals(0b101, PackedSectionMetadata.getVisualsFlags(packed));
        assertEquals(ChunkUpdateType.IMPORTANT_REBUILD, PackedSectionMetadata.getPendingUpdate(packed));
        assertTrue(PackedSectionMetadata.isBuildInFlight(packed));

        long changedVisibility = PackedSectionMetadata.withVisibilityData(packed, VisibilityEncoding.NULL);
        assertEquals(VisibilityEncoding.NULL, PackedSectionMetadata.getVisibilityData(changedVisibility));
        assertEquals(0b101, PackedSectionMetadata.getVisualsFlags(changedVisibility));
        assertEquals(ChunkUpdateType.IMPORTANT_REBUILD, PackedSectionMetadata.getPendingUpdate(changedVisibility));
        assertTrue(PackedSectionMetadata.isBuildInFlight(changedVisibility));

        long changedVisuals = PackedSectionMetadata.withVisualsFlags(packed, 0b010);
        assertEquals(VisibilityEncoding.EVERYTHING, PackedSectionMetadata.getVisibilityData(changedVisuals));
        assertEquals(0b010, PackedSectionMetadata.getVisualsFlags(changedVisuals));
        assertEquals(ChunkUpdateType.IMPORTANT_REBUILD, PackedSectionMetadata.getPendingUpdate(changedVisuals));
        assertTrue(PackedSectionMetadata.isBuildInFlight(changedVisuals));

        long changedPending = PackedSectionMetadata.withPendingUpdate(packed, ChunkUpdateType.SORT);
        assertEquals(VisibilityEncoding.EVERYTHING, PackedSectionMetadata.getVisibilityData(changedPending));
        assertEquals(0b101, PackedSectionMetadata.getVisualsFlags(changedPending));
        assertEquals(ChunkUpdateType.SORT, PackedSectionMetadata.getPendingUpdate(changedPending));
        assertTrue(PackedSectionMetadata.isBuildInFlight(changedPending));

        long changedBuild = PackedSectionMetadata.withBuildInFlight(packed, false);
        assertEquals(VisibilityEncoding.EVERYTHING, PackedSectionMetadata.getVisibilityData(changedBuild));
        assertEquals(0b101, PackedSectionMetadata.getVisualsFlags(changedBuild));
        assertEquals(ChunkUpdateType.IMPORTANT_REBUILD, PackedSectionMetadata.getPendingUpdate(changedBuild));
        assertFalse(PackedSectionMetadata.isBuildInFlight(changedBuild));
    }

    @Test
    void compactMetadataMatchesPackedMetadata() {
        long packed = 0L;
        packed = PackedSectionMetadata.withVisibilityData(packed, VisibilityEncoding.EVERYTHING);
        packed = PackedSectionMetadata.withVisualsFlags(packed, 0b110);
        packed = PackedSectionMetadata.withPendingUpdate(packed, ChunkUpdateType.IMPORTANT_SORT);
        packed = PackedSectionMetadata.withBuildInFlight(packed, true);

        int compactMeta = PackedSectionMetadata.toCompactMeta(packed);

        assertEquals(PackedSectionMetadata.getVisualsFlags(packed),
                PackedSectionMetadata.getCompactVisualsFlags(compactMeta));
        assertEquals(PackedSectionMetadata.getPendingUpdate(packed),
                PackedSectionMetadata.getCompactPendingUpdate(compactMeta));
        assertEquals(PackedSectionMetadata.isBuildInFlight(packed),
                PackedSectionMetadata.isCompactBuildInFlight(compactMeta));
    }

    @Test
    void nullPendingUpdateRoundTripsThroughPackedAndCompactMetadata() {
        long packed = PackedSectionMetadata.withPendingUpdate(
                PackedSectionMetadata.withBuildInFlight(0L, true), null);

        assertNull(PackedSectionMetadata.getPendingUpdate(packed));
        assertNull(PackedSectionMetadata.getCompactPendingUpdate(PackedSectionMetadata.toCompactMeta(packed)));
    }
}
