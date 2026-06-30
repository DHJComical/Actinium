package com.gtnewhorizons.angelica.glsm.redirect;

public final class RedirectorDebugOptions {
    private static final java.nio.file.Path CONFIG_PATH = java.nio.file.Paths.get("config", "actinium-options.json");
    private static final java.nio.file.Path LEGACY_CONFIG_PATH = java.nio.file.Paths.get("config", "embeddium-options.json");
    private static final Snapshot SNAPSHOT = loadSnapshot();

    private RedirectorDebugOptions() {
    }

    public static boolean enableLogSpam() {
        return getBooleanOverride("angelica.redirectorLogspam", SNAPSHOT.redirectorLogSpam);
    }

    public static boolean enableDebug() {
        return getBooleanOverride("actinium.redirectorDebug", SNAPSHOT.redirectorDebug);
    }

    public static boolean enableClassDump() {
        return getBooleanOverride("angelica.dumpClass", SNAPSHOT.classDump);
    }

    private static boolean getBooleanOverride(String key, boolean fallback) {
        String override = System.getProperty(key);
        return override != null ? Boolean.parseBoolean(override) : fallback;
    }

    private static Snapshot loadSnapshot() {
        java.nio.file.Path path = java.nio.file.Files.isRegularFile(CONFIG_PATH) ? CONFIG_PATH : LEGACY_CONFIG_PATH;
        if (!java.nio.file.Files.isRegularFile(path)) {
            return Snapshot.DEFAULT;
        }

        try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(path)) {
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return Snapshot.DEFAULT;
            }

            com.google.gson.JsonObject debug = root.getAsJsonObject().getAsJsonObject("debug");
            if (debug == null) {
                return Snapshot.DEFAULT;
            }

            return new Snapshot(
                getBoolean(debug, "enable_redirector_debug"),
                getBoolean(debug, "enable_redirector_log_spam"),
                getBoolean(debug, "enable_redirector_class_dump")
            );
        } catch (java.io.IOException | RuntimeException ignored) {
            return Snapshot.DEFAULT;
        }
    }

    private static boolean getBoolean(com.google.gson.JsonObject object, String key) {
        com.google.gson.JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }

    private record Snapshot(boolean redirectorDebug, boolean redirectorLogSpam, boolean classDump) {
        private static final Snapshot DEFAULT = new Snapshot(false, false, false);
    }
}
