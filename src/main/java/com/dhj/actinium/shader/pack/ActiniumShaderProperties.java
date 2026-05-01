package com.dhj.actinium.shader.pack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class ActiniumShaderProperties {
    public static final ActiniumShaderProperties EMPTY = new ActiniumShaderProperties();
    private static final float DEFAULT_SUN_PATH_ROTATION = 0.0f;
    private static final float DEFAULT_SHADOW_INTERVAL_SIZE = 2.0f;
    private static final int DEFAULT_SHADOW_MAP_RESOLUTION = 1024;
    private static final float DEFAULT_SHADOW_DISTANCE = 160.0f;
    private static final float DEFAULT_SHADOW_NEAR_PLANE = 0.05f;
    private static final float DEFAULT_SHADOW_FAR_PLANE = 256.0f;
    private static final float DEFAULT_SHADOW_DISTANCE_RENDER_MUL = -1.0f;
    private static final float DEFAULT_ENTITY_SHADOW_DISTANCE_MUL = 1.0f;
    private static final boolean DEFAULT_SHADOW_HARDWARE_FILTERING = false;

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
    private final Map<String, Map<String, String>> stageTexturePaths = new LinkedHashMap<>();
    private final Map<String, Map<Integer, Boolean>> explicitFlips = new LinkedHashMap<>();
    private @Nullable String noiseTexturePath;
    private final List<String> sliderOptions = new ArrayList<>();
    private final Map<String, List<String>> profiles = new LinkedHashMap<>();
    private final Map<String, List<String>> profiles2 = new LinkedHashMap<>();
    private @Nullable List<String> mainScreenOptions;
    private final Map<String, List<String>> subScreenOptions = new LinkedHashMap<>();
    private @Nullable Integer mainScreenColumnCount;
    private final Map<String, Integer> subScreenColumnCount = new LinkedHashMap<>();
    private float sunPathRotation = DEFAULT_SUN_PATH_ROTATION;
    private float shadowIntervalSize = DEFAULT_SHADOW_INTERVAL_SIZE;
    private int shadowMapResolution = DEFAULT_SHADOW_MAP_RESOLUTION;
    private float shadowDistance = DEFAULT_SHADOW_DISTANCE;
    private float shadowNearPlane = DEFAULT_SHADOW_NEAR_PLANE;
    private float shadowFarPlane = DEFAULT_SHADOW_FAR_PLANE;
    private float shadowDistanceRenderMul = DEFAULT_SHADOW_DISTANCE_RENDER_MUL;
    private float entityShadowDistanceMul = DEFAULT_ENTITY_SHADOW_DISTANCE_MUL;
    private boolean shadowHardwareFiltering = DEFAULT_SHADOW_HARDWARE_FILTERING;

    public static ActiniumShaderProperties parse(Properties properties) {
        return parse(properties, Collections.emptyMap(), Collections.emptyList());
    }

    public static ActiniumShaderProperties parse(Properties properties, Iterable<String> shaderSources) {
        return parse(properties, Collections.emptyMap(), shaderSources);
    }

    public static ActiniumShaderProperties parse(Properties properties, Map<String, String> rawProperties, Iterable<String> shaderSources) {
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

            parsed.tryParseTextureDirective(key, value);
            parsed.tryParseFlipDirective(key, value);
        });

        DirectiveSourceParser.parseInto(parsed, shaderSources);
        parsed.parseMenuMetadata(rawProperties);
        return parsed;
    }

    public void applyRuntimeOverrides(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }

        overrides.forEach((keyObject, valueObject) -> {
            String key = keyObject != null ? keyObject.trim() : "";
            String value = valueObject != null ? valueObject.trim() : "";

            switch (key) {
                case "clouds" -> this.cloudSetting = value;
                case "oldHandLight" -> this.oldHandLight = parseBoolean(value, this.oldHandLight);
                case "separateAo" -> this.separateAo = parseBoolean(value, this.separateAo);
                case "shadowTerrain" -> this.shadowTerrain = parseBoolean(value, this.shadowTerrain);
                case "shadowTranslucent" -> this.shadowTranslucent = parseBoolean(value, this.shadowTranslucent);
                case "shadowEntities" -> this.shadowEntities = parseBoolean(value, this.shadowEntities);
                case "shadowPlayer" -> this.shadowPlayer = parseBoolean(value, this.shadowPlayer);
                case "shadowBlockEntities" -> this.shadowBlockEntities = parseBoolean(value, this.shadowBlockEntities);
                case "shadow.enabled", "shadowEnabled" -> this.shadowEnabled = parseBoolean(value, this.shadowEnabled);
                case "prepareBeforeShadow" -> this.prepareBeforeShadow = parseBoolean(value, this.prepareBeforeShadow);
                case "sunPathRotation" -> this.sunPathRotation = parseFloat(value, this.sunPathRotation);
                case "shadowIntervalSize" -> this.shadowIntervalSize = parseFloat(value, this.shadowIntervalSize);
                case "shadowMapResolution" -> this.shadowMapResolution = parseInteger(value, this.shadowMapResolution);
                case "shadowDistance" -> this.shadowDistance = parseFloat(value, this.shadowDistance);
                case "shadowNearPlane" -> this.shadowNearPlane = parseFloat(value, this.shadowNearPlane);
                case "shadowFarPlane" -> this.shadowFarPlane = parseFloat(value, this.shadowFarPlane);
                case "shadowDistanceRenderMul" -> this.shadowDistanceRenderMul = parseFloat(value, this.shadowDistanceRenderMul);
                case "entityShadowDistanceMul" -> this.entityShadowDistanceMul = parseFloat(value, this.entityShadowDistanceMul);
                case "shadowHardwareFiltering" -> this.shadowHardwareFiltering = parseBoolean(value, this.shadowHardwareFiltering);
                case "weather" -> this.parseWeather(value);
                default -> {
                }
            }
        });
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

    public float getSunPathRotation() {
        return this.sunPathRotation;
    }

    public float getShadowIntervalSize() {
        return this.shadowIntervalSize;
    }

    public int getShadowMapResolution() {
        return this.shadowMapResolution;
    }

    public float getShadowDistance() {
        return this.shadowDistance;
    }

    public float getShadowNearPlane() {
        return this.shadowNearPlane;
    }

    public float getShadowFarPlane() {
        return this.shadowFarPlane;
    }

    public float getShadowDistanceRenderMul() {
        return this.shadowDistanceRenderMul;
    }

    public float getEntityShadowDistanceMul() {
        return this.entityShadowDistanceMul;
    }

    public boolean isShadowHardwareFiltering() {
        return this.shadowHardwareFiltering;
    }

    public Map<String, String> getConditionallyEnabledPrograms() {
        return Collections.unmodifiableMap(this.conditionallyEnabledPrograms);
    }

    public @Nullable String getStageTexturePath(String stageName, String samplerName) {
        Map<String, String> stageTextures = this.stageTexturePaths.get(stageName);
        return stageTextures != null ? stageTextures.get(samplerName) : null;
    }

    public @Nullable String getNoiseTexturePath() {
        return this.noiseTexturePath;
    }

    public Map<String, Map<String, String>> getStageTexturePaths() {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        this.stageTexturePaths.forEach((stage, samplers) -> copy.put(stage, Collections.unmodifiableMap(new LinkedHashMap<>(samplers))));
        return Collections.unmodifiableMap(copy);
    }

    public Map<Integer, Boolean> getExplicitFlips(String passName) {
        if (passName == null || passName.isBlank()) {
            return Collections.emptyMap();
        }

        Map<Integer, Boolean> flips = this.explicitFlips.get(passName);
        if (flips == null || flips.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(flips));
    }

    public List<String> getSliderOptions() {
        return Collections.unmodifiableList(this.sliderOptions);
    }

    public Map<String, List<String>> getProfiles() {
        return immutableListMap(this.profiles);
    }

    public Map<String, List<String>> getProfiles2() {
        return immutableListMap(this.profiles2);
    }

    public @Nullable List<String> getMainScreenOptions() {
        return this.mainScreenOptions != null ? Collections.unmodifiableList(this.mainScreenOptions) : null;
    }

    public Map<String, List<String>> getSubScreenOptions() {
        return immutableListMap(this.subScreenOptions);
    }

    public @Nullable Integer getMainScreenColumnCount() {
        return this.mainScreenColumnCount;
    }

    public Map<String, Integer> getSubScreenColumnCount() {
        return Collections.unmodifiableMap(this.subScreenColumnCount);
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

    private void tryParseTextureDirective(String key, String value) {
        if (!key.startsWith("texture.")) {
            return;
        }

        if ("texture.noise".equals(key)) {
            this.noiseTexturePath = value;
            return;
        }

        String remainder = key.substring("texture.".length());
        int separator = remainder.indexOf('.');

        if (separator <= 0 || separator >= remainder.length() - 1) {
            return;
        }

        String stageName = remainder.substring(0, separator).trim();
        String samplerName = remainder.substring(separator + 1).trim();

        if (stageName.isEmpty() || samplerName.isEmpty()) {
            return;
        }

        this.stageTexturePaths
                .computeIfAbsent(stageName, ignored -> new LinkedHashMap<>())
                .put(samplerName, value);
    }

    private void tryParseFlipDirective(String key, String value) {
        if (!key.startsWith("flip.")) {
            return;
        }

        String remainder = key.substring("flip.".length()).trim();
        int separator = remainder.lastIndexOf('.');

        if (separator <= 0 || separator >= remainder.length() - 1) {
            return;
        }

        String passName = remainder.substring(0, separator).trim();
        String bufferName = remainder.substring(separator + 1).trim();

        if (passName.isEmpty() || bufferName.isEmpty()) {
            return;
        }

        int targetIndex = resolveRenderTargetIndex(bufferName);
        if (targetIndex < 0) {
            return;
        }

        Map<Integer, Boolean> passFlips = this.explicitFlips.computeIfAbsent(passName, ignored -> new LinkedHashMap<>());
        boolean fallback = passFlips.getOrDefault(targetIndex, false);
        passFlips.put(targetIndex, parseBoolean(value, fallback));
    }

    private void parseMenuMetadata(Map<String, String> rawProperties) {
        rawProperties.forEach((key, value) -> {
            String trimmedValue = value.trim();

            if ("sliders".equals(key)) {
                this.sliderOptions.clear();
                this.sliderOptions.addAll(splitWhitespaceList(trimmedValue));
                return;
            }

            if (key.startsWith("profile2.")) {
                String profileName = key.substring("profile2.".length()).trim();

                if (!profileName.isEmpty()) {
                    this.profiles2.put(profileName, splitWhitespaceList(trimmedValue));
                }
                return;
            }

            if (key.startsWith("profile.")) {
                String profileName = key.substring("profile.".length()).trim();

                if (!profileName.isEmpty()) {
                    this.profiles.put(profileName, splitWhitespaceList(trimmedValue));
                }
                return;
            }

            if ("screen.columns".equals(key)) {
                this.mainScreenColumnCount = parseInteger(trimmedValue, this.mainScreenColumnCount);
                return;
            }

            if ("screen".equals(key)) {
                this.mainScreenOptions = splitWhitespaceList(trimmedValue);
                return;
            }

            if (key.startsWith("screen.") && key.endsWith(".columns")) {
                String screenName = key.substring("screen.".length(), key.length() - ".columns".length()).trim();

                if (!screenName.isEmpty()) {
                    this.subScreenColumnCount.put(screenName, parseInteger(trimmedValue, this.subScreenColumnCount.get(screenName)));
                }
                return;
            }

            if (key.startsWith("screen.")) {
                String screenName = key.substring("screen.".length()).trim();

                if (!screenName.isEmpty()) {
                    this.subScreenOptions.put(screenName, splitWhitespaceList(trimmedValue));
                }
            }
        });
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes" -> true;
            case "false", "off", "no" -> false;
            default -> fallback;
        };
    }

    private static Integer parseInteger(String value, @Nullable Integer fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static List<String> splitWhitespaceList(String value) {
        if (value.isBlank()) {
            return List.of();
        }

        String[] split = value.trim().split("\\s+");
        List<String> values = new ArrayList<>(split.length);

        for (String entry : split) {
            if (!entry.isBlank()) {
                values.add(entry);
            }
        }

        return values;
    }

    private static Map<String, List<String>> immutableListMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, Collections.unmodifiableList(new ArrayList<>(value))));
        return Collections.unmodifiableMap(copy);
    }

    private static int resolveRenderTargetIndex(String bufferName) {
        String normalized = bufferName.trim().toLowerCase(Locale.ROOT);

        if (normalized.startsWith("colortex")) {
            try {
                int index = Integer.parseInt(normalized.substring("colortex".length()));
                return index >= 0 && index <= 7 ? index : -1;
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        return switch (normalized) {
            case "gcolor" -> 0;
            case "gdepth" -> 1;
            case "gnormal" -> 2;
            case "composite" -> 3;
            case "gaux1" -> 4;
            case "gaux2" -> 5;
            case "gaux3" -> 6;
            case "gaux4" -> 7;
            default -> -1;
        };
    }

    private static final class DirectiveSourceParser {
        private static final String[] COMPARISON_OPERATORS = {"==", "!=", ">=", "<=", ">", "<"};

        private DirectiveSourceParser() {
        }

        public static void parseInto(ActiniumShaderProperties target, Iterable<String> shaderSources) {
            for (String shaderSource : shaderSources) {
                if (shaderSource == null || shaderSource.isBlank()) {
                    continue;
                }

                parseSource(target, shaderSource);
            }
        }

        private static void parseSource(ActiniumShaderProperties target, String shaderSource) {
            Map<String, String> defines = new HashMap<>();
            java.util.ArrayList<ConditionalFrame> stack = new java.util.ArrayList<>();
            boolean active = true;

            for (String rawLine : shaderSource.split("\\R")) {
                String line = stripInlineComment(rawLine).trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("#ifdef ")) {
                    String name = line.substring("#ifdef ".length()).trim();
                    boolean condition = defines.containsKey(name);
                    stack.add(new ConditionalFrame(active, condition, active && condition));
                    active = active && condition;
                    continue;
                }

                if (line.startsWith("#ifndef ")) {
                    String name = line.substring("#ifndef ".length()).trim();
                    boolean condition = !defines.containsKey(name);
                    stack.add(new ConditionalFrame(active, condition, active && condition));
                    active = active && condition;
                    continue;
                }

                if (line.startsWith("#if ")) {
                    boolean condition = evaluateExpression(line.substring("#if ".length()).trim(), defines);
                    stack.add(new ConditionalFrame(active, condition, active && condition));
                    active = active && condition;
                    continue;
                }

                if (line.startsWith("#elif ")) {
                    if (!stack.isEmpty()) {
                        ConditionalFrame frame = stack.get(stack.size() - 1);
                        boolean condition = !frame.branchTaken && evaluateExpression(line.substring("#elif ".length()).trim(), defines);
                        frame.branchTaken |= condition;
                        frame.active = frame.parentActive && condition;
                        active = frame.active;
                    }
                    continue;
                }

                if (line.startsWith("#else")) {
                    if (!stack.isEmpty()) {
                        ConditionalFrame frame = stack.get(stack.size() - 1);
                        boolean condition = !frame.branchTaken;
                        frame.branchTaken = true;
                        frame.active = frame.parentActive && condition;
                        active = frame.active;
                    }
                    continue;
                }

                if (line.startsWith("#endif")) {
                    if (!stack.isEmpty()) {
                        ConditionalFrame frame = stack.remove(stack.size() - 1);
                        active = frame.parentActive;
                    }
                    continue;
                }

                if (!active) {
                    continue;
                }

                if (line.startsWith("#define ")) {
                    int valueStart = line.indexOf(' ', "#define ".length());
                    String name;
                    String value;

                    if (valueStart >= 0) {
                        name = line.substring("#define ".length(), valueStart).trim();
                        value = line.substring(valueStart + 1).trim();
                    } else {
                        name = line.substring("#define ".length()).trim();
                        value = "1";
                    }

                    if (!name.isEmpty() && !name.contains("(")) {
                        defines.put(name, value.isEmpty() ? "1" : value);
                    }
                    continue;
                }

                if (line.startsWith("#undef ")) {
                    String name = line.substring("#undef ".length()).trim();
                    defines.remove(name);
                    continue;
                }

                parseConstDirective(target, line);
            }
        }

        private static void parseConstDirective(ActiniumShaderProperties target, String line) {
            if (!line.startsWith("const ")) {
                return;
            }

            int equals = line.indexOf('=');
            if (equals < 0) {
                return;
            }

            String[] declaration = line.substring(0, equals).trim().split("\\s+");
            if (declaration.length < 3) {
                return;
            }

            String type = declaration[1];
            String name = declaration[2];
            String value = trimDirectiveValue(line.substring(equals + 1));

            switch (name) {
                case "sunPathRotation" -> target.sunPathRotation = parseFloat(value, target.sunPathRotation);
                case "shadowIntervalSize" -> target.shadowIntervalSize = parseFloat(value, target.shadowIntervalSize);
                case "shadowMapResolution" -> target.shadowMapResolution = Math.max(1, parseInt(value, target.shadowMapResolution));
                case "shadowDistance" -> target.shadowDistance = parseFloat(value, target.shadowDistance);
                case "shadowNearPlane" -> target.shadowNearPlane = parseFloat(value, target.shadowNearPlane);
                case "shadowFarPlane" -> target.shadowFarPlane = parseFloat(value, target.shadowFarPlane);
                case "shadowDistanceRenderMul" -> target.shadowDistanceRenderMul = parseFloat(value, target.shadowDistanceRenderMul);
                case "entityShadowDistanceMul" -> target.entityShadowDistanceMul = parseFloat(value, target.entityShadowDistanceMul);
                case "shadowHardwareFiltering" -> {
                    if ("bool".equals(type)) {
                        target.shadowHardwareFiltering = parseBoolean(value, target.shadowHardwareFiltering);
                    }
                }
            }
        }

        private static boolean evaluateExpression(String expression, Map<String, String> defines) {
            String trimmed = trimOuterParentheses(expression.trim());

            int operatorIndex = findTopLevelOperator(trimmed, "||");
            if (operatorIndex >= 0) {
                return evaluateExpression(trimmed.substring(0, operatorIndex), defines)
                        || evaluateExpression(trimmed.substring(operatorIndex + 2), defines);
            }

            operatorIndex = findTopLevelOperator(trimmed, "&&");
            if (operatorIndex >= 0) {
                return evaluateExpression(trimmed.substring(0, operatorIndex), defines)
                        && evaluateExpression(trimmed.substring(operatorIndex + 2), defines);
            }

            if (trimmed.startsWith("!")) {
                return !evaluateExpression(trimmed.substring(1), defines);
            }

            if (trimmed.startsWith("defined(") && trimmed.endsWith(")")) {
                return defines.containsKey(trimmed.substring("defined(".length(), trimmed.length() - 1).trim());
            }

            for (String operator : COMPARISON_OPERATORS) {
                operatorIndex = findTopLevelOperator(trimmed, operator);
                if (operatorIndex >= 0) {
                    double left = resolveNumeric(trimmed.substring(0, operatorIndex), defines);
                    double right = resolveNumeric(trimmed.substring(operatorIndex + operator.length()), defines);
                    return switch (operator) {
                        case "==" -> left == right;
                        case "!=" -> left != right;
                        case ">=" -> left >= right;
                        case "<=" -> left <= right;
                        case ">" -> left > right;
                        case "<" -> left < right;
                        default -> false;
                    };
                }
            }

            return resolveTruthy(trimmed, defines);
        }

        private static int findTopLevelOperator(String expression, String operator) {
            int depth = 0;

            for (int i = 0; i <= expression.length() - operator.length(); i++) {
                char c = expression.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }

                if (depth == 0 && expression.startsWith(operator, i)) {
                    return i;
                }
            }

            return -1;
        }

        private static boolean resolveTruthy(String token, Map<String, String> defines) {
            String resolved = resolveToken(token.trim(), defines);

            if (resolved.isEmpty()) {
                return false;
            }

            if ("true".equalsIgnoreCase(resolved)) {
                return true;
            }

            if ("false".equalsIgnoreCase(resolved)) {
                return false;
            }

            return parseFloat(resolved, 0.0f) != 0.0f;
        }

        private static double resolveNumeric(String token, Map<String, String> defines) {
            String resolved = resolveToken(token.trim(), defines);

            if ("true".equalsIgnoreCase(resolved)) {
                return 1.0;
            }

            if ("false".equalsIgnoreCase(resolved)) {
                return 0.0;
            }

            return parseFloat(resolved, 0.0f);
        }

        private static String resolveToken(String token, Map<String, String> defines) {
            String trimmed = trimOuterParentheses(token.trim());
            String resolved = defines.get(trimmed);
            return resolved != null ? resolved.trim() : trimmed;
        }

        private static String trimDirectiveValue(String value) {
            String trimmed = value.trim();

            if (trimmed.endsWith(";")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }

            int commentStart = trimmed.indexOf("//");
            if (commentStart >= 0) {
                trimmed = trimmed.substring(0, commentStart).trim();
            }

            return trimOuterParentheses(trimmed);
        }

        private static String stripInlineComment(String line) {
            int commentStart = line.indexOf("//");
            return commentStart >= 0 ? line.substring(0, commentStart) : line;
        }

        private static String trimOuterParentheses(String value) {
            String trimmed = value.trim();

            while (trimmed.startsWith("(") && trimmed.endsWith(")") && hasBalancedOuterParentheses(trimmed)) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }

            return trimmed;
        }

        private static boolean hasBalancedOuterParentheses(String value) {
            int depth = 0;

            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0 && i < value.length() - 1) {
                        return false;
                    }
                }
            }

            return depth == 0;
        }

        private static int parseInt(String value, int fallback) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static float parseFloat(String value, float fallback) {
            try {
                return Float.parseFloat(value.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static final class ConditionalFrame {
            private final boolean parentActive;
            private boolean branchTaken;
            private boolean active;

            private ConditionalFrame(boolean parentActive, boolean branchTaken, boolean active) {
                this.parentActive = parentActive;
                this.branchTaken = branchTaken;
                this.active = active;
            }
        }
    }
}
