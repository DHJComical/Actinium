package net.coderbot.iris.shaderpack.include;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderPackSourceNamesTest {
    @TempDir
    Path packRoot;

    @Test
    void includesComputeOnlySetupAndBeginProgramArrays() {
        List<String> starts = ShaderPackSourceNames.POTENTIAL_STARTS;

        assertTrue(starts.contains("setup.csh"));
        assertTrue(starts.contains("setup1.csh"));
        assertTrue(starts.contains("setup99_z.csh"));
        assertTrue(starts.contains("begin.csh"));
        assertTrue(starts.contains("begin1.csh"));
        assertTrue(starts.contains("begin99_z.csh"));
    }

    @Test
    void discoversComputeOnlySetupAndBeginSourcesWithTheirIncludes() throws IOException {
        Path world0 = Files.createDirectories(packRoot.resolve("world0"));
        Path includeDirectory = Files.createDirectories(packRoot.resolve("lib"));
        Files.writeString(includeDirectory.resolve("shared.glsl"), "const int sharedValue = 7;\n");
        Files.writeString(world0.resolve("setup.csh"), "#version 430\n#include \"/lib/shared.glsl\"\nvoid setupPass() {}\n");
        Files.writeString(world0.resolve("begin.csh"), "#version 430\n#include \"/lib/shared.glsl\"\nvoid beginPass() {}\n");

        ImmutableList.Builder<AbsolutePackPath> starts = ImmutableList.builder();
        boolean found = ShaderPackSourceNames.findPresentSources(
            starts,
            packRoot,
            AbsolutePackPath.fromAbsolutePath("/world0"),
            ShaderPackSourceNames.POTENTIAL_STARTS
        );

        assertTrue(found);
        ImmutableList<AbsolutePackPath> discoveredSources = starts.build();
        assertEquals(
            List.of(
                AbsolutePackPath.fromAbsolutePath("/world0/setup.csh"),
                AbsolutePackPath.fromAbsolutePath("/world0/begin.csh")
            ),
            discoveredSources
        );

        IncludeProcessor processor = new IncludeProcessor(new IncludeGraph(packRoot, discoveredSources));
        assertEquals(
            List.of("#version 430", "const int sharedValue = 7;", "void setupPass() {}"),
            processor.getIncludedFile(AbsolutePackPath.fromAbsolutePath("/world0/setup.csh"))
        );
        assertEquals(
            List.of("#version 430", "const int sharedValue = 7;", "void beginPass() {}"),
            processor.getIncludedFile(AbsolutePackPath.fromAbsolutePath("/world0/begin.csh"))
        );
    }
}
