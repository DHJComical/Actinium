package net.coderbot.iris.shaderpack.materialmap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlatteningMapCoverageTest {
    @Test
    void commonRegressionTargetsProduceLegacyEntries() {
        assertResolves("cobweb", Map.of());
        assertResolves("sugar_cane", Map.of());
        assertResolves("redstone_ore", Map.of("lit", "true"));
        assertResolves("redstone_ore", Map.of("lit", "false"));
        assertResolves("rail", Map.of("shape", "north_south"));
        assertResolves("oak_leaves", Map.of());
        assertResolves("oak_leaves", Map.of("persistent", "false"));
    }

    private static void assertResolves(String modernName, Map<String, String> stateProperties) {
        List<BlockEntry> entries = FlatteningMap.toLegacy(modernName, stateProperties);
        assertNotNull(entries, modernName + " returned null");
    }
}
