package com.dhj.actinium.render.shader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the preprocessor conditionals of Actinium's built-in shader sources.
 *
 * <p>A stray {@code #endif} or an orphaned {@code #else} does not fail any Java-side test: the GLSL compiler rejects
 * the whole program at load time and the pass simply draws nothing. That happened once, when a merge left the vertex
 * include with one extra {@code #endif}, and it cost a debugging session with a completely invisible terrain to find.
 * The check here is structural: every conditional has to be opened before it is closed, {@code #else}/{@code #elif}
 * need an enclosing conditional, and nothing may stay open at the end of the file.
 *
 * <p>The files are read from the packaged resources on the test classpath, so what is checked is what ships.
 */
class ShaderConditionalBalanceTest {
    private static final String SHADER_RESOURCE_ROOT = "assets/actinium/shaders";
    private static final List<String> SUFFIXES = List.of(".glsl", ".vsh", ".fsh", ".gsh", ".tcs", ".tes", ".comp");

    @Test
    void everyShaderSourceHasBalancedConditionals() throws IOException {
        Path root = shaderRoot();
        List<Path> shaders = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(ShaderConditionalBalanceTest::isShaderSource)
                    .forEach(shaders::add);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not walk " + root, failure);
        }

        assertFalse(shaders.isEmpty(), "No shader sources found under " + root
                + "; this check would otherwise pass vacuously");

        List<String> failures = new ArrayList<>();

        for (Path shader : shaders) {
            String problem = checkBalance(shader);

            if (problem != null) {
                failures.add(shader + ": " + problem);
            }
        }

        assertTrue(failures.isEmpty(), () -> "Unbalanced shader conditionals:\n  " + String.join("\n  ", failures));
    }

    /** {@return the directory holding the built-in shader sources that this build actually packages} */
    private static Path shaderRoot() {
        URL resource = ShaderConditionalBalanceTest.class.getClassLoader().getResource(SHADER_RESOURCE_ROOT);

        if (resource == null) {
            throw new IllegalStateException("Built-in shader sources are missing from the test classpath: "
                    + SHADER_RESOURCE_ROOT);
        }

        Path root = Path.of(URI.create(resource.toString()));

        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Built-in shader sources are not a directory on disk: " + root);
        }

        return root;
    }

    private static boolean isShaderSource(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        return SUFFIXES.stream().anyMatch(name::endsWith);
    }

    /** {@return a description of the first problem found, or null when the file's conditionals are balanced} */
    private static String checkBalance(Path shader) throws IOException {
        Deque<Integer> openAt = new ArrayDeque<>();
        int lineNumber = 0;

        for (String line : Files.readAllLines(shader, StandardCharsets.UTF_8)) {
            lineNumber++;
            String trimmed = line.trim();

            if (!trimmed.startsWith("#")) {
                continue;
            }

            String directive = trimmed.substring(1).stripLeading();

            if (directive.startsWith("ifdef") || directive.startsWith("ifndef") || directive.startsWith("if ")) {
                openAt.push(lineNumber);
            } else if (directive.startsWith("else") || directive.startsWith("elif")) {
                if (openAt.isEmpty()) {
                    return String.format("line %d: '#%s' without an open conditional", lineNumber, firstWord(directive));
                }
            } else if (directive.startsWith("endif")) {
                if (openAt.isEmpty()) {
                    return String.format("line %d: '#endif' without an open conditional", lineNumber);
                }

                openAt.pop();
            }
        }

        if (!openAt.isEmpty()) {
            return String.format("unclosed conditional(s) opened at line(s) %s", openAt);
        }

        return null;
    }

    private static String firstWord(String directive) {
        int end = directive.indexOf(' ');

        return end < 0 ? directive : directive.substring(0, end);
    }
}
