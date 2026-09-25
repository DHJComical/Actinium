package com.dhj.actinium.world.biome;

import net.minecraft.world.biome.BiomeColorHelper;
import org.junit.jupiter.api.Test;

import com.dhj.actinium.runtime.ActiniumRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct logic verification of the biome color position noise (issue #56). The tests call
 * {@link BiomeColorNoise} itself — no source text checks, no reflection — and exercise the
 * guarantees the injection points rely on: determinism, a bounded smooth value range, alpha
 * preservation, bidirectional scaling around the mean, and per-resolver intensity routing.
 */
class BiomeColorNoiseTest {

    @Test
    void sampleIsDeterministicForTheSameCoordinate() {
        assertEquals(BiomeColorNoise.sample(1234.0F, -5678.0F), BiomeColorNoise.sample(1234.0F, -5678.0F), 0.0F);
        assertEquals(BiomeColorNoise.sample(0.0F, 0.0F), BiomeColorNoise.sample(0.0F, 0.0F), 0.0F);
        assertEquals(BiomeColorNoise.sample(-1.0F, 1.0F), BiomeColorNoise.sample(-1.0F, 1.0F), 0.0F);
    }

    @Test
    void sampleStaysWithinUnitRangeAndNeverProducesNaN() {
        for (int x = -10000; x <= 10000; x += 137) {
            for (int z = -10000; z <= 10000; z += 137) {
                float value = BiomeColorNoise.sample((float) x, (float) z);
                assertFalse(Float.isNaN(value), "NaN at " + x + ", " + z);
                assertTrue(value >= -1.0F && value <= 1.0F, "Out of range at " + x + ", " + z + ": " + value);
            }
        }
    }

    @Test
    void sampleIsSmoothBetweenNeighboringBlocks() {
        // Worst-case slope of the quintic fade is 1.875 at a cell midpoint; with a 16 block cell,
        // the 2:1 two-octave mix and the [-1, 1] remap, the theoretical bound for a one block step
        // is ~0.32. The 0.6 bound leaves headroom yet still fails hard if a discontinuity (e.g. a
        // tangent-style pole) ever creeps into the fade curve.
        for (int x = -2000; x <= 2000; x += 41) {
            for (int z = -2000; z <= 2000; z += 41) {
                float base = BiomeColorNoise.sample((float) x, (float) z);
                assertTrue(Math.abs(BiomeColorNoise.sample((float) x + 1.0F, (float) z) - base) < 0.6F,
                        "X step discontinuity at " + x + ", " + z);
                assertTrue(Math.abs(BiomeColorNoise.sample((float) x, (float) z + 1.0F) - base) < 0.6F,
                        "Z step discontinuity at " + x + ", " + z);
            }
        }
    }

    @Test
    void sampleVariesAcrossDistantPositions() {
        // Guards against a degenerate all-constant field.
        assertNotEquals(BiomeColorNoise.sample(0.0F, 0.0F), BiomeColorNoise.sample(5000.0F, 5000.0F));
        assertNotEquals(BiomeColorNoise.sample(-3210.0F, 777.0F), BiomeColorNoise.sample(911.0F, -44.0F));
        assertNotEquals(BiomeColorNoise.sample(12345.0F, -12345.0F), BiomeColorNoise.sample(-5432.0F, 5432.0F));
    }

    @Test
    void applyPreservesTheAlphaByte() {
        // Cached tint colors may be plain 0x00RRGGBB or full ARGB; the alpha byte must survive
        // unchanged in both cases.
        int withoutAlpha = BiomeColorNoise.apply(0x00804020, 0.08F, 0.5F);
        assertEquals(0x00, withoutAlpha >>> 24);

        int withAlpha = BiomeColorNoise.apply(0xFF804020, 0.08F, -0.5F);
        assertEquals(0xFF, withAlpha >>> 24);
    }

    @Test
    void applyIsIdentityWhenAmplitudeIsZeroOrNoiseIsNull() {
        assertEquals(0x001A2B3C, BiomeColorNoise.apply(0x001A2B3C, 0.0F, 0.75F));
        assertEquals(0x001A2B3C, BiomeColorNoise.apply(0x001A2B3C, -0.1F, 0.75F));
        assertEquals(0xFF1A2B3C, BiomeColorNoise.apply(0xFF1A2B3C, 0.08F, 0.0F));
    }

    @Test
    void applyScalesAroundTheMeanInBothDirections() {
        // Mid gray channels: 128 * 1.04 = 133.12 -> 133 (brighter), 128 * 0.96 = 122.88 -> 123
        // (darker). The multiplicative factor keeps the mean centered, unlike a darken-only mix.
        int midGray = 0x00808080;
        int brightened = BiomeColorNoise.apply(midGray, 0.08F, 0.5F);
        int darkened = BiomeColorNoise.apply(midGray, 0.08F, -0.5F);

        assertEquals(133, brightened & 0xFF);
        assertEquals(123, darkened & 0xFF);
        assertTrue(brightened > midGray);
        assertTrue(darkened < midGray);
    }

    @Test
    void applyClampsSaturatedChannelsInsteadOfWrapping() {
        int result = BiomeColorNoise.apply(0x00FFFFFF, 0.08F, 0.9F);
        assertEquals(0x00FFFFFF, result);
    }

    @Test
    void applyForResolverRoutesToTheConfiguredIntensityPerResolver() {
        // The test JVM has no game options file, so ActiniumRuntime degrades to read-only
        // defaults: the feature on, all three intensities at 0.08.
        var quality = ActiniumRuntime.options().quality;
        assertTrue(quality.useBiomeColorNoise);
        assertEquals(0.08F, quality.biomeColorNoiseGrassIntensity, 0.0F);
        assertEquals(0.08F, quality.biomeColorNoiseFoliageIntensity, 0.0F);
        assertEquals(0.08F, quality.biomeColorNoiseWaterIntensity, 0.0F);

        int x = 3141;
        int y = 64;
        int z = -2718;
        int grassColor = 0x0079C05A;
        int foliageColor = 0x00565E31;
        int waterColor = 0x003F76E4;

        assertEquals(BiomeColorNoise.apply(grassColor, quality.biomeColorNoiseGrassIntensity, BiomeColorNoise.sample(x, z)),
                BiomeColorNoise.applyForResolver(BiomeColorHelper.GRASS_COLOR, x, y, z, grassColor));
        assertEquals(BiomeColorNoise.apply(foliageColor, quality.biomeColorNoiseFoliageIntensity, BiomeColorNoise.sample(x, z)),
                BiomeColorNoise.applyForResolver(BiomeColorHelper.FOLIAGE_COLOR, x, y, z, foliageColor));
        assertEquals(BiomeColorNoise.apply(waterColor, quality.biomeColorNoiseWaterIntensity, BiomeColorNoise.sample(x, z)),
                BiomeColorNoise.applyForResolver(BiomeColorHelper.WATER_COLOR, x, y, z, waterColor));
    }

    @Test
    void applyForResolverBypassesNoiseWhenDisabled() {
        var quality = ActiniumRuntime.options().quality;
        boolean original = quality.useBiomeColorNoise;
        quality.useBiomeColorNoise = false;
        try {
            assertEquals(0x00ABCDEF, BiomeColorNoise.applyForResolver(BiomeColorHelper.GRASS_COLOR, 123, 64, -456, 0x00ABCDEF));
        } finally {
            quality.useBiomeColorNoise = original;
        }
    }

    @Test
    void applyForResolverLeavesCustomResolversUntouched() {
        // Mod-added resolvers have unknown color semantics; the noise must not be guessed for them.
        BiomeColorHelper.ColorResolver custom = (biome, pos) -> 0x00FF00AA;

        assertEquals(0x00FF00AA, BiomeColorNoise.applyForResolver(custom, 42, 64, -42, 0x00FF00AA));
    }
}
