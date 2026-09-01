package org.embeddedt.embeddium.impl.biome;

import org.embeddedt.embeddium.impl.util.position.PositionalSupplier;
import org.embeddedt.embeddium.impl.util.position.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BiomeColorCacheTest {
    private static final Object BIOME = new Object();
    private static final Integer RESOLVER = 1;

    /**
     * A section far away from the world origin so that the world coordinates fed into the cache are clearly
     * distinguishable from relative buffer indices. With blendRadius = 0 the cache covers
     * x [94, 113], y [62, 81], z [190, 209]; the radius only widens the X/Z ranges.
     */
    private static final SectionPos SECTION = new SectionPos(6, 4, 12);

    private static long key(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 20) ^ z;
    }

    /**
     * Creates a cache whose resolved color is a deterministic function of the world coordinates passed down
     * by {@code updateColorBuffers} (the abstract {@code resolveColor} parameters receive world coordinates).
     */
    private static BiomeColorCache<Object, Integer> createCoordinateCache(int blendRadius, PositionalSupplier<Object> biomes) {
        return new BiomeColorCache<>(biomes, blendRadius) {
            @Override
            protected int resolveColor(Integer resolver, Object biome, int relativeX, int relativeY, int relativeZ) {
                return (relativeX * 31) + (relativeY * 7) + relativeZ;
            }
        };
    }

    @Test
    void defaultHookIsIdentity() {
        var cache = createCoordinateCache(0, (x, y, z) -> BIOME);
        cache.update(SECTION);

        // Without any override the cached color must be exactly the resolved color
        assertEquals(100 * 31 + 70 * 7 + 200, cache.getColor(RESOLVER, 100, 70, 200));
        assertEquals(105 * 31 + 70 * 7 + 200, cache.getColor(RESOLVER, 105, 70, 200));
        assertEquals(96 * 31 + 70 * 7 + 200, cache.getColor(RESOLVER, 96, 70, 200));
    }

    @Test
    void postProcessHookTransformsCachedColor() {
        var cache = new BiomeColorCache<Object, Integer>((x, y, z) -> BIOME, 0) {
            @Override
            protected int resolveColor(Integer resolver, Object biome, int relativeX, int relativeY, int relativeZ) {
                return (relativeX * 31) + (relativeY * 7) + relativeZ;
            }

            @Override
            protected boolean shouldPostProcessColors() {
                return true;
            }

            @Override
            protected int postProcessColor(Integer resolver, int worldX, int worldY, int worldZ, int color) {
                return color ^ 0x00FF0000;
            }
        };
        cache.update(SECTION);

        // The cached color must be the resolved color run through the post-process hook
        assertEquals((100 * 31 + 70 * 7 + 200) ^ 0x00FF0000, cache.getColor(RESOLVER, 100, 70, 200));
        assertEquals((110 * 31 + 70 * 7 + 200) ^ 0x00FF0000, cache.getColor(RESOLVER, 110, 70, 200));
    }

    @Test
    void postProcessRunsAfterBlur() {
        Map<Long, Integer> postProcessInputs = new HashMap<>();

        var cache = new BiomeColorCache<Object, Integer>((x, y, z) -> BIOME, 2) {
            @Override
            protected int resolveColor(Integer resolver, Object biome, int relativeX, int relativeY, int relativeZ) {
                // Square wave along X: interior pixels have neighbors of a different color, so the box blur
                // must change their value
                int red = Math.floorMod(relativeX, 4) < 2 ? 255 : 0;
                return 0xFF000000 | (red << 16);
            }

            @Override
            protected boolean shouldPostProcessColors() {
                return true;
            }

            @Override
            protected int postProcessColor(Integer resolver, int worldX, int worldY, int worldZ, int color) {
                postProcessInputs.put(key(worldX, worldY, worldZ), color);
                return color ^ 0x00FF0000;
            }
        };
        cache.update(SECTION);

        // Interior probe point: with radius 2 the buffer extends 4 blocks past the section plus the 2 block
        // neighbor margin, so x = 100 sits well inside the blurred region (x % 4 == 0 -> raw red channel 255)
        int probeX = 100, probeY = 70, probeZ = 200;
        int rawColor = 0xFF000000 | (255 << 16);

        int cachedColor = cache.getColor(RESOLVER, probeX, probeY, probeZ);

        Integer colorSeenByHook = postProcessInputs.get(key(probeX, probeY, probeZ));
        assertNotNull(colorSeenByHook);
        // The hook must observe the blurred color, not the directly resolved one
        assertNotEquals(rawColor, colorSeenByHook.intValue());
        // ...and the cached color is exactly the blurred color transformed by the hook
        assertEquals(colorSeenByHook.intValue() ^ 0x00FF0000, cachedColor);
        assertNotEquals(rawColor ^ 0x00FF0000, cachedColor);
    }

    @Test
    void postProcessRunsOnUniformColorSlice() {
        var cache = new BiomeColorCache<Object, Integer>((x, y, z) -> BIOME, 2) {
            @Override
            protected int resolveColor(Integer resolver, Object biome, int relativeX, int relativeY, int relativeZ) {
                // Every pixel of the layer resolves to the same color, taking the uniqueColor fast path
                // which skips the blur entirely
                return 0xFF00FF00;
            }

            @Override
            protected boolean shouldPostProcessColors() {
                return true;
            }

            @Override
            protected int postProcessColor(Integer resolver, int worldX, int worldY, int worldZ, int color) {
                return color ^ 0x000000FF;
            }
        };
        cache.update(SECTION);

        assertEquals(0xFF00FFFF, cache.getColor(RESOLVER, 100, 70, 200));
        assertEquals(0xFF00FFFF, cache.getColor(RESOLVER, 95, 75, 195));
    }
}
