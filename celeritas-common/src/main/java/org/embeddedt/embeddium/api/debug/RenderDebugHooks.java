package org.embeddedt.embeddium.api.debug;

public interface RenderDebugHooks {
    RenderDebugHooks NOOP = new RenderDebugHooks() {
    };

    default boolean shouldCaptureGlState() {
        return false;
    }

    default void checkDrawError(String stage, String source, int drawMode, int vertexFlags, int stride, int vertexCount, String format, int vao, int vbo) {
    }

    default long beginRenderGlobalStageTiming() {
        return 0L;
    }

    default void recordRenderGlobalStageTiming(String stage, int pass, long startNanos) {
    }

    default void recordRenderGlobalCounterTiming(String stage, int pass, long nanos, int checked, int visible, int rendered, int outlined, int multipass) {
    }

    default void logWorldPassState(String stage, String phase, String subject) {
    }

    default void logShadowTerrainLayer(String stage, String passName, int visibleChunks) {
    }

    default void logCurrentFramebufferSamples(String label, int localColorAttachments) {
    }

    default void check(String stage) {
    }

    default void logShadowPassState(String stage, boolean terrain, boolean translucent, boolean entities, boolean player, boolean blockEntities, int visibleChunks, int renderedEntities, int renderedBlockEntities) {
    }

    default void logTerrainMaterialSample(String source, int blockId, int renderType, int lightValue, int localX, int localY, int localZ, float u, float v) {
    }

    default boolean shouldCaptureGpuPerfTiming() {
        return false;
    }

    default void recordTerrainRendererTiming(String passName, long totalNanos, long fillNanos, long tessellationNanos, long uniformsNanos, long drawNanos, int regions, int batches, int commands) {
    }
}
