package com.dhj.actinium.world;

import net.minecraft.init.Bootstrap;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the null-volume semantics of {@link WorldSlice#blockBoxContains}.
 *
 * <p>A {@link WorldSlice} is a reused snapshot: it is only valid between {@code copyData} and
 * {@code reset}. Renderers outside the chunk build pipeline may retain the slice and query it
 * later on the main thread (MalisisCore's {@code AnimatedRenderer} does this from
 * {@code RenderWorldLastEvent}, issue #57). A reset slice has a {@code null} volume and must be
 * treated as covering nothing — every query falls back instead of dereferencing a null box.</p>
 */
class WorldSliceBlockBoxContainsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        // WorldSlice's static initializer reads Blocks.AIR, which requires the block registry
        // to be bootstrapped first (same pattern as BiomeUniformsTest).
        Bootstrap.register();
    }
    @Test
    void nullVolumeCoversNothing() {
        // Reset slice: no coordinate is inside the volume, no NPE.
        assertFalse(WorldSlice.blockBoxContains(null, 0, 0, 0));
        assertFalse(WorldSlice.blockBoxContains(null, 16, 128, 16));
        assertFalse(WorldSlice.blockBoxContains(null, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    @Test
    void volumeContainsInteriorPoints() {
        StructureBoundingBox box = new StructureBoundingBox(0, 0, 0, 15, 255, 15);

        assertTrue(WorldSlice.blockBoxContains(box, 0, 0, 0));
        assertTrue(WorldSlice.blockBoxContains(box, 15, 255, 15));
        assertTrue(WorldSlice.blockBoxContains(box, 8, 64, 8));
    }

    @Test
    void volumeExcludesOutsidePoints() {
        StructureBoundingBox box = new StructureBoundingBox(0, 0, 0, 15, 255, 15);

        assertFalse(WorldSlice.blockBoxContains(box, -1, 0, 0));
        assertFalse(WorldSlice.blockBoxContains(box, 16, 0, 0));
        assertFalse(WorldSlice.blockBoxContains(box, 0, 256, 0));
        assertFalse(WorldSlice.blockBoxContains(box, 0, 0, 16));
    }

    @Test
    void volumeUsesInclusiveBounds() {
        StructureBoundingBox box = new StructureBoundingBox(10, 20, 30, 12, 22, 32);

        // Both min and max corners are inside (inclusive bounds, matching StructureBoundingBox.isVecInside).
        assertTrue(WorldSlice.blockBoxContains(box, 10, 20, 30));
        assertTrue(WorldSlice.blockBoxContains(box, 12, 22, 32));
        assertFalse(WorldSlice.blockBoxContains(box, 9, 22, 31));
        assertFalse(WorldSlice.blockBoxContains(box, 13, 22, 31));
    }
}
