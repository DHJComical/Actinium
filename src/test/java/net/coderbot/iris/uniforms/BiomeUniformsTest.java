package net.coderbot.iris.uniforms;

import net.coderbot.iris.parsing.BiomeCategories;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BiomeUniformsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    void mapsWarmRainyBiomeToRain() {
        Biome biome = biome(new Biome.BiomeProperties("Rainy")
            .setTemperature(0.8F)
            .setRainfall(0.9F));

        assertEquals(1, BiomeUniforms.getBiomePrecipitation(biome, new BlockPos(0, 64, 0)));
    }

    @Test
    void mapsColdSnowyBiomeToSnow() {
        Biome biome = biome(new Biome.BiomeProperties("Snowy")
            .setTemperature(-0.2F)
            .setRainfall(0.5F)
            .setSnowEnabled());

        assertEquals(2, BiomeUniforms.getBiomePrecipitation(biome, new BlockPos(0, 64, 0)));
    }

    @Test
    void mapsDryBiomeToNoPrecipitation() {
        Biome biome = biome(new Biome.BiomeProperties("Desert")
            .setTemperature(2.0F)
            .setRainfall(0.0F)
            .setRainDisabled());

        assertEquals(0, BiomeUniforms.getBiomePrecipitation(biome, new BlockPos(0, 64, 0)));
    }

    @Test
    void detectsSwampCategoryFromName() {
        Biome biome = biome(new Biome.BiomeProperties("Swampland")
            .setTemperature(0.8F)
            .setRainfall(0.9F));

        assertEquals(BiomeCategories.SWAMP.ordinal(), BiomeUniforms.getBiomeCategory(biome));
    }

    @Test
    void exposesDefaultTemperature() {
        Biome biome = biome(new Biome.BiomeProperties("Temperate")
            .setTemperature(0.7F)
            .setRainfall(0.5F));

        assertEquals(0.7F, BiomeUniforms.getBiomeTemperature(biome));
    }

    private static Biome biome(Biome.BiomeProperties properties) {
        return new TestBiome(properties);
    }

    private static final class TestBiome extends Biome {
        TestBiome(Biome.BiomeProperties properties) {
            super(properties);
        }
    }
}
