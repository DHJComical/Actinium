package com.dhj.actinium.shader.pack;

import com.dhj.actinium.celeritas.ActiniumShaders;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public final class ActiniumShaderPackResources implements AutoCloseable {
    private final String packName;
    private final Path packPath;
    private final @Nullable FileSystem fileSystem;
    private final @Nullable Path shadersRoot;
    private final Properties configProperties;

    private ActiniumShaderPackResources(String packName, Path packPath, @Nullable FileSystem fileSystem, @Nullable Path shadersRoot, Properties configProperties) {
        this.packName = packName;
        this.packPath = packPath;
        this.fileSystem = fileSystem;
        this.shadersRoot = shadersRoot;
        this.configProperties = configProperties;
    }

    public static ActiniumShaderPackResources builtin() {
        return new ActiniumShaderPackResources(ActiniumShaderPackManager.BUILTIN_PACK_NAME, Path.of("."), null, null, new Properties());
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

        return new ActiniumShaderPackResources(pack.name(), packPath, fileSystem, shadersRoot, readConfigProperties(shadersRoot.resolve("config.txt")));
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

    private static Properties readConfigProperties(Path path) {
        Properties properties = new Properties();

        if (!Files.isRegularFile(path)) {
            return properties;
        }

        try (var stream = Files.newInputStream(path)) {
            properties.load(stream);
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to read shader pack config {}", path, e);
        }

        return properties;
    }

    private static String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source " + path, e);
        }
    }
}
