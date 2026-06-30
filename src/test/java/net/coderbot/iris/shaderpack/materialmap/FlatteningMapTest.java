package net.coderbot.iris.shaderpack.materialmap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatteningMapTest {
    @Test
    void cobwebFallsBackToLegacyWeb() {
        List<BlockEntry> entries = FlatteningMap.toLegacy("cobweb");

        assertSingleEntry(entries, "minecraft", "web");
    }

    @Test
    void sugarCaneFallsBackToLegacyReeds() {
        List<BlockEntry> entries = FlatteningMap.toLegacy("sugar_cane");

        assertSingleEntry(entries, "minecraft", "reeds");
    }

    @Test
    void litRedstoneOreResolvesToLitLegacyBlock() {
        List<BlockEntry> entries = FlatteningMap.toLegacy("redstone_ore", Map.of("lit", "true"));

        assertSingleEntry(entries, "minecraft", "lit_redstone_ore");
    }

    @Test
    void railShapeResolvesToExpectedLegacyMeta() {
        List<BlockEntry> entries = FlatteningMap.toLegacy("rail", Map.of("shape", "north_west"));

        assertSingleEntry(entries, "minecraft", "rail");
        assertEquals(java.util.Set.of(9), entries.get(0).getMetas());
    }

    @Test
    void oakLeavesResolvesToLegacyLeavesMeta() {
        List<BlockEntry> entries = FlatteningMap.toLegacy("oak_leaves");

        assertSingleEntry(entries, "minecraft", "leaves");
        assertTrue(entries.get(0).getMetas().contains(0));
    }

    @Test
    void oakLeavesPersistentFalseKeepsLegacyLeavesMeta() {
        List<BlockEntry> entries = FlatteningMap.toLegacy("oak_leaves", Map.of("persistent", "false"));

        assertSingleEntry(entries, "minecraft", "leaves");
        assertFalse(entries.get(0).getMetas().isEmpty());
    }

    private static void assertSingleEntry(List<BlockEntry> entries, String namespace, String name) {
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals(new NamespacedId(namespace, name), entries.get(0).getId());
    }
}
