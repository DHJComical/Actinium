package com.dhj.actinium.shader.pack;

import com.dhj.actinium.block_rendering.ActiniumBlockRenderingSettings;
import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.celeritas.shader_overrides.ActiniumTerrainPass;
import com.dhj.actinium.shader.options.ActiniumShaderOption;
import com.dhj.actinium.shader.options.ActiniumShaderOptionMenu;
import com.dhj.actinium.shader.options.ActiniumShaderOptionMenuLoader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ActiniumShaderPackManager {
    public static final String BUILTIN_PACK_NAME = "Actinium Shader";
    public static final int MAX_TERRAIN_DEBUG_MODE = 13;
    private static final Path ROOT_SHADERPACKS_DIRECTORY = Paths.get("shaderpacks");
    private static final Path DEV_CLIENT_DIRECTORY = Paths.get("run", "client");
    private static final Path DEV_SHADERPACKS_DIRECTORY = DEV_CLIENT_DIRECTORY.resolve("shaderpacks");

    private static final ActiniumShaderPack BUILTIN_PACK = new ActiniumShaderPack(BUILTIN_PACK_NAME, null, true);
    private static ActiniumShaderConfig config;
    private static ActiniumShaderPackResources activePackResources;
    private static ActiniumShaderProperties activeShaderProperties = ActiniumShaderProperties.EMPTY;
    private static ActiniumIdMap activeIdMap = ActiniumIdMap.EMPTY;
    private static @Nullable ActiniumShaderOptionMenu activeOptionMenu;
    private static int reloadVersion;

    private ActiniumShaderPackManager() {
    }

    public static void initialize() {
        getConfig();
        try {
            ensureShaderPacksDirectory();
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to create shaderpacks directory", e);
        }
        reload();
    }

    public static boolean areShadersEnabled() {
        return getConfig().areShadersEnabled();
    }

    public static @Nullable String getSelectedPackName() {
        return getConfig().getSelectedPack();
    }

    public static boolean isShaderToggleEnabled() {
        return getConfig().shadersEnabled;
    }

    public static boolean isDebugEnabled() {
        return getConfig().isDebugEnabled();
    }

    public static int getTerrainDebugMode() {
        return clampTerrainDebugMode(getConfig().getTerrainDebugMode());
    }

    public static void setTerrainDebugMode(int terrainDebugMode) {
        ActiniumShaderConfig config = getConfig();
        config.setTerrainDebugMode(clampTerrainDebugMode(terrainDebugMode));
        config.save();
        reload();
    }

    private static int clampTerrainDebugMode(int terrainDebugMode) {
        return Math.max(0, Math.min(MAX_TERRAIN_DEBUG_MODE, terrainDebugMode));
    }

    public static void setDebugEnabled(boolean debugEnabled) {
        ActiniumShaderConfig config = getConfig();
        config.setDebugEnabled(debugEnabled);
        config.save();
        reload();
    }

    public static void applySelection(@Nullable String packName, boolean shadersEnabled) {
        ActiniumShaderConfig config = getConfig();
        config.setSelectedPack(packName);
        config.setShadersEnabled(shadersEnabled);

        if (config.getSelectedPack() == null) {
            config.setShadersEnabled(false);
        }

        config.save();
        reload();
    }

    public static List<ActiniumShaderPack> discoverPacks() {
        List<ActiniumShaderPack> packs = new ArrayList<>();
        packs.add(BUILTIN_PACK);

        Path directory;

        try {
            directory = ensureShaderPacksDirectory();
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to access shaderpacks directory", e);
            return packs;
        }

        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(ActiniumShaderPackManager::isShaderPackCandidate)
                    .map(path -> new ActiniumShaderPack(path.getFileName().toString(), path, false))
                    .sorted(Comparator.comparing(pack -> pack.name().toLowerCase(Locale.ROOT)))
                    .forEach(packs::add);
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to enumerate shader packs", e);
        }

        return packs;
    }

    public static boolean isBuiltinPack(@Nullable String packName) {
        return BUILTIN_PACK_NAME.equals(packName);
    }

    public static boolean openShaderPacksDirectory() {
        Path directory;

        try {
            directory = ensureShaderPacksDirectory();
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to create shaderpacks directory", e);
            return false;
        }

        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        try {
            Desktop.getDesktop().open(directory.toFile());
            return true;
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to open shaderpacks directory", e);
            return false;
        }
    }

    public static Path getShaderPacksDirectory() {
        if (Files.isDirectory(DEV_SHADERPACKS_DIRECTORY) || Files.isDirectory(DEV_CLIENT_DIRECTORY)) {
            return DEV_SHADERPACKS_DIRECTORY;
        }

        return ROOT_SHADERPACKS_DIRECTORY;
    }

    public static @Nullable ActiniumShaderPack findPackByName(@Nullable String packName) {
        if (packName == null) {
            return null;
        }

        if (isBuiltinPack(packName)) {
            return BUILTIN_PACK;
        }

        for (ActiniumShaderPack pack : discoverPacks()) {
            if (pack.name().equals(packName)) {
                return pack;
            }
        }

        return null;
    }

    public static void reload() {
        closeActiveResources();
        clearRuntimeState();

        if (!areShadersEnabled()) {
            reloadVersion++;
            ActiniumBlockRenderingSettings.INSTANCE.reloadRendererIfRequired();
            ActiniumShaders.logger().info("Actinium shaders disabled");
            return;
        }

        String selectedPackName = getSelectedPackName();
        ActiniumShaderPack selectedPack = findPackByName(selectedPackName);

        if (selectedPack == null) {
            reloadVersion++;
            ActiniumBlockRenderingSettings.INSTANCE.reloadRendererIfRequired();
            ActiniumShaders.logger().warn("Selected shader pack '{}' could not be found; falling back to bundled shaders", selectedPackName);
            return;
        }

        try {
            activePackResources = ActiniumShaderPackResources.load(selectedPack, getPackOptionOverrides(selectedPack.name()));
            activeShaderProperties = activePackResources.shaderProperties();
            activeIdMap = activePackResources.idMap();
            activeOptionMenu = activePackResources.isBuiltin() ? null : ActiniumShaderOptionMenuLoader.load(activePackResources);
            applyRuntimeState(activeShaderProperties, activeIdMap);
            reloadVersion++;
            ActiniumBlockRenderingSettings.INSTANCE.reloadRendererIfRequired();

            if (activePackResources.isBuiltin()) {
                ActiniumShaders.logger().info("Using bundled Actinium shader resources");
            } else {
                ActiniumShaders.logger().info("Loaded Actinium shader pack '{}' from {}", activePackResources.packName(), activePackResources.packPath());
                if (isDebugEnabled()) {
                    ActiniumShaders.logger().info(
                            "[DEBUG] Parsed shader directives: sunPathRotation={}, shadowDistance={}, shadowResolution={}, shadowIntervalSize={}, shadowNear={}, shadowFar={}, shadowDistanceRenderMul={}, hardwareFiltering={}",
                            activeShaderProperties.getSunPathRotation(),
                            activeShaderProperties.getShadowDistance(),
                            activeShaderProperties.getShadowMapResolution(),
                            activeShaderProperties.getShadowIntervalSize(),
                            activeShaderProperties.getShadowNearPlane(),
                            activeShaderProperties.getShadowFarPlane(),
                            activeShaderProperties.getShadowDistanceRenderMul(),
                            activeShaderProperties.isShadowHardwareFiltering()
                    );
                }
            }
        } catch (IOException e) {
            reloadVersion++;
            ActiniumBlockRenderingSettings.INSTANCE.reloadRendererIfRequired();
            ActiniumShaders.logger().error("Failed to load shader pack '{}'", selectedPack.name(), e);
        }
    }

    public static @Nullable String getShaderSource(String name) {
        if (activePackResources == null) {
            return null;
        }

        return activePackResources.readShaderSource(name);
    }

    public static @Nullable String getProgramSource(ActiniumTerrainPass pass, ShaderType type) {
        if (activePackResources == null) {
            return null;
        }

        return activePackResources.readProgramSource(pass, type);
    }

    public static @Nullable String getProgramSource(String programName, ShaderType type) {
        if (activePackResources == null) {
            return null;
        }

        return activePackResources.readProgramSource(programName, type);
    }

    public static int getReloadVersion() {
        return reloadVersion;
    }

    public static @Nullable ActiniumShaderPackResources getActivePackResources() {
        return activePackResources;
    }

    public static @Nullable String getEffectiveOptionValue(String optionName) {
        if (activeOptionMenu == null || optionName == null || optionName.isBlank()) {
            return null;
        }

        ActiniumShaderOption option = activeOptionMenu.getOption(optionName);

        if (option == null) {
            return null;
        }

        Map<String, String> normalizedOverrides = activeOptionMenu.normalizeOverrides(getPackOptionOverrides(getSelectedPackName()));
        String overrideValue = normalizedOverrides.get(option.name());

        if (overrideValue != null && option.acceptsValue(overrideValue)) {
            return overrideValue;
        }

        return option.getDefaultSerializedValue();
    }

    public static Map<String, String> getPackOptionOverrides(@Nullable String packName) {
        return new LinkedHashMap<>(getConfig().getPackOptionOverrides(packName));
    }

    public static void savePackOptionOverrides(@Nullable String packName, Map<String, String> overrides) {
        ActiniumShaderConfig config = getConfig();
        config.setPackOptionOverrides(packName, overrides);
        config.save();
    }

    public static @Nullable ActiniumShaderOptionMenu loadShaderOptionMenu(@Nullable String packName) {
        return loadShaderOptionMenu(packName, getPackOptionOverrides(packName));
    }

    public static @Nullable ActiniumShaderOptionMenu loadShaderOptionMenu(@Nullable String packName, Map<String, String> overrides) {
        ActiniumShaderPack pack = findPackByName(packName);

        if (pack == null || pack.builtin()) {
            return null;
        }

        try (ActiniumShaderPackResources resources = ActiniumShaderPackResources.load(pack, overrides)) {
            return ActiniumShaderOptionMenuLoader.load(resources);
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to load shader option menu for '{}'", pack.name(), e);
            return null;
        }
    }

    public static ActiniumShaderProperties getActiveShaderProperties() {
        return activeShaderProperties;
    }

    public static ActiniumIdMap getActiveIdMap() {
        return activeIdMap;
    }

    private static boolean isShaderPackCandidate(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if (fileName.startsWith(".")) {
            return false;
        }

        return Files.isDirectory(path) || fileName.endsWith(".zip");
    }

    private static Path ensureShaderPacksDirectory() throws IOException {
        Path directory = getShaderPacksDirectory();

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        return directory;
    }

    private static ActiniumShaderConfig getConfig() {
        if (config == null) {
            try {
                config = ActiniumShaderConfig.load();
            } catch (Exception e) {
                ActiniumShaders.logger().error("Failed to load Actinium shader config", e);
                config = new ActiniumShaderConfig();
                config.setReadOnly();
            }
        }

        return config;
    }

    private static void closeActiveResources() {
        if (activePackResources != null) {
            activePackResources.close();
            activePackResources = null;
        }
    }

    private static void clearRuntimeState() {
        activeShaderProperties = ActiniumShaderProperties.EMPTY;
        activeIdMap = ActiniumIdMap.EMPTY;
        activeOptionMenu = null;
        ActiniumBlockRenderingSettings.INSTANCE.clearLocalOverrides();
    }

    private static void applyRuntimeState(ActiniumShaderProperties shaderProperties, ActiniumIdMap idMap) {
        ActiniumBlockRenderingSettings settings = ActiniumBlockRenderingSettings.INSTANCE;
        settings.setBlockMetaMatches(idMap.getBlockMetaMatches());
        settings.setBlockTypeIds(idMap.getBlockTypeIds());
        settings.setUseSeparateAo(shaderProperties.isSeparateAo());
        settings.setUseExtendedVertexFormat(true);
    }
}
