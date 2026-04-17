package com.dhj.actinium.shader.pack;

import com.dhj.actinium.celeritas.shader_overrides.ActiniumTerrainPass;
import com.dhj.actinium.celeritas.ActiniumShaders;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public final class ActiniumShaderPackResources implements AutoCloseable {
    private final String packName;
    private final Path packPath;
    private final @Nullable FileSystem fileSystem;
    private final @Nullable Path shadersRoot;
    private final Properties configProperties;
    private final ActiniumShaderProperties shaderProperties;
    private final ActiniumIdMap idMap;

    private ActiniumShaderPackResources(String packName, Path packPath, @Nullable FileSystem fileSystem, @Nullable Path shadersRoot, Properties configProperties, ActiniumShaderProperties shaderProperties, ActiniumIdMap idMap) {
        this.packName = packName;
        this.packPath = packPath;
        this.fileSystem = fileSystem;
        this.shadersRoot = shadersRoot;
        this.configProperties = configProperties;
        this.shaderProperties = shaderProperties;
        this.idMap = idMap;
    }

    public static ActiniumShaderPackResources builtin() {
        return new ActiniumShaderPackResources(
                ActiniumShaderPackManager.BUILTIN_PACK_NAME,
                Path.of("."),
                null,
                null,
                new Properties(),
                ActiniumShaderProperties.EMPTY,
                ActiniumIdMap.EMPTY
        );
    }

    public static ActiniumShaderPackResources load(ActiniumShaderPack pack) throws IOException {
        if (pack.builtin()) {
            return builtin();
        }

        if (pack.path() == null) {
            throw new IOException("Shader pack path is missing for " + pack.name());
        }

        Path packPath = pack.path();
        FileSystem fileSystem = null;
        Path shadersRoot;

        if (packPath.toString().toLowerCase().endsWith(".zip")) {
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
        ActiniumShaderProperties shaderProperties = ActiniumShaderProperties.parse(readPropertiesFile(shadersRoot.resolve("shaders.properties"), "shaders.properties"));
        ActiniumIdMap idMap = ActiniumIdMap.parse(readPropertiesFile(shadersRoot.resolve("block.properties"), "block.properties"));

        return new ActiniumShaderPackResources(pack.name(), packPath, fileSystem, shadersRoot, configProperties, shaderProperties, idMap);
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
            Path path = this.findProgramPath(candidate + "." + type.fileExtension);

            if (path != null) {
                return readShaderText(path);
            }
        }

        return null;
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
        Properties properties = new Properties();

        if (!Files.isRegularFile(path)) {
            return properties;
        }

        try (var stream = Files.newInputStream(path)) {
            properties.load(stream);
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to read shader pack file {} ({})", logicalName, path, e);
        }

        return properties;
    }

    private @Nullable Path findProgramPath(String relativePath) {
        for (String prefix : getDimensionPrefixes()) {
            Path candidate = this.shadersRoot.resolve(prefix).resolve(relativePath);

            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        Path fallback = this.shadersRoot.resolve(relativePath);
        return Files.isRegularFile(fallback) ? fallback : null;
    }

    private String readShaderText(Path path) {
        return flattenIncludes(path, new LinkedHashMap<>());
    }

    private String flattenIncludes(Path path, Map<Path, Boolean> includeStack) {
        Path normalizedPath = path.normalize();

        if (!normalizedPath.startsWith(this.shadersRoot)) {
            throw new RuntimeException("Shader include escapes pack root: " + path);
        }

        if (includeStack.containsKey(normalizedPath)) {
            throw new RuntimeException("Circular shader include detected: " + normalizedPath);
        }

        includeStack.put(normalizedPath, Boolean.TRUE);

        try {
            List<String> output = new ArrayList<>();

            for (String line : Files.readAllLines(normalizedPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                String includeTarget = parseIncludeTarget(trimmed);

                if (includeTarget == null) {
                    output.add(line);
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
