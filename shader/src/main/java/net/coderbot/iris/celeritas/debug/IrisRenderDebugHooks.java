package net.coderbot.iris.celeritas.debug;

import net.coderbot.iris.debug.IrisGlDebug;
import org.embeddedt.embeddium.api.debug.RenderDebugHooks;

public final class IrisRenderDebugHooks implements RenderDebugHooks {
    public static final IrisRenderDebugHooks INSTANCE = new IrisRenderDebugHooks();

    private IrisRenderDebugHooks() {
    }

    @Override
    public boolean shouldCaptureGlState() {
        return IrisGlDebug.shouldCaptureGlState();
    }

    @Override
    public void checkDrawError(String stage, String source, int drawMode, int vertexFlags, int stride, int vertexCount, String format, int vao, int vbo) {
        IrisGlDebug.checkDrawError(stage, source, drawMode, vertexFlags, stride, vertexCount, format, vao, vbo);
    }

    @Override
    public long beginRenderGlobalStageTiming() {
        return IrisGlDebug.beginRenderGlobalStageTiming();
    }

    @Override
    public void recordRenderGlobalStageTiming(String stage, int pass, long startNanos) {
        IrisGlDebug.recordRenderGlobalStageTiming(stage, pass, startNanos);
    }

    @Override
    public void recordRenderGlobalCounterTiming(String stage, int pass, long nanos, int checked, int visible, int rendered, int outlined, int multipass) {
        IrisGlDebug.recordRenderGlobalCounterTiming(stage, pass, nanos, checked, visible, rendered, outlined, multipass);
    }

    @Override
    public void logWorldPassState(String stage, String phase, String subject) {
        IrisGlDebug.logWorldPassState(stage, phase, subject);
    }

    @Override
    public void logShadowTerrainLayer(String stage, String passName, int visibleChunks) {
        IrisGlDebug.logShadowTerrainLayer(stage, passName, visibleChunks);
    }

    @Override
    public void logCurrentFramebufferSamples(String label, int localColorAttachments) {
        IrisGlDebug.logCurrentFramebufferSamples(label, localColorAttachments);
    }

    @Override
    public void check(String stage) {
        IrisGlDebug.check(stage);
    }

    @Override
    public void logShadowPassState(String stage, boolean terrain, boolean translucent, boolean entities, boolean player, boolean blockEntities, int visibleChunks, int renderedEntities, int renderedBlockEntities) {
        IrisGlDebug.logShadowPassState(stage, terrain, translucent, entities, player, blockEntities, visibleChunks, renderedEntities, renderedBlockEntities);
    }

    @Override
    public void logTerrainMaterialSample(String source, int blockId, int renderType, int lightValue, int localX, int localY, int localZ, float u, float v) {
        IrisGlDebug.logTerrainMaterialSample(source, blockId, renderType, lightValue, localX, localY, localZ, u, v);
    }

    @Override
    public boolean shouldCaptureGpuPerfTiming() {
        return IrisGlDebug.shouldCaptureGpuPerfTiming();
    }

    @Override
    public void recordTerrainRendererTiming(String passName, long totalNanos, long fillNanos, long tessellationNanos, long uniformsNanos, long drawNanos, int regions, int batches, int commands) {
        IrisGlDebug.recordTerrainRendererTiming(passName, totalNanos, fillNanos, tessellationNanos, uniformsNanos, drawNanos, regions, batches, commands);
    }
}
