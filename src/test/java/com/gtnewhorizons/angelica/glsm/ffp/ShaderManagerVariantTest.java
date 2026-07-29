package com.gtnewhorizons.angelica.glsm.ffp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderManagerVariantTest {

    @Test
    void matchesOnlyTheSignificantVertexAndFragmentKeyData() {
        long[] currentFragmentKey = { 11L, 22L, 99L, 99L };

        assertTrue(ShaderManager.isCurrentVariant(
            7L,
            currentFragmentKey,
            2,
            7L,
            new long[] { 11L, 22L, 0L, 0L },
            2
        ));
        assertFalse(ShaderManager.isCurrentVariant(
            7L,
            currentFragmentKey,
            2,
            8L,
            new long[] { 11L, 22L, 0L, 0L },
            2
        ));
        assertFalse(ShaderManager.isCurrentVariant(
            7L,
            currentFragmentKey,
            2,
            7L,
            new long[] { 11L, 23L, 0L, 0L },
            2
        ));
        assertFalse(ShaderManager.isCurrentVariant(
            7L,
            currentFragmentKey,
            2,
            7L,
            new long[] { 11L, 22L, 33L, 0L },
            3
        ));
    }
}
