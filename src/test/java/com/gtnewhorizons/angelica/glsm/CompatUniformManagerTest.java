package com.gtnewhorizons.angelica.glsm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatUniformManagerTest {

    @Test
    void findsLightUniformWhenTheFirstStructMemberIsOptimizedOut() {
        int[] locations = absentLocations();
        locations[CompatUniformManager.LOC_LIGHT_BASE + CompatUniformManager.LF_POSITION] = 12;

        assertTrue(CompatUniformManager.hasUniformLocation(
            locations,
            CompatUniformManager.LOC_LIGHT_BASE,
            2 * CompatUniformManager.LIGHT_FIELDS
        ));
    }

    @Test
    void findsMaterialUniformWhenEmissionIsOptimizedOut() {
        int[] locations = absentLocations();
        locations[CompatUniformManager.LOC_MAT_BASE + CompatUniformManager.MF_SHININESS] = 27;

        assertTrue(CompatUniformManager.hasUniformLocation(
            locations,
            CompatUniformManager.LOC_MAT_BASE,
            CompatUniformManager.MAT_FIELDS
        ));
    }

    @Test
    void rejectsUniformRangesWithoutActiveLocations() {
        int[] locations = absentLocations();

        assertFalse(CompatUniformManager.hasUniformLocation(
            locations,
            CompatUniformManager.LOC_LIGHT_BASE,
            2 * CompatUniformManager.LIGHT_FIELDS
        ));
        assertFalse(CompatUniformManager.hasUniformLocation(
            locations,
            CompatUniformManager.LOC_MAT_BASE,
            CompatUniformManager.MAT_FIELDS
        ));
    }

    private static int[] absentLocations() {
        int[] locations = new int[CompatUniformManager.LOC_COUNT];
        Arrays.fill(locations, -1);
        return locations;
    }
}
