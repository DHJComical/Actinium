package com.gtnewhorizons.angelica.glsm.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GLSMPerfDebug {
    private static final Logger LOGGER = LogManager.getLogger("GLSMPerfDebug");
    private static final long REPORT_INTERVAL_NS = 1_000_000_000L;
    private static final int SAMPLE_MASK = 255;

    public static final boolean ENABLED = Boolean.getBoolean("actinium.glsmPerfDebug");

    public enum Stage {
        STREAM_DRAW("stream.draw"),
        STREAM_DRAW_DIRECT("stream.drawDirect"),
        STREAM_UPLOAD_AND_DRAW("stream.uploadAndDraw"),
        STREAM_PERSISTENT_UPLOAD("stream.persistentUpload"),
        STREAM_ORPHAN_UPLOAD("stream.orphanUpload"),
        STREAM_SHADER_PREDRAW("stream.shaderPreDraw"),
        STREAM_DRAW_CALL("stream.drawCall"),
        FFP_PREDRAW("ffp.preDraw"),
        FFP_UNIFORMS("ffp.uniforms"),
        GL_PREPARE_CLIENT_ARRAYS("gl.prepareClientArrays"),
        GL_CLIENT_ARRAY_UPLOAD("gl.clientArrayUpload"),
        QUAD_ARRAYS("quad.arrays"),
        QUAD_ELEMENTS("quad.elements"),
        QUAD_SCRATCH_UPLOAD_DRAW("quad.scratchUploadDraw");

        private final String label;

        Stage(String label) {
            this.label = label;
        }
    }

    public enum Source {
        STREAM_TESSELLATOR("source.streamTessellator"),
        DIRECT_LIVE_IMMEDIATE("source.directLiveImmediate"),
        DIRECT_COMPILE_EXECUTE("source.directCompileExecute"),
        DIRECT_EXTERNAL("source.directExternal");

        private final String label;

        Source(String label) {
            this.label = label;
        }
    }

    private static final long[] totalNanos = new long[Stage.values().length];
    private static final long[] maxNanos = new long[Stage.values().length];
    private static final int[] sampledCounts = new int[Stage.values().length];
    private static final int[] observedCounts = new int[Stage.values().length];
    private static final int[] sourceCounts = new int[Source.values().length];
    private static long lastReportNanos;

    private GLSMPerfDebug() {}

    public static long begin(Stage stage) {
        final int index = stage.ordinal();
        observedCounts[index]++;
        if ((observedCounts[index] & SAMPLE_MASK) != 0) {
            return 0L;
        }
        return System.nanoTime();
    }

    public static void end(Stage stage, long startNanos) {
        if (startNanos == 0L) return;
        final long endNanos = System.nanoTime();
        recordSample(stage, endNanos, endNanos - startNanos);
    }

    public static long now() {
        return System.nanoTime();
    }

    public static void count(Source source) {
        sourceCounts[source.ordinal()]++;
    }

    public static void record(Stage stage, long startNanos, long endNanos) {
        if (startNanos == 0L) return;
        observedCounts[stage.ordinal()]++;
        recordSample(stage, endNanos, endNanos - startNanos);
    }

    private static void recordSample(Stage stage, long now, long elapsed) {
        final int index = stage.ordinal();
        totalNanos[index] += elapsed;
        sampledCounts[index]++;
        if (elapsed > maxNanos[index]) {
            maxNanos[index] = elapsed;
        }
        if (lastReportNanos == 0L) {
            lastReportNanos = now;
        } else if (now - lastReportNanos >= REPORT_INTERVAL_NS) {
            report(now);
        }
    }

    private static void report(long now) {
        final StringBuilder sb = new StringBuilder(512);
        sb.append("GLSM perf:");
        boolean hasSamples = false;
        final Stage[] stages = Stage.values();
        for (int i = 0; i < stages.length; i++) {
            final int observed = observedCounts[i];
            final int sampled = sampledCounts[i];
            if (observed == 0) continue;
            hasSamples = true;
            sb.append(' ')
                .append(stages[i].label)
                .append("[count=").append(observed)
                .append(",samples=").append(sampled)
                .append(",avgUs=").append(sampled == 0 ? "n/a" : formatMicros(totalNanos[i] / sampled))
                .append(",maxUs=").append(formatMicros(maxNanos[i]))
                .append(",totalMs=").append(formatMillis(totalNanos[i]))
                .append(']');
            observedCounts[i] = 0;
            sampledCounts[i] = 0;
            totalNanos[i] = 0L;
            maxNanos[i] = 0L;
        }
        final Source[] sources = Source.values();
        for (int i = 0; i < sources.length; i++) {
            final int count = sourceCounts[i];
            if (count == 0) continue;
            hasSamples = true;
            sb.append(' ')
                .append(sources[i].label)
                .append("[count=").append(count).append(']');
            sourceCounts[i] = 0;
        }
        lastReportNanos = now;
        if (hasSamples) {
            LOGGER.info(sb.toString());
        }
    }

    private static String formatMicros(long nanos) {
        return String.format("%.2f", nanos / 1_000.0);
    }

    private static String formatMillis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }
}
