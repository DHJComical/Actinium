package com.dhj.actinium.gui;

import net.coderbot.iris.shaderpack.LanguageMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShaderPackTranslationLookupTest {
    private static final List<String> GAME_LANGUAGE_FIRST = List.of("zh_cn", "en_us");

    @TempDir
    Path tempDir;

    @Test
    void resolvesEntryFromGameLanguage() throws IOException {
        LanguageMap languageMap = languageMap("zh_cn.lang", "option.CLOUD_DENSITY=云密度");

        String translation = ShaderPackTranslationLookup.lookup(
            languageMap, Map.of(), "option.CLOUD_DENSITY", GAME_LANGUAGE_FIRST);

        assertEquals("云密度", translation);
    }

    @Test
    void fallsBackToEnglishWhenGameLanguageFileMissing() throws IOException {
        // BSL-style pack shipping only en_US.lang: zh_cn misses and must degrade to English instead of null
        LanguageMap languageMap = languageMap("en_US.lang", "option.CLOUD_DENSITY=Cloud Density");

        String translation = ShaderPackTranslationLookup.lookup(
            languageMap, Map.of(), "option.CLOUD_DENSITY", GAME_LANGUAGE_FIRST);

        assertEquals("Cloud Density", translation);
    }

    @Test
    void prefersGameLanguageOverEnglishFallback() throws IOException {
        // photon-style pack shipping both zh_CN.lang and en_US.lang: the game language hit must not be stolen by the en_us fallback
        LanguageMap languageMap = languageMap(
            "en_US.lang", "option.CLOUD_DENSITY=Cloud Density\n",
            "zh_CN.lang", "option.CLOUD_DENSITY=云密度\n");

        String translation = ShaderPackTranslationLookup.lookup(
            languageMap, Map.of(), "option.CLOUD_DENSITY", GAME_LANGUAGE_FIRST);

        assertEquals("云密度", translation);
    }

    @Test
    void keepsVanillaTranslationWhenKeyCollides() throws IOException {
        LanguageMap languageMap = languageMap("en_us.lang", "option.CLOUD_DENSITY=Cloud Density");

        Map<String, String> vanilla = new HashMap<>();
        vanilla.put("option.CLOUD_DENSITY", "vanilla wins");

        String translation = ShaderPackTranslationLookup.lookup(
            languageMap, vanilla, "option.CLOUD_DENSITY", GAME_LANGUAGE_FIRST);

        assertNull(translation);
    }

    @Test
    void returnsNullWhenLanguageMapAbsent() {
        assertNull(ShaderPackTranslationLookup.lookup(
            null, Map.of(), "option.CLOUD_DENSITY", GAME_LANGUAGE_FIRST));
    }

    @Test
    void returnsNullWhenKeyMissingEverywhere() throws IOException {
        LanguageMap languageMap = languageMap("en_US.lang", "option.OTHER=Other");

        assertNull(ShaderPackTranslationLookup.lookup(
            languageMap, Map.of(), "option.CLOUD_DENSITY", GAME_LANGUAGE_FIRST));
    }

    @Test
    void matchesLanguageCodeCaseInsensitively() throws IOException {
        LanguageMap languageMap = languageMap("zh_CN.lang", "option.CLOUD_DENSITY=云密度");

        String translation = ShaderPackTranslationLookup.lookup(
            languageMap, Map.of(), "option.CLOUD_DENSITY", List.of("zh_CN"));

        assertEquals("云密度", translation);
    }

    private LanguageMap languageMap(String... fileNamesAndContents) throws IOException {
        for (int i = 0; i < fileNamesAndContents.length; i += 2) {
            Files.writeString(tempDir.resolve(fileNamesAndContents[i]), fileNamesAndContents[i + 1], StandardCharsets.UTF_8);
        }
        return new LanguageMap(tempDir);
    }
}
