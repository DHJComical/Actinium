package org.embeddedt.embeddium.impl.model.light.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.api.debug.RenderDebugHooksHolder;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AODebug {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumAODebug");
    private static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();
    private static final boolean OVERRIDE_ENABLED = Boolean.getBoolean("actinium.aoDebug");

    private AODebug() {
    }

    public static boolean isEnabled() {
        return OVERRIDE_ENABLED || RenderDebugHooksHolder.shouldCaptureGlState();
    }

    private static boolean shouldLog(String label, int maxCount) {
        if (!isEnabled()) {
            return false;
        }

        int count = COUNTS.merge(label, 1, Integer::sum);
        return count <= maxCount;
    }

    public static void logSettings(String stage, float aoLevel, boolean separateAo) {
        if (!isEnabled()) {
            return;
        }

        String label = "settings:" + stage + ":" + aoLevel + ":" + separateAo;
        if (shouldLog(label, 2)) {
            LOGGER.info(
                "ao-settings stage={} aoLevel={} separateAo={} count={}",
                stage,
                aoLevel,
                separateAo,
                COUNTS.get(label)
            );
        }
    }

    public static void logRenderDecision(
        String stage,
        int blockId,
        int x,
        int y,
        int z,
        boolean vanillaAoEnabled,
        int blockLight,
        boolean modelAo,
        boolean smooth,
        float aoLevel,
        boolean separateAo
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = "decision:" + stage + ":" + blockId + ":" + vanillaAoEnabled + ":" + modelAo + ":" + smooth;
        if (shouldLog(label, 8)) {
            LOGGER.info(
                "ao-decision stage={} blockId={} pos=[{},{},{}] vanillaAoEnabled={} blockLight={} modelAo={} smooth={} aoLevel={} separateAo={} count={}",
                stage,
                blockId,
                x,
                y,
                z,
                vanillaAoEnabled,
                blockLight,
                modelAo,
                smooth,
                aoLevel,
                separateAo,
                COUNTS.get(label)
            );
        }
    }

    public static void logLightData(
        String stage,
        int blockId,
        int x,
        int y,
        int z,
        float vanillaAo,
        float aoLevel,
        float ao,
        int packed,
        int lightValue,
        boolean opaque,
        boolean fullCube,
        boolean fullOpaque
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = "light:" + stage + ":" + blockId + ":" + vanillaAo + ":" + aoLevel;
        if (shouldLog(label, 8)) {
            LOGGER.info(
                "ao-light stage={} blockId={} pos=[{},{},{}] vanillaAo={} aoLevel={} ao={} packed={} lightValue={} opaque={} fullCube={} fullOpaque={} count={}",
                stage,
                blockId,
                x,
                y,
                z,
                vanillaAo,
                aoLevel,
                ao,
                packed,
                lightValue,
                opaque,
                fullCube,
                fullOpaque,
                COUNTS.get(label)
            );
        }
    }

    public static void logFaceData(
        String stage,
        int x,
        int y,
        int z,
        String direction,
        boolean offset,
        float[] corners,
        float[] result
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = "face:" + stage + ":" + direction + ":" + offset;
        if (shouldLog(label, 6)) {
            LOGGER.info(
                "ao-face stage={} pos=[{},{},{}] direction={} offset={} corners={} result={} count={}",
                stage,
                x,
                y,
                z,
                direction,
                offset,
                Arrays.toString(corners),
                Arrays.toString(result),
                COUNTS.get(label)
            );
        }
    }

    public static void logVertexBrightness(
        String stage,
        int x,
        int y,
        int z,
        String face,
        boolean shade,
        boolean depthBlend,
        float[] brightness
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = "brightness:" + stage + ":" + face + ":" + shade + ":" + depthBlend;
        if (shouldLog(label, 6)) {
            LOGGER.info(
                "ao-brightness stage={} pos=[{},{},{}] face={} shade={} depthBlend={} brightness={} count={}",
                stage,
                x,
                y,
                z,
                face,
                shade,
                depthBlend,
                Arrays.toString(brightness),
                COUNTS.get(label)
            );
        }
    }

    public static void logVertexColor(
        String stage,
        int blockId,
        int x,
        int y,
        int z,
        int vertex,
        float ao,
        int colorBefore,
        int colorAfter,
        boolean separateAo
    ) {
        if (!isEnabled()) {
            return;
        }

        String label = "color:" + stage + ":" + blockId + ":" + separateAo + ":" + vertex;
        if (shouldLog(label, 8)) {
            LOGGER.info(
                "ao-color stage={} blockId={} pos=[{},{},{}] vertex={} ao={} colorBefore=0x{} colorAfter=0x{} separateAo={} count={}",
                stage,
                blockId,
                x,
                y,
                z,
                vertex,
                ao,
                Integer.toHexString(colorBefore),
                Integer.toHexString(colorAfter),
                separateAo,
                COUNTS.get(label)
            );
        }
    }
}
