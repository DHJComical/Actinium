package net.coderbot.iris.shaderpack;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McVersionDefinesTest {
    @Test
    void withMcVersionReplacesEveryMcVersionAndKeepsTheOtherDefinesInOrder() {
        List<StringPair> defines = List.of(
                new StringPair("MC_OS_LINUX", ""),
                new StringPair("MC_VERSION", "11202"),
                new StringPair("MC_GL_VERSION", "460"),
                new StringPair("MC_VERSION", "11202"),
                new StringPair("IS_ACTINIUM", "")
        );

        List<StringPair> rewritten = McVersionDefines.withMcVersion(defines, 11800);

        assertEquals(
                List.of("MC_OS_LINUX=", "MC_GL_VERSION=460", "IS_ACTINIUM=", "MC_VERSION=11800"),
                render(rewritten)
        );
        assertEquals(
                List.of("MC_OS_LINUX=", "MC_VERSION=11202", "MC_GL_VERSION=460", "MC_VERSION=11202", "IS_ACTINIUM="),
                render(defines)
        );
    }

    @Test
    void withMcVersionAppendsMcVersionWhenItIsMissing() {
        List<StringPair> rewritten = McVersionDefines.withMcVersion(List.of(new StringPair("IS_ACTINIUM", "")), 260101);

        assertEquals(List.of("IS_ACTINIUM=", "MC_VERSION=260101"), render(rewritten));
    }

    @Test
    void mcVersionOfReturnsTheLastDefinition() {
        List<StringPair> defines = List.of(
                new StringPair("MC_VERSION", "11202"),
                new StringPair("IS_ACTINIUM", ""),
                new StringPair("MC_VERSION", "11800")
        );

        assertEquals(OptionalInt.of(11800), McVersionDefines.mcVersionOf(defines));
    }

    @Test
    void mcVersionOfIsEmptyWithoutMcVersion() {
        assertEquals(OptionalInt.empty(), McVersionDefines.mcVersionOf(List.of()));
        assertEquals(OptionalInt.empty(), McVersionDefines.mcVersionOf(List.of(new StringPair("IS_ACTINIUM", ""))));
    }

    @Test
    void mcVersionOfRejectsNonNumericValues() {
        assertThrows(IllegalStateException.class,
                () -> McVersionDefines.mcVersionOf(List.of(new StringPair("MC_VERSION", "1.12.2"))));
        assertThrows(IllegalStateException.class,
                () -> McVersionDefines.mcVersionOf(List.of(new StringPair("MC_VERSION", ""))));
    }

    private static List<String> render(Iterable<StringPair> defines) {
        List<String> rendered = new ArrayList<>();
        for (StringPair define : defines) {
            rendered.add(define.getKey() + "=" + define.getValue());
        }
        return rendered;
    }
}
