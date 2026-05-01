package com.dhj.actinium.shader.pack;

import com.dhj.actinium.celeritas.shader_overrides.ActiniumTerrainPass;
import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.options.ActiniumShaderOptionParser;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

public final class ActiniumShaderPackResources implements AutoCloseable {
    private final String packName;
    private final Path packPath;
    private final @Nullable FileSystem fileSystem;
    private final @Nullable Path shadersRoot;
    private final Properties configProperties;
    private final ActiniumShaderProperties shaderProperties;
    private final ActiniumIdMap idMap;
    private final Map<String, String> optionOverrides;

    private ActiniumShaderPackResources(String packName, Path packPath, @Nullable FileSystem fileSystem, @Nullable Path shadersRoot,
                                        Properties configProperties, ActiniumShaderProperties shaderProperties,
                                        ActiniumIdMap idMap, Map<String, String> optionOverrides) {
        this.packName = packName;
        this.packPath = packPath;
        this.fileSystem = fileSystem;
        this.shadersRoot = shadersRoot;
        this.configProperties = configProperties;
        this.shaderProperties = shaderProperties;
        this.idMap = idMap;
        this.optionOverrides = new LinkedHashMap<>(optionOverrides);
    }

    public static ActiniumShaderPackResources builtin() {
        return new ActiniumShaderPackResources(
                ActiniumShaderPackManager.BUILTIN_PACK_NAME,
                Path.of("."),
                null,
                null,
                new Properties(),
                ActiniumShaderProperties.EMPTY,
                ActiniumIdMap.EMPTY,
                Map.of()
        );
    }

    public static ActiniumShaderPackResources load(ActiniumShaderPack pack) throws IOException {
        return load(pack, Map.of());
    }

    public static ActiniumShaderPackResources load(ActiniumShaderPack pack, Map<String, String> optionOverrides) throws IOException {
        if (pack.builtin()) {
            return builtin();
        }

        if (pack.path() == null) {
            throw new IOException("Shader pack path is missing for " + pack.name());
        }

        Path packPath = pack.path();
        FileSystem fileSystem = null;
        Path shadersRoot;

        if (packPath.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            fileSystem = FileSystems.newFileSystem(packPath, ActiniumShaderPackResources.class.getClassLoader());
            Path zipRoot = fileSystem.getRootDirectories().iterator().next();
            shadersRoot = findShadersRoot(zipRoot).orElse(null);
        } else {
            shadersRoot = findShadersRoot(packPath).orElse(null);
        }

        if (shadersRoot == null) {
            if (fileSystem != null) {
                fileSystem.close();
            }

            throw new IOException("Could not locate shaders directory in " + pack.name());
        }

        Properties configProperties = readPropertiesFile(shadersRoot.resolve("config.txt"), "config.txt");
        ActiniumShaderPackResources parserResources = new ActiniumShaderPackResources(
                pack.name(),
                packPath,
                fileSystem,
                shadersRoot,
                configProperties,
                ActiniumShaderProperties.EMPTY,
                ActiniumIdMap.EMPTY,
                optionOverrides
        );
        List<String> directiveSources = parserResources.collectDirectiveSources();
        Map<String, String> rawShaderProperties = readRawPropertiesEntries(shadersRoot.resolve("shaders.properties"));
        ActiniumShaderProperties shaderProperties = ActiniumShaderProperties.parse(
                readPropertiesFile(shadersRoot.resolve("shaders.properties"), "shaders.properties", directiveSources),
                rawShaderProperties,
                directiveSources
        );
        shaderProperties.applyRuntimeOverrides(optionOverrides);
        Properties blockProperties = readPropertiesFile(shadersRoot.resolve("block.properties"), "block.properties", directiveSources);
        Properties entityProperties = readDimensionAwareProperties(shadersRoot, "entity.properties", directiveSources);
        ActiniumIdMap idMap = ActiniumIdMap.parse(blockProperties, entityProperties);

        return new ActiniumShaderPackResources(pack.name(), packPath, fileSystem, shadersRoot, configProperties, shaderProperties, idMap, optionOverrides);
    }

