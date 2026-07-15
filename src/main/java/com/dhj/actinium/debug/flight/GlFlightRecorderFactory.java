package com.dhj.actinium.debug.flight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

final class GlFlightRecorderFactory {
    private static final int DEFAULT_SLOT_COUNT = 16_384;
    private static final Path DEFAULT_DIAGNOSTICS_DIRECTORY = Path.of("diagnostics");

    private GlFlightRecorderFactory() {
    }

    static GlFlightRecorder openConfigured() throws IOException {
        GlFlightRecorderMode mode = GlFlightRecorderMode.parse(
            System.getProperty(GlFlightRecorder.MODE_PROPERTY, "off").trim().toLowerCase(Locale.ROOT)
        );
        if (mode == GlFlightRecorderMode.OFF) {
            return DisabledGlFlightRecorder.INSTANCE;
        }

        Files.createDirectories(DEFAULT_DIAGNOSTICS_DIRECTORY);
        long timestamp = Clock.systemUTC().millis();
        long processId = ProcessHandle.current().pid();
        Path path = DEFAULT_DIAGNOSTICS_DIRECTORY.resolve(
            "gl-flight-" + timestamp + "-" + processId + ".bin"
        );
        return open(path, mode, DEFAULT_SLOT_COUNT);
    }

    static GlFlightRecorder open(Path path, GlFlightRecorderMode mode, int slotCount) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(mode, "mode");
        if (mode == GlFlightRecorderMode.OFF) {
            return DisabledGlFlightRecorder.INSTANCE;
        }

        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Recorder path has no parent: " + path);
        }
        Files.createDirectories(parent);
        return new MappedGlFlightRecorder(absolutePath, mode, slotCount);
    }
}
