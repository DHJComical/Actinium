package com.dhj.actinium.world.biome;

import net.minecraft.world.biome.BiomeColorHelper;
import org.junit.jupiter.api.Test;

import com.dhj.actinium.runtime.ActiniumRuntime;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct logic verification of the cache-level noise override (issue #56). The tests call the
 * {@link BiomeColorCache} overrides themselves — no source text checks, no reflection — and lock
 * down the two contracts the celeritas-common base class hook relies on: the post-processing pass
 * is skipped whenever the noise would be an identity, and the per-pixel adjustment is exactly the
 * engine dispatch in {@link BiomeColorNoise#applyForResolver}.
 */
class BiomeColorCacheNoiseTest {

    /**
     * {@link WorldSlice} copies data from a live {@code World}, which does not exist in the test
     * JVM, and even a {@code null} slice reference is rejected when the slice-bound method
     * reference is created. The package-private supplier constructor exists for exactly this
     * case: the supplier is never invoked here, so a placeholder lambda keeps the test focused on
     * the hook overrides, which only read the quality options.
     */
    private final BiomeColorCache cache = new BiomeColorCache((x, y, z) -> null, 4);

    private final SodiumGameOptions.QualitySettings quality = ActiniumRuntime.options().quality;

    private boolean shouldPostProcess() {
        return cache.shouldPostProcessColors();
    }

    @Test
    void shouldPostProcessIsTrueWithDefaultConfig() {
        // The test JVM has no game options file, so ActiniumRuntime degrades to read-only
        // defaults: the feature on, all three intensities at 0.08.
        assertTrue(quality.useBiomeColorNoise);
        assertEquals(0.08F, quality.biomeColorNoiseGrassIntensity, 0.0F);
        assertEquals(0.08F, quality.biomeColorNoiseFoliageIntensity, 0.0F);
        assertEquals(0.08F, quality.biomeColorNoiseWaterIntensity, 0.0F);

        assertTrue(shouldPostProcess());
    }

    @Test
    void shouldPostProcessIsFalseWhenFeatureDisabled() {
        boolean original = quality.useBiomeColorNoise;
        quality.useBiomeColorNoise = false;
        try {
            assertFalse(shouldPostProcess());
        } finally {
            quality.useBiomeColorNoise = original;
        }
    }

    @Test
    void shouldPostProcessIsFalseWhenEveryIntensityIsZero() {
        // With the toggle on but all amplitudes at zero the noise is a mathematical identity, so
        // the whole per-pixel pass must be skipped.
        float originalGrass = quality.biomeColorNoiseGrassIntensity;
        float originalFoliage = quality.biomeColorNoiseFoliageIntensity;
        float originalWater = quality.biomeColorNoiseWaterIntensity;
        quality.biomeColorNoiseGrassIntensity = 0.0F;
        quality.biomeColorNoiseFoliageIntensity = 0.0F;
        quality.biomeColorNoiseWaterIntensity = 0.0F;
        try {
            assertFalse(shouldPostProcess());
        } finally {
            quality.biomeColorNoiseGrassIntensity = originalGrass;
            quality.biomeColorNoiseFoliageIntensity = originalFoliage;
            quality.biomeColorNoiseWaterIntensity = originalWater;
        }
    }

    @Test
    void shouldPostProcessIsTrueWhenAnySingleIntensityIsPositive() {
        float originalGrass = quality.biomeColorNoiseGrassIntensity;
        float originalFoliage = quality.biomeColorNoiseFoliageIntensity;
        float originalWater = quality.biomeColorNoiseWaterIntensity;
        quality.biomeColorNoiseGrassIntensity = 0.0F;
        quality.biomeColorNoiseFoliageIntensity = 0.01F;
        quality.biomeColorNoiseWaterIntensity = 0.0F;
        try {
            assertTrue(shouldPostProcess());
        } finally {
            quality.biomeColorNoiseGrassIntensity = originalGrass;
            quality.biomeColorNoiseFoliageIntensity = originalFoliage;
            quality.biomeColorNoiseWaterIntensity = originalWater;
        }
    }

    @Test
    void postProcessColorMatchesTheEngineDispatchForEveryVanillaResolver() {
        int x = 3141;
        int y = 64;
        int z = -2718;

        assertEquals(BiomeColorNoise.applyForResolver(BiomeColorHelper.GRASS_COLOR, x, y, z, 0x0079C05A),
                cache.postProcessColor(BiomeColorHelper.GRASS_COLOR, x, y, z, 0x0079C05A));
        assertEquals(BiomeColorNoise.applyForResolver(BiomeColorHelper.FOLIAGE_COLOR, x, y, z, 0x00565E31),
                cache.postProcessColor(BiomeColorHelper.FOLIAGE_COLOR, x, y, z, 0x00565E31));
        assertEquals(BiomeColorNoise.applyForResolver(BiomeColorHelper.WATER_COLOR, x, y, z, 0x003F76E4),
                cache.postProcessColor(BiomeColorHelper.WATER_COLOR, x, y, z, 0x003F76E4));
    }

    @Test
    void postProcessColorIsIdentityWhenFeatureDisabled() {
        boolean original = quality.useBiomeColorNoise;
        quality.useBiomeColorNoise = false;
        try {
            assertEquals(0x00ABCDEF,
                    cache.postProcessColor(BiomeColorHelper.GRASS_COLOR, 123, 64, -456, 0x00ABCDEF));
        } finally {
            quality.useBiomeColorNoise = original;
        }
    }

    @Test
    void postProcessColorLeavesUnknownResolversUntouched() {
        // The override forwards to the engine dispatch, which must keep treating mod-added
        // resolvers as identity instead of guessing an intensity for them.
        BiomeColorHelper.ColorResolver custom = (biome, pos) -> 0x00FF00AA;

        assertEquals(0x00FF00AA, cache.postProcessColor(custom, 42, 64, -42, 0x00FF00AA));
    }
}
