package com.dhj.actinium.shader.pack;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.dhj.actinium.celeritas.ActiniumShaders;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ActiniumShaderConfig {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();
    private static final String FILE_NAME = "actinium-shaderpacks.json";

    @Nullable
    public String selectedPack;
    @Setter
    public boolean shadersEnabled;
    @Setter
    @Getter
    public boolean debugEnabled;
    public Map<String, Map<String, String>> packOptionOverrides = new LinkedHashMap<>();

    private transient Path configPath;
    private transient boolean readOnly;

    public static ActiniumShaderConfig load() {
        Path path = Paths.get("config", FILE_NAME);
        ActiniumShaderConfig config;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, ActiniumShaderConfig.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse Actinium shader config", e);
            } catch (JsonSyntaxException e) {
                ActiniumShaders.logger().error("Could not parse Actinium shader config, using defaults", e);
                config = new ActiniumShaderConfig();
            }
        } else {
            config = new ActiniumShaderConfig();
        }

        if (config == null) {
            config = new ActiniumShaderConfig();
        }

        config.configPath = path;
        if (config.packOptionOverrides == null) {
            config.packOptionOverrides = new LinkedHashMap<>();
        }
        config.selectedPack = normalizePackName(config.selectedPack);
        if (config.selectedPack == null) {
            config.shadersEnabled = false;
        }
        return config;
    }

    public void save() {
        if (this.readOnly) {
            throw new IllegalStateException("Actinium shader config is read-only");
        }

        try {
            Path dir = this.configPath.getParent();

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            } else if (!Files.isDirectory(dir)) {
                throw new IOException("Not a directory: " + dir);
            }

            Path tempPath = this.configPath.resolveSibling(this.configPath.getFileName() + ".tmp");
            Files.writeString(tempPath, GSON.toJson(this));
            Files.move(tempPath, this.configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save Actinium shader config", e);
        }
    }

    public void setReadOnly() {
        this.readOnly = true;
    }

    public @Nullable String getSelectedPack() {
        return normalizePackName(this.selectedPack);
    }

    public void setSelectedPack(@Nullable String packName) {
        this.selectedPack = normalizePackName(packName);
    }

    public boolean areShadersEnabled() {
        return this.shadersEnabled && this.getSelectedPack() != null;
    }

    public boolean isShaderPackEnabled() {
        return this.areShadersEnabled();
    }

    public Map<String, String> getPackOptionOverrides(@Nullable String packName) {
        String normalizedPackName = normalizePackName(packName);

        if (normalizedPackName == null) {
            return Collections.emptyMap();
        }

        Map<String, String> overrides = this.packOptionOverrides.get(normalizedPackName);
        return overrides != null ? Collections.unmodifiableMap(overrides) : Collections.emptyMap();
    }

    public void setPackOptionOverrides(@Nullable String packName, Map<String, String> overrides) {
        String normalizedPackName = normalizePackName(packName);

        if (normalizedPackName == null) {
            return;
        }

        if (overrides.isEmpty()) {
            this.packOptionOverrides.remove(normalizedPackName);
            return;
        }

        this.packOptionOverrides.put(normalizedPackName, new LinkedHashMap<>(overrides));
    }

    private static @Nullable String normalizePackName(@Nullable String packName) {
        if (packName == null) {
            return null;
        }

        String normalized = packName.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
