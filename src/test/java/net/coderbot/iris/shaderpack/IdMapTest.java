package net.coderbot.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import net.coderbot.iris.shaderpack.include.AbsolutePackPath;
import net.coderbot.iris.shaderpack.include.IncludeGraph;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.shaderpack.option.ShaderPackOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdMapTest {
    private static final List<StringPair> LEGACY_ENVIRONMENT = List.of(
            new StringPair("MC_VERSION", "11202"),
            new StringPair("ACTINIUM_TEST_DEFINE", "1")
    );

    @TempDir
    Path tempDir;

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

    @Test
    void packWithoutLegacySectionIsEvaluatedAsModernVersionAndKeepsOtherDefines() throws IOException {
        writePackFile("block.properties", """
                #if MC_VERSION == 260101 && defined ACTINIUM_TEST_DEFINE
                block.1=minecraft:short_grass
                #endif
                """);
        writePackFile("item.properties", """
                #if MC_VERSION == 260101
                item.2=minecraft:iron_sword
                #endif
                """);

        IdMap idMap = new IdMap(tempDir, createOptions(), LEGACY_ENVIRONMENT);

        assertFalse(idMap.hasLegacySection());
        assertEquals(Map.of(1, List.of(BlockEntry.parse("minecraft:short_grass"))), idMap.getBlockProperties());
        assertEquals(2, idMap.getItemIdMap().getInt(new NamespacedId("minecraft", "iron_sword")));
    }

    @Test
    void packWithLegacySectionKeepsTheRealMcVersion() throws IOException {
        writePackFile("block.properties", """
                #if MC_VERSION >= 11300
                block.1=minecraft:short_grass
                #else
                block.1=minecraft:tallgrass
                #endif
                """);
        writePackFile("item.properties", """
                #if MC_VERSION >= 11300
                item.2=minecraft:iron_sword
                #else
                item.2=minecraft:legacy_sword
                #endif
                """);

        IdMap idMap = new IdMap(tempDir, createOptions(), LEGACY_ENVIRONMENT);

        assertTrue(idMap.hasLegacySection());
        assertEquals(Map.of(1, List.of(BlockEntry.parse("minecraft:tallgrass"))), idMap.getBlockProperties());
        assertEquals(2, idMap.getItemIdMap().getInt(new NamespacedId("minecraft", "legacy_sword")));
        assertEquals(-1, idMap.getItemIdMap().getInt(new NamespacedId("minecraft", "iron_sword")));
    }

    private void writePackFile(String name, String contents) throws IOException {
        Files.writeString(tempDir.resolve(name), contents);
    }

    private ShaderPackOptions createOptions() throws IOException {
        Files.writeString(tempDir.resolve("entry.glsl"), "void main() {}\n");
        IncludeGraph graph = new IncludeGraph(
                tempDir,
                ImmutableList.of(AbsolutePackPath.fromAbsolutePath("/entry.glsl"))
        );
        return new ShaderPackOptions(graph, Map.of());
    }
}
