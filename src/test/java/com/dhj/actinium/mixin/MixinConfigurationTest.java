package com.dhj.actinium.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MixinConfigurationTest {
    private static final List<String> CONFIGS = List.of(
        "mixins.actinium.vintage.json",
        "mixins.actinium.iris.json",
        "mixins.actinium.dh.json",
        "mixins.actinium.gibbed.json",
        "mixins.actinium.lumenized.json"
    );

    @Test
    void everyDeclaredMixinClassExists() throws IOException {
        ClassLoader classLoader = MixinConfigurationTest.class.getClassLoader();

        for (String configName : CONFIGS) {
            try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(classLoader.getResourceAsStream(configName), "Missing " + configName),
                StandardCharsets.UTF_8
            )) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                String packageName = config.get("package").getAsString();
                assertDeclaredClassesExist(classLoader, configName, packageName, config.getAsJsonArray("mixins"));
                assertDeclaredClassesExist(classLoader, configName, packageName, config.getAsJsonArray("client"));
                assertDeclaredClassesExist(classLoader, configName, packageName, config.getAsJsonArray("server"));
            }
        }
    }

    private static void assertDeclaredClassesExist(
        ClassLoader classLoader,
        String configName,
        String packageName,
        JsonArray declarations
    ) {
        if (declarations == null) {
            return;
        }

        for (var declaration : declarations) {
            String className = packageName + "." + declaration.getAsString();
            String resourceName = className.replace('.', '/') + ".class";
            assertNotNull(classLoader.getResource(resourceName), configName + " references missing " + className);
            assertFalse(className.contains(".."), configName + " contains an invalid class name");
        }
    }
}
