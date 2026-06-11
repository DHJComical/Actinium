package me.decce.gnetum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.decce.gnetum.util.AnyBooleanValue;
import me.decce.gnetum.util.TriStateBoolean;
import me.decce.gnetum.util.TwoStateBoolean;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class GnetumConfig {
    public static final int UNLIMITED_FPS = 125; // when maxFps is set to this value it means unlimited
    private static Path path;
    private static Path pathOld;

    public TwoStateBoolean enabled = new TwoStateBoolean(AnyBooleanValue.ON);
    public TwoStateBoolean showHudFps = new TwoStateBoolean(AnyBooleanValue.ON);
    public int numberOfPasses = 3;
    public int maxFps = 60;

    public HashMap<String, CacheSetting> mapVanillaElements = new HashMap<>();
    public HashMap<String, CacheSetting> mapModdedElementsPre = new HashMap<>();
    public HashMap<String, CacheSetting> mapModdedElementsPost = new HashMap<>();

    public boolean isEnabled() {
        return enabled.get() && OpenGlHelper.isFramebufferEnabled();
    }

    private static void importOld(GnetumConfig config) {
        if (Files.exists(pathOld)) {
            try {
                Configuration configuration = new Configuration(pathOld.toFile());
                config.enabled.value = configuration.get("general", "enabled", true).getBoolean() ? AnyBooleanValue.ON : AnyBooleanValue.AUTO;
                config.mapVanillaElements.get(Gnetum.HAND_ELEMENT).enabled.value = configuration.get("general", "bufferHand", false).getBoolean() ? AnyBooleanValue.ON : AnyBooleanValue.AUTO;
                Files.delete(pathOld);
                config.save();
                Gnetum.LOGGER.info("Successfully imported configuration from file \"Gnetum.cfg\"!");
            } catch (Throwable ignored) {}
        }
    }

    private static GnetumConfig createDefault() {
        GnetumConfig config = new GnetumConfig();
        config.mapVanillaElements.put(Gnetum.HAND_ELEMENT, new CacheSetting(1, new TriStateBoolean(AnyBooleanValue.AUTO, false)));
        importOld(config);
        return config;
    }

    public static GnetumConfig load(Path path) {
        GnetumConfig.path = path.resolve("gnetum.json");
        GnetumConfig.pathOld = path.resolve("Gnetum.cfg");
        try {
            if (!Files.exists(GnetumConfig.path)) return createDefault();
            String json = FileUtils.readFileToString(GnetumConfig.path.toFile(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            GnetumConfig config = gson.fromJson(json, GnetumConfig.class);
            if (!config.mapVanillaElements.containsKey(Gnetum.HAND_ELEMENT)) {
                config.mapVanillaElements.put(Gnetum.HAND_ELEMENT, new CacheSetting(1, new TriStateBoolean(AnyBooleanValue.AUTO, false)));
            }
            config.mapVanillaElements.get(Gnetum.HAND_ELEMENT).enabled.defaultValue = false;
            config.validate();
            return config;
        } catch (IOException e) {
            Gnetum.LOGGER.error("Failed to read configuration!", e);
        }
        return createDefault();
    }

    public void validate() {
        this.numberOfPasses = clamp(this.numberOfPasses, 2, 10);
        this.mapVanillaElements.forEach((s, c) -> c.pass = clamp(c.pass, 1, this.numberOfPasses));
        this.mapModdedElementsPre.forEach((s, c) -> c.pass = clamp(c.pass, 1, this.numberOfPasses));
        this.mapModdedElementsPost.forEach((s, c) -> c.pass = clamp(c.pass, 1, this.numberOfPasses));
        this.maxFps = clamp(this.maxFps, 1, UNLIMITED_FPS);
        this.hideElementsOrphanOrUncached();
    }

    private void hideElementsOrphanOrUncached() {
        mapModdedElementsPre.entrySet().stream()
                .filter(entry -> !Gnetum.OTHER_MODS.equals(entry.getKey()) && !Loader.isModLoaded(entry.getKey()) || Gnetum.uncachedElements.has(entry.getKey(), ElementType.PRE))
                .forEach(entry -> entry.getValue().hidden = true);
        mapModdedElementsPost.entrySet().stream()
                .filter(entry -> !Gnetum.OTHER_MODS.equals(entry.getKey()) && !Loader.isModLoaded(entry.getKey()) || Gnetum.uncachedElements.has(entry.getKey(), ElementType.POST))
                .forEach(entry -> entry.getValue().hidden = true);
        mapVanillaElements.forEach((s, c) -> {
            int semicolon = s.indexOf(':');
            if (semicolon == -1) return;
            String modid = s.substring(0, semicolon);
            if (!Loader.isModLoaded(modid) || Gnetum.uncachedElements.has(s)) c.hidden = true;
        });
    }

    private static int clamp(int value, int min, int max) { // min & max: inclusive
        return Math.max(Math.min(value, max), min);
    }

    public static void reload() {
        Gnetum.config = load(path);
    }

    public static void reset() {
        Gnetum.config = createDefault();
        Gnetum.config.save();
    }

    public void save() {
        this.validate();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        try {
            FileUtils.writeStringToFile(path.toFile(), json, StandardCharsets.UTF_8, false);
        } catch (IOException e) {
            Gnetum.LOGGER.error("Failed to save config file!", e);
        }
        if (PerformanceAnalyzer.latestAnalysisResult != null) {
            PerformanceAnalyzer.latestAnalysisResult.markOutdated();
        }
    }
}