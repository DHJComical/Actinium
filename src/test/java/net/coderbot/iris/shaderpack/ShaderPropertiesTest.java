package net.coderbot.iris.shaderpack;

import net.coderbot.iris.shaderpack.include.AbsolutePackPath;
import net.coderbot.iris.shaderpack.include.IncludeGraph;
import net.coderbot.iris.shaderpack.option.ShaderPackOptions;
import net.minecraft.launchwrapper.Launch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderPropertiesTest {
    @TempDir
    Path tempDir;

    @BeforeAll
    static void initializeLaunchEnvironment() {
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("fml.deobfuscatedEnvironment", false);
    }

    @Test
    void parsesExplicitBufferFlipsAndProfiles() throws IOException {
        ShaderProperties properties = parse("""
            flip.composite.colortex1=false
            flip.composite.colortex8=true
            profile.low=SHADOWS_OFF !BLOOM
            profile2.compat=LEGACY_LIGHTING
            """);

        assertFalse(properties.getExplicitFlips().get("composite").getBoolean("colortex1"));
        assertTrue(properties.getExplicitFlips().get("composite").getBoolean("colortex8"));
        assertEquals(List.of("SHADOWS_OFF", "!BLOOM"), properties.getProfiles().get("low"));
        assertEquals(List.of("LEGACY_LIGHTING"), properties.getProfiles2().get("compat"));
    }

    @Test
    void preprocessorSelectsTheActivePropertyBranch() throws IOException {
        ShaderProperties properties = parse("""
            #ifdef ACTINIUM_TEST
            flip.composite.colortex2=true
            #else
            flip.composite.colortex2=false
            #endif
            """, List.of(new StringPair("ACTINIUM_TEST", "1")));

        assertTrue(properties.getExplicitFlips().get("composite").getBoolean("colortex2"));
    }

    private ShaderProperties parse(String source) throws IOException {
        return parse(source, List.of());
    }

    private ShaderProperties parse(String source, Iterable<StringPair> environmentDefines) throws IOException {
        Path entry = tempDir.resolve("entry.glsl");
        Files.writeString(entry, "void main() {}\n");
        IncludeGraph graph = new IncludeGraph(
            tempDir,
            com.google.common.collect.ImmutableList.of(AbsolutePackPath.fromAbsolutePath("/entry.glsl"))
        );
        ShaderPackOptions options = new ShaderPackOptions(graph, Map.of());
        return new ShaderProperties(source, options, environmentDefines);
    }
}