    public boolean isBuiltin() {
        return this.shadersRoot == null;
    }

    public String packName() {
        return this.packName;
    }

    public Path packPath() {
        return this.packPath;
    }

    public Properties configProperties() {
        return this.configProperties;
    }

    public ActiniumShaderProperties shaderProperties() {
        return this.shaderProperties;
    }

    public ActiniumIdMap idMap() {
        return this.idMap;
    }

    public @Nullable String readShaderSource(String name) {
        if (this.shadersRoot == null) {
            return null;
        }

        String[] split = name.contains(":") ? name.split(":", 2) : new String[]{"minecraft", name};
        String namespace = split[0];
        String path = split[1];

        if (!"actinium".equals(namespace)) {
            return null;
        }

        Path directPath = this.shadersRoot.resolve(path);
        if (Files.isRegularFile(directPath)) {
            return readUtf8(directPath);
        }

        Path namespacedPath = this.shadersRoot.resolve(namespace).resolve(path);
        if (Files.isRegularFile(namespacedPath)) {
            return readUtf8(namespacedPath);
        }

        return null;
    }

    public @Nullable String readProgramSource(ActiniumTerrainPass pass, ShaderType type) {
        if (this.shadersRoot == null) {
            return null;
        }

        for (String candidate : getProgramCandidates(pass)) {
            String source = this.readProgramSource(candidate, type);
            if (source != null) {
                return source;
            }
        }

        return null;
    }

    public @Nullable String readProgramSource(String programName, ShaderType type) {
        if (this.shadersRoot == null) {
            return null;
        }

        Path path = this.findProgramPath(programName + "." + type.fileExtension);
        return path != null ? readShaderText(path) : null;
    }

    public @Nullable byte[] readResourceBytes(String relativePath) {
        if (this.shadersRoot == null) {
            return null;
        }

        Path resourcePath = this.shadersRoot.resolve(relativePath).normalize();

        if (!resourcePath.startsWith(this.shadersRoot) || !Files.isRegularFile(resourcePath)) {
            return null;
        }

        try {
            return Files.readAllBytes(resourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader pack resource " + resourcePath, e);
        }
    }

    public List<String> findShaderOptionFiles() {
        if (this.shadersRoot == null) {
            return List.of();
        }

        Set<String> supportedFileNames = Set.of("config.glsl", "settings.glsl", "options.glsl");
        List<String> results = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(this.shadersRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName() != null)
                    .filter(path -> supportedFileNames.contains(path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .map(path -> this.shadersRoot.relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .forEach(results::add);
        } catch (IOException e) {
            throw new RuntimeException("Failed to enumerate shader option files for " + this.packName, e);
        }

        return results;
    }

    public @Nullable InputStream openResource(String relativePath) throws IOException {
        if (this.shadersRoot == null) {
            return null;
        }

        Path resourcePath = this.shadersRoot.resolve(relativePath).normalize();

        if (!resourcePath.startsWith(this.shadersRoot) || !Files.isRegularFile(resourcePath)) {
            return null;
        }

        return Files.newInputStream(resourcePath);
    }

    @Override
    public void close() {
        if (this.fileSystem != null) {
            try {
                this.fileSystem.close();
            } catch (IOException e) {
                ActiniumShaders.logger().warn("Failed to close shader pack filesystem for {}", this.packName, e);
            }
        }
    }

    private static Optional<Path> findShadersRoot(Path root) throws IOException {
        Path direct = root.resolve("shaders");
        if (Files.isDirectory(direct)) {
            return Optional.of(direct);
        }

        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName() != null && "shaders".equals(path.getFileName().toString()))
                    .findFirst();
        }
    }

    private static Properties readPropertiesFile(Path path, String logicalName) {
        return readPropertiesFile(path, logicalName, List.of());
    }

    private static Properties readPropertiesFile(Path path, String logicalName, Iterable<String> directiveSources) {
        return ActiniumDirectiveProcessor.loadPropertiesFile(path, logicalName, directiveSources);
    }

    private static Properties readDimensionAwareProperties(Path shadersRoot, String logicalName, Iterable<String> directiveSources) {
        Properties merged = new Properties();
        merged.putAll(readPropertiesFile(shadersRoot.resolve(logicalName), logicalName, directiveSources));

        for (String prefix : getDimensionPrefixes()) {
            Path overridePath = shadersRoot.resolve(prefix).resolve(logicalName);
            merged.putAll(readPropertiesFile(overridePath, prefix + "/" + logicalName, directiveSources));
        }

        return merged;
    }

    private static Map<String, String> readRawPropertiesEntries(Path path) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();

        if (!Files.isRegularFile(path)) {
            return entries;
        }

        try {
            StringBuilder current = new StringBuilder();

            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = rawLine;

                if (continuesLine(line)) {
                    current.append(line, 0, line.length() - 1);
                    continue;
                }

                current.append(line);
                String logicalLine = current.toString();
                current.setLength(0);

                parseRawPropertyLine(logicalLine, entries);
            }

            if (!current.isEmpty()) {
                parseRawPropertyLine(current.toString(), entries);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read raw shader properties from " + path, e);
        }

        return entries;
    }

