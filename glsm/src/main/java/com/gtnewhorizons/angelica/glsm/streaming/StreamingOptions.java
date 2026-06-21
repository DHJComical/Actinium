package com.gtnewhorizons.angelica.glsm.streaming;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public final class StreamingOptions {
    public static final String PERSISTENT_STREAMING_PROPERTY = "actinium.glsm.usePersistentStreaming";
    public static final String FINISH_BEFORE_PERSISTENT_STREAMING_DESTROY_PROPERTY = "actinium.glsm.finishBeforePersistentStreamingDestroy";
    public static final String STREAMING_UPLOAD_STRATEGY_PROPERTY = "actinium.glsm.streamingUploadStrategy";

    private static final Logger LOGGER = LogManager.getLogger("StreamingOptions");

    private StreamingOptions() {
    }

    public static boolean usePersistentStreaming() {
        return Boolean.parseBoolean(System.getProperty(PERSISTENT_STREAMING_PROPERTY, "false"))
            && !Boolean.getBoolean("angelica.forceOrphanStreaming")
            && !Boolean.getBoolean("actinium.glsm.forceOrphanStreaming");
    }

    public static boolean finishBeforePersistentStreamingDestroy() {
        return Boolean.parseBoolean(System.getProperty(FINISH_BEFORE_PERSISTENT_STREAMING_DESTROY_PROPERTY, "true"));
    }

    public static StreamingUploader.UploadStrategy resolveUploadStrategy(StreamingUploader.UploadStrategy fallback) {
        String override = System.getProperty(STREAMING_UPLOAD_STRATEGY_PROPERTY);
        if (override == null || override.trim().isEmpty()) {
            return fallback;
        }

        String normalized = override.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return StreamingUploader.UploadStrategy.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid GLSM streaming upload strategy '{}', using {}", override, fallback);
            return fallback;
        }
    }
}
