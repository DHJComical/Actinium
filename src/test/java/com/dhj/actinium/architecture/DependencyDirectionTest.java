package com.dhj.actinium.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Enforces the module dependency direction at the bytecode level.
 *
 * <p>Subprojects (shader/glsm/GTNHLib/celeritas-common) must never reference the root
 * project's {@code com.dhj.actinium} implementation classes. Gradle already enforces this
 * at compile time via each subproject's classpath; this test backstops that constraint on
 * the compiled output, so a build-configuration regression cannot silently re-introduce
 * a reverse dependency.
 */
class DependencyDirectionTest {

    private static final String ROOT_PACKAGE_BINARY = "com/dhj/actinium";

    private static final List<String> SUBPROJECTS = List.of(
        "shader",
        "glsm",
        "GTNHLib",
        "celeritas-common"
    );

    @Test
    void subprojectsDoNotReferenceRootProject() throws IOException {
        for (String subproject : SUBPROJECTS) {
            final String projectRoot = System.getProperty("actinium.projectRoot");
            if (projectRoot == null) {
                fail("Missing system property actinium.projectRoot - run tests via the root Gradle project");
            }
            final Path classesDir = Path.of(projectRoot, subproject, "build", "classes", "java", "main");
            assertTrue(Files.isDirectory(classesDir),
                "Missing compiled classes for " + subproject + " - run the root project test task so subprojects are built first");
            try (Stream<Path> classes = Files.walk(classesDir)) {
                classes.filter(path -> path.getFileName().toString().endsWith(".class"))
                    .forEach(path -> assertClassHasNoRootReference(subproject, path));
            }
        }
    }

    private static void assertClassHasNoRootReference(String subproject, Path classFile) {
        try {
            final byte[] bytes = Files.readAllBytes(classFile);
            final String content = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (content.contains(ROOT_PACKAGE_BINARY)) {
                fail(subproject + " class " + classFile.getFileName()
                    + " references the root project package " + ROOT_PACKAGE_BINARY.replace('/', '.'));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + classFile, e);
        }
    }
}
