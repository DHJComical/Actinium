package com.dhj.actinium.compat.cavebiomes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the semantic-section-to-storage-index arithmetic used when reading
 * {@code Chunk.storageArrays} in CaveBiomesAPI worlds (issue #43: CaveBiomesAPI
 * reshapes storage to the extended -64..320 layout, so vanilla indexing meshes
 * underground sections at surface positions).
 */
class CaveBiomesHeightMappingTest {
    @Test
    void vanillaRangeIsIdentityMapping() {
        // No CaveBiomesAPI world: storage index equals the semantic section Y.
        assertEquals(0, CaveBiomesHeightMapping.storageSectionIndex(0, 0));
        assertEquals(4, CaveBiomesHeightMapping.storageSectionIndex(0, 4));
        assertEquals(15, CaveBiomesHeightMapping.storageSectionIndex(0, 15));
        assertEquals(-4, CaveBiomesHeightMapping.storageSectionIndex(0, -4));
    }

    @Test
    void defaultExtendedRangeShiftsByMinSection() {
        // Default -64..320 range: minSection = -4, storage index = sectionY + 4.
        // Surface section 0 (world Y 0..15) lives in storage slot 4, not slot 0.
        assertEquals(4, CaveBiomesHeightMapping.storageSectionIndex(-4, 0));
        // Surface-at-height section 4 (world Y 64..79) lives in storage slot 8.
        assertEquals(8, CaveBiomesHeightMapping.storageSectionIndex(-4, 4));
        // Top semantic section (world Y 240..255) lives in slot 19.
        assertEquals(19, CaveBiomesHeightMapping.storageSectionIndex(-4, 15));
        // Underground semantic sections below the vanilla floor map to the leading slots.
        assertEquals(0, CaveBiomesHeightMapping.storageSectionIndex(-4, -4));
        assertEquals(3, CaveBiomesHeightMapping.storageSectionIndex(-4, -1));
        // Highest extended slot (world Y 304..319) is slot 23.
        assertEquals(23, CaveBiomesHeightMapping.storageSectionIndex(-4, 19));
        assertEquals(20, CaveBiomesHeightMapping.storageSectionIndex(-4, 16));
    }

    @Test
    void otherConfiguredRangesTranslateByTheirOwnMinSection() {
        assertEquals(2, CaveBiomesHeightMapping.storageSectionIndex(-2, 0));
        assertEquals(7, CaveBiomesHeightMapping.storageSectionIndex(-2, 5));
    }

    @Test
    void minSectionIsDerivedFromMinY() {
        assertEquals(-4, CaveBiomesHeightMapping.minSection(-64));
        assertEquals(0, CaveBiomesHeightMapping.minSection(0));
        assertEquals(-2, CaveBiomesHeightMapping.minSection(-32));
    }
}