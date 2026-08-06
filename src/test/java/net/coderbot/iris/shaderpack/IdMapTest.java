package net.coderbot.iris.shaderpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdMapTest {
    @Test
    void modernMcVersionConditionalWithoutFallbackDoesNotEnableLegacySection() {
        String properties = """
                #if defined SHADER_GRASS_SETTING && MC_VERSION == 12001 && defined SHADER_GRASS_UNSUPPORTED_FIX
                block.12=minecraft:short_grass minecraft:grass
                #endif
                #ifdef BOES_EARTH_BLOCKSTATES
                #else
                #endif
                """;

        assertFalse(IdMap.hasLegacySection(properties));
    }

    @Test
    void explicitLegacyVersionEnablesLegacySection() {
        String properties = """
                #if MC_VERSION == 11202
                block.12=minecraft:tallgrass
                #endif
                """;

        assertTrue(IdMap.hasLegacySection(properties));
    }

    @Test
    void mcVersionFallbackEnablesLegacySection() {
        String properties = """
                #if MC_VERSION >= 11400
                block.12=minecraft:short_grass
                #else
                block.12=minecraft:tallgrass
                #endif
                """;

        assertTrue(IdMap.hasLegacySection(properties));
    }
}
