package net.coderbot.iris.shaderpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageMapTest {
    @TempDir
    Path tempDir;

    @Test
    void normalizesLanguageFileNamesToLowerCase() throws IOException {
        Files.writeString(tempDir.resolve("en_US.lang"), "key=value\n", StandardCharsets.UTF_8);

        LanguageMap languageMap = new LanguageMap(tempDir);

        assertNotNull(languageMap.getTranslations("en_us"));
        assertEquals("value", languageMap.getTranslations("en_us").get("key"));
        assertNull(languageMap.getTranslations("en_US"));
        assertTrue(languageMap.getLanguages().contains("en_us"));
    }
}
