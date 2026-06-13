package net.coderbot.iris.uniforms;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VanillaBiomeList {
    public static class BiomeEntry {
        public final Biome biome;
        public final String name;

        public BiomeEntry(Biome biome, String name) {
            this.biome = biome;
            this.name = name;
        }
    }

    private static final List<BiomeEntry> VANILLA_BIOMES = createVanillaBiomeList();

    private static List<BiomeEntry> createVanillaBiomeList() {
        final List<BiomeEntry> biomes = new ArrayList<>();

        biomes.add(new BiomeEntry(Biomes.OCEAN, "OCEAN"));
        biomes.add(new BiomeEntry(Biomes.PLAINS, "PLAINS"));
        biomes.add(new BiomeEntry(Biomes.DESERT, "DESERT"));
        biomes.add(new BiomeEntry(Biomes.EXTREME_HILLS, "EXTREME_HILLS"));
        biomes.add(new BiomeEntry(Biomes.FOREST, "FOREST"));
        biomes.add(new BiomeEntry(Biomes.TAIGA, "TAIGA"));
        biomes.add(new BiomeEntry(Biomes.SWAMPLAND, "SWAMPLAND"));
        biomes.add(new BiomeEntry(Biomes.RIVER, "RIVER"));
        biomes.add(new BiomeEntry(Biomes.HELL, "HELL"));
        biomes.add(new BiomeEntry(Biomes.SKY, "SKY"));
        biomes.add(new BiomeEntry(Biomes.FROZEN_OCEAN, "FROZEN_OCEAN"));
        biomes.add(new BiomeEntry(Biomes.FROZEN_RIVER, "FROZEN_RIVER"));
        biomes.add(new BiomeEntry(Biomes.ICE_PLAINS, "ICE_PLAINS"));
        biomes.add(new BiomeEntry(Biomes.ICE_MOUNTAINS, "ICE_MOUNTAINS"));
        biomes.add(new BiomeEntry(Biomes.MUSHROOM_ISLAND, "MUSHROOM_ISLAND"));
        biomes.add(new BiomeEntry(Biomes.MUSHROOM_ISLAND_SHORE, "MUSHROOM_ISLAND_SHORE"));
        biomes.add(new BiomeEntry(Biomes.BEACH, "BEACH"));
        biomes.add(new BiomeEntry(Biomes.DESERT_HILLS, "DESERT_HILLS"));
        biomes.add(new BiomeEntry(Biomes.FOREST_HILLS, "FOREST_HILLS"));
        biomes.add(new BiomeEntry(Biomes.TAIGA_HILLS, "TAIGA_HILLS"));
        biomes.add(new BiomeEntry(Biomes.EXTREME_HILLS_EDGE, "EXTREME_HILLS_EDGE"));
        biomes.add(new BiomeEntry(Biomes.JUNGLE, "JUNGLE"));
        biomes.add(new BiomeEntry(Biomes.JUNGLE_HILLS, "JUNGLE_HILLS"));
        biomes.add(new BiomeEntry(Biomes.JUNGLE_EDGE, "JUNGLE_EDGE"));
        biomes.add(new BiomeEntry(Biomes.DEEP_OCEAN, "DEEP_OCEAN"));
        biomes.add(new BiomeEntry(Biomes.STONE_BEACH, "STONE_BEACH"));
        biomes.add(new BiomeEntry(Biomes.COLD_BEACH, "COLD_BEACH"));
        biomes.add(new BiomeEntry(Biomes.BIRCH_FOREST, "BIRCH_FOREST"));
        biomes.add(new BiomeEntry(Biomes.BIRCH_FOREST_HILLS, "BIRCH_FOREST_HILLS"));
        biomes.add(new BiomeEntry(Biomes.ROOFED_FOREST, "ROOFED_FOREST"));
        biomes.add(new BiomeEntry(Biomes.COLD_TAIGA, "COLD_TAIGA"));
        biomes.add(new BiomeEntry(Biomes.COLD_TAIGA_HILLS, "COLD_TAIGA_HILLS"));
        biomes.add(new BiomeEntry(Biomes.REDWOOD_TAIGA, "REDWOOD_TAIGA"));
        biomes.add(new BiomeEntry(Biomes.REDWOOD_TAIGA_HILLS, "REDWOOD_TAIGA_HILLS"));
        biomes.add(new BiomeEntry(Biomes.EXTREME_HILLS_WITH_TREES, "EXTREME_HILLS_WITH_TREES"));
        biomes.add(new BiomeEntry(Biomes.SAVANNA, "SAVANNA"));
        biomes.add(new BiomeEntry(Biomes.SAVANNA_PLATEAU, "SAVANNA_PLATEAU"));
        biomes.add(new BiomeEntry(Biomes.MESA, "MESA"));
        biomes.add(new BiomeEntry(Biomes.MESA_ROCK, "MESA_ROCK"));
        biomes.add(new BiomeEntry(Biomes.MESA_CLEAR_ROCK, "MESA_CLEAR_ROCK"));
        biomes.add(new BiomeEntry(Biomes.VOID, "VOID"));

        biomes.add(new BiomeEntry(Biomes.SKY, "THE_END"));
        biomes.add(new BiomeEntry(Biomes.SKY, "SMALL_END_ISLANDS"));
        biomes.add(new BiomeEntry(Biomes.SKY, "END_MIDLANDS"));
        biomes.add(new BiomeEntry(Biomes.SKY, "END_HIGHLANDS"));
        biomes.add(new BiomeEntry(Biomes.SKY, "END_BARRENS"));

        return Collections.unmodifiableList(biomes);
    }

    /**
     * Returns the cached list of all vanilla biomes with their shader-friendly names.
     * The list is immutable and created once during class initialization.
     */
    public static List<BiomeEntry> getVanillaBiomes() {
        return VANILLA_BIOMES;
    }
}
