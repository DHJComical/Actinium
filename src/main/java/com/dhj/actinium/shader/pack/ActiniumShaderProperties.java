package com.dhj.actinium.shader.pack;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class ActiniumShaderProperties {
    public static final ActiniumShaderProperties EMPTY = new ActiniumShaderProperties();

    private @Nullable String cloudSetting;
    private boolean oldHandLight;
    private boolean separateAo;
    private boolean weather;
    private boolean weatherParticles = true;
    private boolean shadowTerrain = true;
    private boolean shadowTranslucent = true;
    private boolean shadowEntities = true;
    private boolean shadowPlayer = true;
    private boolean shadowBlockEntities = true;
    private boolean shadowEnabled = true;
    private boolean prepareBeforeShadow;
    private final Map<String, String> conditionallyEnabledPrograms = new LinkedHashMap<>();

    public static ActiniumShaderProperties parse(Properties properties) {
        ActiniumShaderProperties parsed = new ActiniumShaderProperties();

        properties.forEach((keyObject, valueObject) -> {
            String key = keyObject.toString();
            String value = valueObject.toString().trim();

            switch (key) {
                case "clouds" -> parsed.cloudSetting = value;
                case "oldHandLight" -> parsed.oldHandLight = parseBoolean(value, parsed.oldHandLight);
                case "separateAo" -> parsed.separateAo = parseBoolean(value, parsed.separateAo);
                case "shadowTerrain" -> parsed.shadowTerrain = parseBoolean(value, parsed.shadowTerrain);
                case "shadowTranslucent" -> parsed.shadowTranslucent = parseBoolean(value, parsed.shadowTranslucent);
                case "shadowEntities" -> parsed.shadowEntities = parseBoolean(value, parsed.shadowEntities);
                case "shadowPlayer" -> parsed.shadowPlayer = parseBoolean(value, parsed.shadowPlayer);
                case "shadowBlockEntities" -> parsed.shadowBlockEntities = parseBoolean(value, parsed.shadowBlockEntities);
                case "shadow.enabled" -> parsed.shadowEnabled = parseBoolean(value, parsed.shadowEnabled);
                case "prepareBeforeShadow" -> parsed.prepareBeforeShadow = parseBoolean(value, parsed.prepareBeforeShadow);
                case "weather" -> parsed.parseWeather(value);
                default -> parsed.tryParseProgramDirective(key, value);
            }
        });

        return parsed;
    }

    public @Nullable String getCloudSetting() {
        return this.cloudSetting;
    }

    public boolean isOldHandLight() {
        return this.oldHandLight;
    }

    public boolean isSeparateAo() {
        return this.separateAo;
    }

    public boolean isWeather() {
        return this.weather;
    }

    public boolean isWeatherParticles() {
        return this.weatherParticles;
    }

    public boolean isShadowTerrain() {
        return this.shadowTerrain;
    }

    public boolean isShadowTranslucent() {
        return this.shadowTranslucent;
    }

    public boolean isShadowEntities() {
        return this.shadowEntities;
    }

    public boolean isShadowPlayer() {
        return this.shadowPlayer;
    }

    public boolean isShadowBlockEntities() {
        return this.shadowBlockEntities;
    }

    public boolean isShadowEnabled() {
        return this.shadowEnabled;
    }

    public boolean isPrepareBeforeShadow() {
        return this.prepareBeforeShadow;
    }

    public Map<String, String> getConditionallyEnabledPrograms() {
        return Collections.unmodifiableMap(this.conditionallyEnabledPrograms);
    }

    private void parseWeather(String value) {
        String[] parts = value.toLowerCase(Locale.ROOT).split("\\s+");

        if (parts.length > 0) {
            this.weather = parseBoolean(parts[0], this.weather);
        }

        if (parts.length > 1) {
            this.weatherParticles = parseBoolean(parts[1], this.weatherParticles);
        }
    }

    private void tryParseProgramDirective(String key, String value) {
        if (!key.startsWith("program.") || !key.endsWith(".enabled")) {
            return;
        }

        int suffixStart = key.length() - ".enabled".length();
        String programName = key.substring("program.".length(), suffixStart);

        if (!programName.isEmpty()) {
            this.conditionallyEnabledPrograms.put(programName, value);
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes" -> true;
            case "false", "off", "no" -> false;
            default -> fallback;
        };
    }
}