    private @Nullable Path findProgramPath(String relativePath) {
        for (String prefix : getDimensionPrefixes()) {
            Path candidate = null;
            if (this.shadersRoot != null) {
                candidate = this.shadersRoot.resolve(prefix).resolve(relativePath);
            }

            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        Path fallback = null;
        if (this.shadersRoot != null) {
            fallback = this.shadersRoot.resolve(relativePath);
        }
        return Files.isRegularFile(fallback) ? fallback : null;
    }

    private String readShaderText(Path path) {
        return flattenIncludes(path, new LinkedHashMap<>());
    }

    private String flattenIncludes(Path path, Map<Path, Boolean> includeStack) {
        Path normalizedPath = path.normalize();

        if (this.shadersRoot != null && !normalizedPath.startsWith(this.shadersRoot)) {
            throw new RuntimeException("Shader include escapes pack root: " + path);
        }

        if (includeStack.containsKey(normalizedPath)) {
            throw new RuntimeException("Circular shader include detected: " + normalizedPath);
        }

        includeStack.put(normalizedPath, Boolean.TRUE);

        try {
            List<String> output = new ArrayList<>();
            String relativePath = this.shadersRoot != null ? this.shadersRoot.relativize(normalizedPath).toString().replace('\\', '/') : normalizedPath.toString();

            for (String line : Files.readAllLines(normalizedPath, StandardCharsets.UTF_8)) {
                String effectiveLine = ActiniumShaderOptionParser.applyOverride(relativePath, line, this.optionOverrides);
                String trimmed = effectiveLine.trim();
                String includeTarget = parseIncludeTarget(trimmed);

                if (includeTarget == null) {
                    output.add(effectiveLine);
                    continue;
                }

                Path includePath = resolveIncludePath(normalizedPath, includeTarget);
                if (includePath == null) {
                    throw new RuntimeException("Missing shader include '" + includeTarget + "' from " + normalizedPath);
                }
                output.add(flattenIncludes(includePath, includeStack));
            }

            return String.join("\n", output);
        } catch (NoSuchFileException e) {
            throw new RuntimeException("Missing shader include " + normalizedPath, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source " + normalizedPath, e);
        } finally {
            includeStack.remove(normalizedPath);
        }
    }

    private @Nullable Path resolveIncludePath(Path currentFile, String includeTarget) {
        Path path;

        if (includeTarget.startsWith("/")) {
            path = this.shadersRoot.resolve(includeTarget.substring(1));
        } else {
            path = currentFile.getParent().resolve(includeTarget);
        }

        Path normalized = path.normalize();
        return Files.isRegularFile(normalized) ? normalized : null;
    }

    private static @Nullable String parseIncludeTarget(String line) {
        if (!line.startsWith("#include")) {
            return null;
        }

        int quoteStart = line.indexOf('"');
        int quoteEnd = line.lastIndexOf('"');

        if (quoteStart >= 0 && quoteEnd > quoteStart) {
            return line.substring(quoteStart + 1, quoteEnd);
        }

        int angleStart = line.indexOf('<');
        int angleEnd = line.lastIndexOf('>');

        if (angleStart >= 0 && angleEnd > angleStart) {
            return line.substring(angleStart + 1, angleEnd);
        }

        return null;
    }

    private static boolean continuesLine(String line) {
        int backslashCount = 0;

        for (int index = line.length() - 1; index >= 0 && line.charAt(index) == '\\'; index--) {
            backslashCount++;
        }

        return (backslashCount % 2) == 1;
    }

    private static void parseRawPropertyLine(String line, Map<String, String> entries) {
        String trimmed = line.trim();

        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return;
        }

        int separator = findPropertySeparator(line);

        if (separator < 0) {
            String key = unescapeProperty(line.trim());

            if (!key.isEmpty()) {
                entries.put(key, "");
            }
            return;
        }

        String key = unescapeProperty(line.substring(0, separator).trim());
        String value = unescapeProperty(line.substring(separator + 1).trim());

        if (!key.isEmpty()) {
            entries.put(key, value);
        }
    }

    private static int findPropertySeparator(String line) {
        boolean escaped = false;

        for (int index = 0; index < line.length(); index++) {
            char c = line.charAt(index);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '=' || c == ':') {
                return index;
            }
        }

        return -1;
    }

