package net.coderbot.iris.shaderpack.materialmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FlatteningMap {
    private static final Map<String, List<BlockEntry>> MODERN_TO_LEGACY = new HashMap<>();
    private static final Map<String, List<BlockEntry>> STATE_MAPPINGS = new HashMap<>();

    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    static {
        rename("grass_block", "grass");
        rename("dirt_path", "grass_path");
        rename("note_block", "noteblock");
        rename("powered_rail", "golden_rail");
        rename("cobweb", "web");
        rename("dead_bush", "deadbush");
        rename("dandelion", "yellow_flower");
        rename("bricks", "brick_block");
        rename("spawner", "mob_spawner");
        rename("oak_door", "wooden_door");
        rename("cobblestone_stairs", "stone_stairs");
        rename("oak_pressure_plate", "wooden_pressure_plate");
        rename("snow_block", "snow");
        rename("sugar_cane", "reeds");
        rename("oak_fence", "fence");
        rename("carved_pumpkin", "pumpkin");
        rename("nether_portal", "portal");
        rename("jack_o_lantern", "lit_pumpkin");
        rename("oak_trapdoor", "trapdoor");
        rename("melon", "melon_block");
        rename("attached_pumpkin_stem", "pumpkin_stem");
        rename("attached_melon_stem", "melon_stem");
        rename("oak_fence_gate", "fence_gate");
        rename("lily_pad", "waterlily");
        rename("nether_bricks", "nether_brick");
        rename("oak_button", "wooden_button");
        rename("nether_quartz_ore", "quartz_ore");
        rename("terracotta", "hardened_clay");
        rename("snow", "snow_layer");

        meta("granite", "stone", 1);
        meta("polished_granite", "stone", 2);
        meta("diorite", "stone", 3);
        meta("polished_diorite", "stone", 4);
        meta("andesite", "stone", 5);
        meta("polished_andesite", "stone", 6);
        meta("smooth_stone", "stone", 0);

        meta("coarse_dirt", "dirt", 1);
        meta("podzol", "dirt", 2);

        meta("oak_planks", "planks", 0);
        meta("spruce_planks", "planks", 1);
        meta("birch_planks", "planks", 2);
        meta("jungle_planks", "planks", 3);
        meta("acacia_planks", "planks", 4);
        meta("dark_oak_planks", "planks", 5);

        metas("oak_sapling", "sapling", 0, 8);
        metas("spruce_sapling", "sapling", 1, 9);
        metas("birch_sapling", "sapling", 2, 10);
        metas("jungle_sapling", "sapling", 3, 11);
        metas("acacia_sapling", "sapling", 4, 12);
        metas("dark_oak_sapling", "sapling", 5, 13);

        meta("red_sand", "sand", 1);

        logVariants("oak_log", "oak_wood", 0);
        logVariants("spruce_log", "spruce_wood", 1);
        logVariants("birch_log", "birch_wood", 2);
        logVariants("jungle_log", "jungle_wood", 3);
        log2Variants("acacia_log", "acacia_wood", 0);
        log2Variants("dark_oak_log", "dark_oak_wood", 1);
        logVariants("stripped_oak_log", "stripped_oak_wood", 0);
        logVariants("stripped_spruce_log", "stripped_spruce_wood", 1);
        logVariants("stripped_birch_log", "stripped_birch_wood", 2);
        logVariants("stripped_jungle_log", "stripped_jungle_wood", 3);
        log2Variants("stripped_acacia_log", "stripped_acacia_wood", 0);
        log2Variants("stripped_dark_oak_log", "stripped_dark_oak_wood", 1);

        leafVariants("oak_leaves", "leaves", 0);
        leafVariants("spruce_leaves", "leaves", 1);
        leafVariants("birch_leaves", "leaves", 2);
        leafVariants("jungle_leaves", "leaves", 3);
        leafVariants("acacia_leaves", "leaves2", 0);
        leafVariants("dark_oak_leaves", "leaves2", 1);

        meta("wet_sponge", "sponge", 1);

        meta("chiseled_sandstone", "sandstone", 1);
        metas("cut_sandstone", "sandstone", 2);
        metas("smooth_sandstone", "sandstone", 2);
        meta("chiseled_red_sandstone", "red_sandstone", 1);
        metas("cut_red_sandstone", "red_sandstone", 2);
        metas("smooth_red_sandstone", "red_sandstone", 2);

        meta("grass", "tallgrass", 1);
        meta("fern", "tallgrass", 2);
        metas("tall_grass", "double_plant", 2, 10);
        metas("large_fern", "double_plant", 3, 11);
        metas("sunflower", "double_plant", 0, 8);
        metas("lilac", "double_plant", 1, 9);
        metas("rose_bush", "double_plant", 4, 12);
        metas("peony", "double_plant", 5, 13);
        doublePlant("sunflower", 0);
        doublePlant("lilac", 1);
        doublePlant("tall_grass", 2);
        doublePlant("large_fern", 3);
        doublePlant("rose_bush", 4);
        doublePlant("peony", 5);

        meta("poppy", "red_flower", 0);
        meta("blue_orchid", "red_flower", 1);
        meta("allium", "red_flower", 2);
        meta("azure_bluet", "red_flower", 3);
        meta("red_tulip", "red_flower", 4);
        meta("orange_tulip", "red_flower", 5);
        meta("white_tulip", "red_flower", 6);
        meta("pink_tulip", "red_flower", 7);
        meta("oxeye_daisy", "red_flower", 8);

        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];
            meta(color + "_wool", "wool", i);
            meta(color + "_stained_glass", "stained_glass", i);
            meta(color + "_stained_glass_pane", "stained_glass_pane", i);
            meta(color + "_carpet", "carpet", i);
            meta(color + "_stained_hardened_clay", "stained_hardened_clay", i);
            rename(color + "_bed", "bed");
        }

        slabVariants("stone_slab", "stone_slab", 0);
        slabVariants("sandstone_slab", "stone_slab", 1);
        slabVariants("cobblestone_slab", "stone_slab", 3);
        slabVariants("brick_slab", "stone_slab", 4);
        slabVariants("stone_brick_slab", "stone_slab", 5);
        slabVariants("nether_brick_slab", "stone_slab", 6);
        slabVariants("quartz_slab", "stone_slab", 7);
        slabVariants("red_sandstone_slab", "stone_slab2", 0);
        slabVariants("purpur_slab", "stone_slab2", 1);
        slabVariants("oak_slab", "wooden_slab", 0);
        slabVariants("spruce_slab", "wooden_slab", 1);
        slabVariants("birch_slab", "wooden_slab", 2);
        slabVariants("jungle_slab", "wooden_slab", 3);
        slabVariants("acacia_slab", "wooden_slab", 4);
        slabVariants("dark_oak_slab", "wooden_slab", 5);
        slabStates("stone_slab", "stone_slab", 0, "double_stone_slab", 0);
        slabStates("sandstone_slab", "stone_slab", 1, "double_stone_slab", 1);
        slabStates("cobblestone_slab", "stone_slab", 3, "double_stone_slab", 3);
        slabStates("brick_slab", "stone_slab", 4, "double_stone_slab", 4);
        slabStates("stone_brick_slab", "stone_slab", 5, "double_stone_slab", 5);
        slabStates("nether_brick_slab", "stone_slab", 6, "double_stone_slab", 6);
        slabStates("quartz_slab", "stone_slab", 7, "double_stone_slab", 7);
        slabStates("red_sandstone_slab", "stone_slab2", 0, "double_stone_slab2", 0);
        slabStates("purpur_slab", "stone_slab2", 1, "double_stone_slab2", 1);
        slabStates("oak_slab", "wooden_slab", 0, "double_wooden_slab", 0);
        slabStates("spruce_slab", "wooden_slab", 1, "double_wooden_slab", 1);
        slabStates("birch_slab", "wooden_slab", 2, "double_wooden_slab", 2);
        slabStates("jungle_slab", "wooden_slab", 3, "double_wooden_slab", 3);
        slabStates("acacia_slab", "wooden_slab", 4, "double_wooden_slab", 4);
        slabStates("dark_oak_slab", "wooden_slab", 5, "double_wooden_slab", 5);

        multi("water", entry("water"), entry("flowing_water"));
        multi("lava", entry("lava"), entry("flowing_lava"));
        liquidLevels("water", "water", "flowing_water");
        liquidLevels("lava", "lava", "flowing_lava");

        state("furnace", "lit", "false", entry("furnace"));
        state("furnace", "lit", "true", entry("lit_furnace"));
        state("redstone_ore", "lit", "false", entry("redstone_ore"));
        state("redstone_ore", "lit", "true", entry("lit_redstone_ore"));
        state("redstone_lamp", "lit", "false", entry("redstone_lamp"));
        state("redstone_lamp", "lit", "true", entry("lit_redstone_lamp"));
        state("repeater", "powered", "false", entry("unpowered_repeater"));
        state("repeater", "powered", "true", entry("powered_repeater"));
        state("comparator", "powered", "false", entry("unpowered_comparator"));
        state("comparator", "powered", "true", entry("powered_comparator"));
    }

    private FlatteningMap() {
    }

    public static List<BlockEntry> toLegacy(String modernName) {
        return MODERN_TO_LEGACY.get(modernName);
    }

    public static List<BlockEntry> toLegacy(String modernName, Map<String, String> stateProperties) {
        if (stateProperties != null && !stateProperties.isEmpty()) {
            List<List<BlockEntry>> propertyResults = new ArrayList<>();

            for (Map.Entry<String, String> property : stateProperties.entrySet()) {
                List<BlockEntry> result = STATE_MAPPINGS.get(stateKey(modernName, property.getKey(), property.getValue()));
                if (result != null) {
                    propertyResults.add(result);
                }
            }

            if (propertyResults.size() == 1) {
                return propertyResults.get(0);
            }

            if (propertyResults.size() > 1) {
                List<BlockEntry> combined = propertyResults.get(0);
                for (int i = 1; i < propertyResults.size(); i++) {
                    combined = intersectBlockEntries(combined, propertyResults.get(i));
                    if (combined.isEmpty()) {
                        break;
                    }
                }

                if (!combined.isEmpty()) {
                    return combined;
                }
            }
        }

        return MODERN_TO_LEGACY.get(modernName);
    }

    private static List<BlockEntry> intersectBlockEntries(List<BlockEntry> a, List<BlockEntry> b) {
        List<BlockEntry> result = new ArrayList<>(Math.min(a.size(), b.size()));
        for (BlockEntry entryA : a) {
            for (BlockEntry entryB : b) {
                if (!entryA.getId().equals(entryB.getId())) {
                    continue;
                }

                Set<Integer> metasA = entryA.getMetas();
                Set<Integer> metasB = entryB.getMetas();

                if (metasA.isEmpty() && metasB.isEmpty()) {
                    result.add(new BlockEntry(entryA.getId(), Collections.emptySet()));
                } else if (metasA.isEmpty()) {
                    result.add(new BlockEntry(entryA.getId(), metasB));
                } else if (metasB.isEmpty()) {
                    result.add(new BlockEntry(entryA.getId(), metasA));
                } else {
                    Set<Integer> intersection = new java.util.HashSet<>(metasA);
                    intersection.retainAll(metasB);
                    if (!intersection.isEmpty()) {
                        result.add(new BlockEntry(entryA.getId(), Set.copyOf(intersection)));
                    }
                }
            }
        }

        return result;
    }

    private static void rename(String modern, String legacy) {
        MODERN_TO_LEGACY.put(modern, List.of(new BlockEntry(new NamespacedId("minecraft", legacy), Collections.emptySet())));
    }

    private static void meta(String modern, String legacy, int meta) {
        MODERN_TO_LEGACY.put(modern, List.of(new BlockEntry(new NamespacedId("minecraft", legacy), Collections.singleton(meta))));
    }

    private static void metas(String modern, String legacy, int... metaValues) {
        ArrayList<Integer> metaList = new ArrayList<>(metaValues.length);
        for (int metaValue : metaValues) {
            metaList.add(metaValue);
        }
        MODERN_TO_LEGACY.put(modern, List.of(new BlockEntry(new NamespacedId("minecraft", legacy), Set.copyOf(metaList))));
    }

    private static void multi(String modern, BlockEntry... entries) {
        MODERN_TO_LEGACY.put(modern, List.of(entries));
    }

    private static void state(String modern, String property, String value, BlockEntry... entries) {
        STATE_MAPPINGS.put(stateKey(modern, property, value), List.of(entries));
    }

    private static String stateKey(String modern, String property, String value) {
        return modern + "|" + property + "=" + value;
    }

    private static void liquidLevels(String modern, String legacyStatic, String legacyDynamic) {
        for (int level = 0; level <= 15; level++) {
            state(modern, "level", String.valueOf(level), entryMetas(legacyStatic, level), entryMetas(legacyDynamic, level));
        }
    }

    private static BlockEntry entry(String legacyName) {
        return new BlockEntry(new NamespacedId("minecraft", legacyName), Collections.emptySet());
    }

    private static BlockEntry entryMetas(String legacyName, int... metaValues) {
        ArrayList<Integer> metaList = new ArrayList<>(metaValues.length);
        for (int metaValue : metaValues) {
            metaList.add(metaValue);
        }
        return new BlockEntry(new NamespacedId("minecraft", legacyName), Set.copyOf(metaList));
    }

    private static void logVariants(String logName, String woodName, int typeOffset) {
        metas(logName, "log", typeOffset, typeOffset + 4, typeOffset + 8);
        meta(woodName, "log", typeOffset + 12);
    }

    private static void log2Variants(String logName, String woodName, int typeOffset) {
        int phantomTypeOffset = typeOffset + 2;
        metas(logName, "log2",
                typeOffset, typeOffset + 4, typeOffset + 8,
                phantomTypeOffset, phantomTypeOffset + 4, phantomTypeOffset + 8);
        metas(woodName, "log2", typeOffset + 12, phantomTypeOffset + 12);
    }

    private static void leafVariants(String modern, String legacy, int typeOffset) {
        metas(modern, legacy, typeOffset, typeOffset + 4, typeOffset + 8, typeOffset + 12);
    }

    private static void slabVariants(String modern, String legacy, int typeOffset) {
        metas(modern, legacy, typeOffset, typeOffset + 8);
    }

    private static void slabStates(String modern, String slabBlock, int typeOffset, String doubleBlock, int doubleMeta) {
        state(modern, "type", "bottom", entryMetas(slabBlock, typeOffset));
        state(modern, "type", "top", entryMetas(slabBlock, typeOffset + 8));
        state(modern, "type", "double", entryMetas(doubleBlock, doubleMeta));
    }

    private static void doublePlant(String modern, int variant) {
        int upper = 0x8 | variant;
        state(modern, "half", "lower", entryMetas("double_plant", variant));
        state(modern, "half", "upper", entryMetas("double_plant", upper));
    }
}
