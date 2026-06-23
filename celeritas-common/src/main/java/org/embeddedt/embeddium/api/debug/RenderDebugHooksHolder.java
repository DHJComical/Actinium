package org.embeddedt.embeddium.api.debug;

import org.jetbrains.annotations.Nullable;

public final class RenderDebugHooksHolder {
    private static RenderDebugHooks hooks = RenderDebugHooks.NOOP;

    private RenderDebugHooksHolder() {
    }

    public static void setHooks(@Nullable RenderDebugHooks hooks) {
        RenderDebugHooksHolder.hooks = hooks != null ? hooks : RenderDebugHooks.NOOP;
    }

    public static boolean shouldCaptureGlState() {
        return hooks.shouldCaptureGlState();
    }

    public static void checkDrawError(String stage, String source, int drawMode, int vertexFlags, int stride, int vertexCount, String format, int vao, int vbo) {
        hooks.checkDrawError(stage, source, drawMode, vertexFlags, stride, vertexCount, format, vao, vbo);
    }

    public static long beginRenderGlobalStageTiming() {
        return hooks.beginRenderGlobalStageTiming();
    }

    public static void recordRenderGlobalStageTiming(String stage, int pass, long startNanos) {
        hooks.recordRenderGlobalStageTiming(stage, pass, startNanos);
    }

    public static void recordRenderGlobalCounterTiming(String stage, int pass, long nanos, int checked, int visible, int rendered, int outlined, int multipass) {
        hooks.recordRenderGlobalCounterTiming(stage, pass, nanos, checked, visible, rendered, outlined, multipass);
    }

    public static void logWorldPassState(String stage, String phase, String subject) {
        hooks.logWorldPassState(stage, phase, subject);
    }

    public static void logShadowTerrainLayer(String stage, String passName, int visibleChunks) {
        hooks.logShadowTerrainLayer(stage, passName, visibleChunks);
    }

    public static void logCurrentFramebufferSamples(String label, int localColorAttachments) {
        hooks.logCurrentFramebufferSamples(label, localColorAttachments);
    }

    public static void check(String stage) {
        hooks.check(stage);
    }

    public static void logShadowPassState(String stage, boolean terrain, boolean translucent, boolean entities, boolean player, boolean blockEntities, int visibleChunks, int renderedEntities, int renderedBlockEntities) {
        hooks.logShadowPassState(stage, terrain, translucent, entities, player, blockEntities, visibleChunks, renderedEntities, renderedBlockEntities);
    }

    public static void logTerrainMaterialSample(String source, int blockId, int renderType, int lightValue, int localX, int localY, int localZ, float u, float v) {
        hooks.logTerrainMaterialSample(source, blockId, renderType, lightValue, localX, localY, localZ, u, v);
    }

    public static boolean shouldCaptureGpuPerfTiming() {
        return hooks.shouldCaptureGpuPerfTiming();
    }

    public static void recordTerrainRendererTiming(String passName, long totalNanos, long fillNanos, long tessellationNanos, long uniformsNanos, long drawNanos, int regions, int batches, int commands) {
        hooks.recordTerrainRendererTiming(passName, totalNanos, fillNanos, tessellationNanos, uniformsNanos, drawNanos, regions, batches, commands);
    }
}
