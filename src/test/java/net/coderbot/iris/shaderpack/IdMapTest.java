package net.coderbot.iris.shaderpack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

final class IdMapTest {
    @Test
    void modernGrassAliasesResolveToTallgrassPlant() {
        assertEquals(Collections.singletonList("minecraft:tallgrass:1"), IdMap.normalizeBlockEntry("short_grass", true));
        assertEquals(Collections.singletonList("minecraft:tallgrass:1"), IdMap.normalizeBlockEntry("minecraft:short_grass", true));
        assertEquals(Collections.singletonList("minecraft:tallgrass:1"), IdMap.normalizeBlockEntry("grass", true));
        assertEquals(Collections.singletonList("minecraft:tallgrass:1"), IdMap.normalizeBlockEntry("minecraft:grass", true));
    }

    @Test
    void legacyGrassAliasStillResolvesToGrassBlock() {
        assertEquals(Collections.singletonList("grass"), IdMap.normalizeBlockEntry("grass", false));
        assertEquals(Collections.singletonList("minecraft:grass"), IdMap.normalizeBlockEntry("minecraft:grass", false));
    }

    @Test
    void modernGrassBlockAliasResolvesToLegacyGrassBlock() {
        assertEquals(Collections.singletonList("minecraft:grass"), IdMap.normalizeBlockEntry("grass_block", true));
        assertEquals(Collections.singletonList("minecraft:grass"), IdMap.normalizeBlockEntry("minecraft:grass_block:snowy=false", true));
        assertTrue(IdMap.normalizeBlockEntry("minecraft:grass_block:snowy=true", true).isEmpty());
    }
}
