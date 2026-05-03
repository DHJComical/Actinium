package org.taumc.actinium.gradle;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class ActiniumUniminedHelper {
    private ActiniumUniminedHelper() {
    }

    public static void configureProductionRemap(Project project) {
        project.getTasks().named("remapJar").configure(task -> {
            if (!hasMethod(task.getClass(), "mixinRemap", Function1.class)) {
                return;
            }

            invokeMethod(task, "mixinRemap", new Class<?>[]{Function1.class}, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object mixin) {
                    invokeMethod(mixin, "enableBaseMixin");
                    invokeMethod(mixin, "enableMixinExtra");
                    invokeMethod(mixin, "disableRefmap");
                    return Unit.INSTANCE;
                }
            });
        });
    }

    public static File prepareDevelopmentAccessTransformer(Project project, File projectAccessTransformer, String... configurationNames) {
        File outputRoot = project.getLayout().getBuildDirectory().dir("generated/actinium").get().getAsFile();
        File extractedOutputDirectory = new File(outputRoot, "dependency-ats-mcp");
        File mergedAccessTransformer = new File(outputRoot, "development-access-transformer.cfg");

        project.delete(extractedOutputDirectory);
        project.delete(mergedAccessTransformer);

        if (!extractedOutputDirectory.exists() && !extractedOutputDirectory.mkdirs()) {
            throw new IllegalStateException("Failed to create dependency AT output directory: " + extractedOutputDirectory);
        }

        File srgToMcp = findSrgToMcpMapping(project);
        SrgMappings mappings = srgToMcp != null ? loadSrgMappings(srgToMcp.toPath()) : SrgMappings.EMPTY;
        if (srgToMcp != null) {
            project.getLogger().lifecycle("Using dependency AT remap file {}", srgToMcp);
        } else {
            project.getLogger().warn("Could not find a srg-mcp.srg mapping file; dependency access transformers will be used without remapping");
        }

        Set<File> dependencyJars = collectDeclaredDependencyJars(project, configurationNames);

        List<File> extractedFiles = new ArrayList<>();
        for (File dependencyJar : dependencyJars) {
            if (!dependencyJar.isFile() || !dependencyJar.getName().endsWith(".jar")) {
                continue;
            }
            extractedFiles.addAll(extractJarAccessTransformers(project, dependencyJar, extractedOutputDirectory, mappings));
        }

        if (!extractedFiles.isEmpty()) {
            project.getLogger().lifecycle("Prepared {} dependency access transformer file(s) for development runtime", extractedFiles.size());
        }

        if (projectAccessTransformer == null && extractedFiles.isEmpty()) {
            return null;
        }

        StringBuilder mergedContent = new StringBuilder();
        if (projectAccessTransformer != null) {
            if (!projectAccessTransformer.exists()) {
                throw new IllegalStateException("Project access transformer file does not exist: " + projectAccessTransformer);
            }
            appendAccessTransformer(mergedContent, projectAccessTransformer, "Project access transformer");
        }
        for (File extractedFile : extractedFiles) {
            String source = extractedFile.getParentFile().getName() + "/" + extractedFile.getName();
            appendAccessTransformer(mergedContent, extractedFile, "Dependency access transformer: " + source);
        }

        try {
            Files.createDirectories(mergedAccessTransformer.toPath().getParent());
            Files.writeString(mergedAccessTransformer.toPath(), mergedContent.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write merged development access transformer", e);
        }

        project.getLogger().lifecycle("Prepared development access transformer {}", mergedAccessTransformer);
        return mergedAccessTransformer;
    }

    private static File findSrgToMcpMapping(Project project) {
        File gradleUserHome = project.getGradle().getGradleUserHomeDir();
        if (gradleUserHome == null || !gradleUserHome.isDirectory()) {
            return null;
        }

        List<Path> candidates = new ArrayList<>();
        try (var stream = Files.walk(gradleUserHome.toPath())) {
            stream.filter(path -> path.getFileName().toString().equals("srg-mcp.srg"))
                    .filter(path -> path.toString().contains("mcp_stable"))
                    .filter(path -> path.toString().contains(File.separator + "39" + File.separator))
                    .forEach(candidates::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan Gradle cache for srg-mcp.srg", e);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((left, right) -> Integer.compare(right.getNameCount(), left.getNameCount()));
        return candidates.get(0).toFile();
    }

    private static SrgMappings loadSrgMappings(Path path) {
        Map<String, String> fieldMappings = new HashMap<>();
        Map<String, String> methodMappings = new HashMap<>();

        try {
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.startsWith("FD: ")) {
                    String[] parts = line.substring(4).trim().split("\\s+");
                    if (parts.length != 2) {
                        continue;
                    }

                    String source = parts[0];
                    String target = parts[1];
                    fieldMappings.put(source, target.substring(target.lastIndexOf('/') + 1));
                } else if (line.startsWith("MD: ")) {
                    String[] parts = line.substring(4).trim().split("\\s+");
                    if (parts.length != 4) {
                        continue;
                    }

                    String source = parts[0];
                    String sourceDescriptor = parts[1];
                    String target = parts[2];
                    methodMappings.put(source + sourceDescriptor, target.substring(target.lastIndexOf('/') + 1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read srg-mcp mappings from " + path, e);
        }

        return new SrgMappings(fieldMappings, methodMappings);
    }

    private static Set<File> collectDeclaredDependencyJars(Project project, String... configurationNames) {
        Set<File> dependencyJars = new LinkedHashSet<>();

        for (String configurationName : configurationNames) {
            Configuration configuration = project.getConfigurations().findByName(configurationName);
            if (configuration == null) {
                project.getLogger().lifecycle("Skipping missing configuration {} while preparing development access transformers", configurationName);
                continue;
            }

            if (configuration.getDependencies().isEmpty()) {
                project.getLogger().lifecycle("No declared dependencies found in {} while preparing development access transformers", configurationName);
                continue;
            }

            project.getLogger().lifecycle("Scanning declared dependencies from {} for embedded access transformers", configurationName);
            for (Dependency dependency : configuration.getDependencies()) {
                if (dependency instanceof ProjectDependency) {
                    project.getLogger().lifecycle("Skipping project dependency {} from {}", dependency, configurationName);
                    continue;
                }

                for (File resolvedFile : resolveDependencyFiles(project, dependency, configurationName)) {
                    if (resolvedFile.isFile() && resolvedFile.getName().endsWith(".jar")) {
                        project.getLogger().lifecycle("Resolved {} from {} to {}", dependency, configurationName, resolvedFile);
                        dependencyJars.add(resolvedFile);
                    }
                }
            }
        }

        return dependencyJars;
    }

    private static Set<File> resolveDependencyFiles(Project project, Dependency dependency, String configurationName) {
        try {
            Configuration detachedConfiguration = project.getConfigurations().detachedConfiguration(dependency.copy());
            return detachedConfiguration.resolve();
        } catch (Exception e) {
            project.getLogger().warn(
                "Failed to resolve dependency {} from {} while preparing development access transformers",
                dependency,
                configurationName,
                e
            );
            return Set.of();
        }
    }

    private static List<File> extractJarAccessTransformers(Project project, File dependencyJar, File outputDirectory, SrgMappings mappings) {
        try (JarFile jarFile = new JarFile(dependencyJar)) {
            Attributes attributes = jarFile.getManifest() != null ? jarFile.getManifest().getMainAttributes() : null;
            if (attributes == null) {
                return List.of();
            }

            String accessTransformers = attributes.getValue("FMLAT");
            if (accessTransformers == null || accessTransformers.isBlank()) {
                return List.of();
            }

            project.getLogger().lifecycle("Found embedded access transformers {} in {}", accessTransformers, dependencyJar);

            File jarOutputDirectory = new File(outputDirectory, sanitizeFileName(dependencyJar.getName()));
            if (!jarOutputDirectory.exists() && !jarOutputDirectory.mkdirs()) {
                throw new IllegalStateException("Failed to create dependency AT directory: " + jarOutputDirectory);
            }

            List<File> extractedFiles = new ArrayList<>();
            for (String entryName : splitManifestValues(accessTransformers)) {
                String normalizedEntryName = "META-INF/" + entryName.trim();
                var entry = jarFile.getJarEntry(normalizedEntryName);
                if (entry == null) {
                    project.getLogger().warn("Dependency {} declares FMLAT {} but the entry was not found", dependencyJar.getName(), normalizedEntryName);
                    continue;
                }

                File extractedFile = new File(jarOutputDirectory, new File(entryName).getName());
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    String original = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    String remapped = remapAccessTransformerContent(original, mappings);
                    Files.writeString(extractedFile.toPath(), remapped, StandardCharsets.UTF_8);
                }
                project.getLogger().lifecycle("Prepared remapped dependency access transformer {}", extractedFile);
                extractedFiles.add(extractedFile);
            }

            return extractedFiles;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract dependency access transformers from " + dependencyJar, e);
        }
    }

    private static void appendAccessTransformer(StringBuilder builder, File accessTransformer, String label) {
        try {
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append(System.lineSeparator());
            }
            builder.append("# ").append(label).append(System.lineSeparator());
            builder.append(Files.readString(accessTransformer.toPath(), StandardCharsets.UTF_8));
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to append access transformer " + accessTransformer, e);
        }
    }

    private static String remapAccessTransformerContent(String content, SrgMappings mappings) {
        StringBuilder builder = new StringBuilder(content.length() + 32);
        for (String rawLine : content.split("\\R", -1)) {
            builder.append(remapAccessTransformerLine(rawLine, mappings)).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String remapAccessTransformerLine(String rawLine, SrgMappings mappings) {
        String line = rawLine;
        String comment = "";
        int commentIndex = rawLine.indexOf('#');
        if (commentIndex >= 0) {
            line = rawLine.substring(0, commentIndex);
            comment = rawLine.substring(commentIndex);
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return rawLine;
        }

        String[] parts = trimmed.split("\\s+");
        if (parts.length < 3) {
            return rawLine;
        }

        String owner = parts[1].replace('.', '/');
        String member = parts[2];

        if (member.contains("(")) {
            int descriptorIndex = member.indexOf('(');
            String methodName = member.substring(0, descriptorIndex);
            String descriptor = member.substring(descriptorIndex);
            String mapped = mappings.methodMappings.get(owner + "/" + methodName + descriptor);
            if (mapped != null) {
                parts[2] = mapped + descriptor;
            }
        } else {
            String mapped = mappings.fieldMappings.get(owner + "/" + member);
            if (mapped != null) {
                parts[2] = mapped;
            }
        }

        String remapped = String.join(" ", parts);
        if (!comment.isEmpty()) {
            remapped = remapped + " " + comment;
        }
        return remapped;
    }

    private static List<String> splitManifestValues(String value) {
        String[] split = value.trim().split("\\s+");
        List<String> values = new ArrayList<>(split.length);
        for (String entry : split) {
            if (!entry.isBlank()) {
                values.add(entry);
            }
        }
        return values;
    }

    private static String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static final class SrgMappings {
        private static final SrgMappings EMPTY = new SrgMappings(Collections.emptyMap(), Collections.emptyMap());

        private final Map<String, String> fieldMappings;
        private final Map<String, String> methodMappings;

        private SrgMappings(Map<String, String> fieldMappings, Map<String, String> methodMappings) {
            this.fieldMappings = fieldMappings;
            this.methodMappings = methodMappings;
        }
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            type.getMethod(name, parameterTypes);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Object invokeMethod(Object target, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, parameterTypes);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke " + name + " on " + target.getClass().getName(), e);
        }
    }

    private static Object invokeMethod(Object target, String name) {
        return invokeMethod(target, name, new Class<?>[0]);
    }
}