    private static String unescapeProperty(String value) {
        return value.replace("\\:", ":").replace("\\=", "=").replace("\\\\", "\\");
    }

    private static LinkedHashSet<String> getProgramCandidates(ActiniumTerrainPass pass) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        switch (pass) {
            case SHADOW, SHADOW_CUTOUT -> candidates.add("shadow");
            case GBUFFER_SOLID -> {
                candidates.add("gbuffers_terrain");
                candidates.add("gbuffers_terrain_solid");
                candidates.add("gbuffers_textured_lit");
                candidates.add("gbuffers_textured");
            }
            case GBUFFER_CUTOUT -> {
                candidates.add("gbuffers_terrain_cutout");
                candidates.add("gbuffers_terrain");
                candidates.add("gbuffers_terrain_solid");
            }
            case GBUFFER_TRANSLUCENT -> {
                candidates.add("gbuffers_water");
                candidates.add("gbuffers_textured_lit");
                candidates.add("gbuffers_terrain");
            }
        }

        return candidates;
    }

    private static List<String> getDimensionPrefixes() {
        int dimensionId = getCurrentDimensionId();
        List<String> prefixes = new ArrayList<>(1);

        if (dimensionId == 0) {
            prefixes.add("world0");
        } else if (dimensionId == 1) {
            prefixes.add("world1");
        } else if (dimensionId == -1) {
            prefixes.add("world-1");
        }

        return prefixes;
    }

    private List<String> collectDirectiveSources() {
        LinkedHashSet<String> sources = new LinkedHashSet<>();

        collectProgramSource(sources, "shadow", ShaderType.VERTEX);
        collectProgramSource(sources, "shadow", ShaderType.FRAGMENT);
        collectProgramSource(sources, "gbuffers_terrain", ShaderType.VERTEX);
        collectProgramSource(sources, "gbuffers_terrain", ShaderType.FRAGMENT);
        collectProgramSource(sources, "gbuffers_terrain_cutout", ShaderType.VERTEX);
        collectProgramSource(sources, "gbuffers_terrain_cutout", ShaderType.FRAGMENT);
        collectProgramSource(sources, "gbuffers_water", ShaderType.VERTEX);
        collectProgramSource(sources, "gbuffers_water", ShaderType.FRAGMENT);

        return new ArrayList<>(sources);
    }

    private void collectProgramSource(LinkedHashSet<String> sources, String programName, ShaderType type) {
        String source = this.readProgramSource(programName, type);

        if (source != null && !source.isBlank()) {
            sources.add(source);
        }
    }

    private static int getCurrentDimensionId() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();

            if (minecraft != null && minecraft.world != null && minecraft.world.provider != null) {
                return minecraft.world.provider.getDimension();
            }
        } catch (Throwable ignored) {
        }

        return Integer.MIN_VALUE;
    }

    private static String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source " + path, e);
        }
    }
}
