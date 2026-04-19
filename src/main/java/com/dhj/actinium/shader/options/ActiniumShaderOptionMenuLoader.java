package com.dhj.actinium.shader.options;

import com.dhj.actinium.shader.pack.ActiniumShaderPackResources;
import com.dhj.actinium.shader.pack.ActiniumShaderProperties;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ActiniumShaderOptionMenuLoader {
    private ActiniumShaderOptionMenuLoader() {
    }

    public static ActiniumShaderOptionMenu load(ActiniumShaderPackResources resources) {
        ActiniumShaderProperties properties = resources.shaderProperties();
        Map<String, String> translations = loadTranslations(resources);
        List<ActiniumShaderOption> options = ActiniumShaderOptionParser.parseOptions(resources, new LinkedHashSet<>(properties.getSliderOptions()));

        ActiniumShaderOptionMenu provisional = new ActiniumShaderOptionMenu(
                resources.packName(),
                options,
                translations,
                properties.getMainScreenOptions(),
                properties.getSubScreenOptions(),
                properties.getMainScreenColumnCount(),
                properties.getSubScreenColumnCount(),
                new ActiniumShaderProfileSet(new LinkedHashMap<>()),
                new ActiniumShaderProfileSet(new LinkedHashMap<>())
        );

        ActiniumShaderProfileSet profiles = ActiniumShaderProfileSet.fromTree(properties.getProfiles(), provisional);
        ActiniumShaderProfileSet profiles2 = ActiniumShaderProfileSet.fromTree(properties.getProfiles2(), provisional);

        return new ActiniumShaderOptionMenu(
                resources.packName(),
                options,
                translations,
                properties.getMainScreenOptions(),
                properties.getSubScreenOptions(),
                properties.getMainScreenColumnCount(),
                properties.getSubScreenColumnCount(),
                profiles,
                profiles2
        );
    }

    private static Map<String, String> loadTranslations(ActiniumShaderPackResources resources) {
        LinkedHashMap<String, String> translations = new LinkedHashMap<>();

        parseLangFile(resources, "en_US", translations);

        for (String candidate : getLanguageCandidates()) {
            if (!"en_US".equals(candidate)) {
                parseLangFile(resources, candidate, translations);
            }
        }

        return translations;
    }

    private static List<String> getLanguageCandidates() {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String configured = Minecraft.getMinecraft().gameSettings.language;

        if (configured != null && !configured.isBlank()) {
            candidates.add(configured);
            candidates.add(configured.replace('-', '_'));

            String[] split = configured.replace('-', '_').split("_", 2);

            if (split.length == 2) {
                candidates.add(split[0].toLowerCase() + "_" + split[1].toUpperCase());
            }
        }

        return new ArrayList<>(candidates);
    }

    private static void parseLangFile(ActiniumShaderPackResources resources, String locale, Map<String, String> translations) {
        byte[] bytes = resources.readResourceBytes("lang/" + locale + ".lang");

        if (bytes == null || bytes.length == 0) {
            return;
        }

        String content = new String(bytes, StandardCharsets.UTF_8);

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int separator = line.indexOf('=');

            if (separator < 0) {
                separator = line.indexOf(':');
            }

            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();

            if (!key.isEmpty()) {
                translations.put(key, unescape(value));
            }
        }
    }

    private static String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\=", "=").replace("\\:", ":").replace("\\\\", "\\");
    }
}
