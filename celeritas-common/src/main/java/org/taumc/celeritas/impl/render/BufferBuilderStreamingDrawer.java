package org.taumc.celeritas.impl.render;

import com.dhj.actinium.config.ActiniumRuntimeOptions;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.debug.GLSMDebug;
import com.gtnewhorizons.angelica.glsm.debug.GLSMPerfDebug;
import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import com.gtnewhorizons.angelica.glsm.streaming.OrphanStreamingBuffer;
import com.gtnewhorizons.angelica.glsm.streaming.PersistentStreamingBuffer;
import com.gtnewhorizons.angelica.glsm.streaming.StreamingOptions;
import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class BufferBuilderStreamingDrawer {
    private static final Logger LOGGER = LogManager.getLogger("BufferBuilderStreamingDrawer");
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("actinium.bufferBuilderStreaming", "true"));

    private static final Map<VertexFormat, DrawState> DRAW_STATES = new HashMap<>();
    private static PersistentStreamingBuffer persistentBuffer;
    private static boolean initialized;

    private BufferBuilderStreamingDrawer() {
    }

    public static boolean isEnabled() {
        return ENABLED && ActiniumRuntimeOptions.allowDirectMemoryAccess();
    }

    public static void draw(BufferBuilder bufferBuilder, String debugSource) {
        final long perfStart = GLSMPerfDebug.ENABLED ? GLSMPerfDebug.begin(GLSMPerfDebug.Stage.BUFFERBUILDER_STREAM_DRAW) : 0L;
        if (bufferBuilder.getVertexCount() <= 0) {
            bufferBuilder.reset();
            if (GLSMPerfDebug.ENABLED) {
                GLSMPerfDebug.end(GLSMPerfDebug.Stage.BUFFERBUILDER_STREAM_DRAW, perfStart);
            }
            return;
        }

        init();

        VertexFormat format = bufferBuilder.getVertexFormat();
        int vertexCount = bufferBuilder.getVertexCount();
        int drawMode = bufferBuilder.getDrawMode();
        int stride = format.getSize();
        int byteCount = vertexCount * stride;
        GLSMPerfDebug.countBufferBuilder(debugSource, drawMode, vertexCount);
        ByteBuffer buffer = bufferBuilder.getByteBuffer().duplicate();
        buffer.position(0);
        buffer.limit(byteCount);

        drawRaw(buffer, format, vertexCount, drawMode, debugSource);
        bufferBuilder.reset();
        if (GLSMPerfDebug.ENABLED) {
            GLSMPerfDebug.end(GLSMPerfDebug.Stage.BUFFERBUILDER_STREAM_DRAW, perfStart);
        }
    }

    public static void drawRaw(ByteBuffer buffer, VertexFormat format, int vertexCount, int drawMode, String debugSource) {
        if (vertexCount <= 0) {
            return;
        }

        init();

        DrawState state = ensureDrawState(format);
        int stride = format.getSize();
        int byteCount = vertexCount * stride;
        ByteBuffer upload = buffer.duplicate();
        upload.position(0);
        upload.limit(byteCount);

        int savedVao = GLStateManager.getBoundVAO();
        int savedVbo = GLStateManager.getBoundVBO();
        int firstVertex = -1;
        boolean logDrawDiagnostics = GLSMDebug.shouldLogDrawDiagnostics();
        boolean checkDrawErrors = IrisGlDebug.shouldCaptureGlState();
        String formatDescription = logDrawDiagnostics || checkDrawErrors ? format.toString() : null;

        try {
            if (persistentBuffer != null) {
                final long uploadStart = GLSMPerfDebug.ENABLED ? GLSMPerfDebug.now() : 0L;
                firstVertex = persistentBuffer.upload(upload, stride);
                if (firstVertex >= 0) {
                    GLSMPerfDebug.record(GLSMPerfDebug.Stage.BUFFERBUILDER_PERSISTENT_UPLOAD, uploadStart, GLSMPerfDebug.now());
                }
            }

            final int vao;
            final int vbo;
            if (firstVertex >= 0) {
                vao = state.persistentVao;
                vbo = persistentBuffer.getBufferId();
                GLStateManager.glBindVertexArray(vao);
                GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            } else {
                vao = state.orphanVao;
                vbo = state.orphanBuffer.getBufferId();
                GLStateManager.glBindVertexArray(vao);
                GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                final long uploadStart = GLSMPerfDebug.ENABLED ? GLSMPerfDebug.now() : 0L;
                state.orphanBuffer.upload(upload);
                GLSMPerfDebug.record(GLSMPerfDebug.Stage.BUFFERBUILDER_ORPHAN_UPLOAD, uploadStart, GLSMPerfDebug.now());
                firstVertex = 0;
            }

            if (logDrawDiagnostics) {
                GLSMDebug.logBufferBuilderUpload(formatDescription, drawMode, state.vertexFlags, stride, vertexCount, byteCount, vao, vbo);
            }

            GLStateManager.prepareWideLineEmulation(drawMode);
            ShaderManager.getInstance().preDraw(state.vertexFlags);
            if (checkDrawErrors) {
                IrisGlDebug.checkDrawError("bufferbuilder-stream:after-predraw", debugSource, drawMode, state.vertexFlags, stride, vertexCount, formatDescription, vao, vbo);
            }
            final long drawStart = GLSMPerfDebug.ENABLED ? GLSMPerfDebug.now() : 0L;
            VanillaVertexBufferRenderer.drawArrays(drawMode, firstVertex, vertexCount);
            GLSMPerfDebug.record(GLSMPerfDebug.Stage.BUFFERBUILDER_DRAW_CALL, drawStart, GLSMPerfDebug.now());
            if (checkDrawErrors) {
                IrisGlDebug.checkDrawError("bufferbuilder-stream:after-draw", debugSource, drawMode, state.vertexFlags, stride, vertexCount, formatDescription, vao, vbo);
            }
        } finally {
            GLStateManager.glBindVertexArray(savedVao);
            GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
            if (checkDrawErrors) {
                IrisGlDebug.checkDrawError("bufferbuilder-stream:after-restore", debugSource, drawMode, state.vertexFlags, stride, vertexCount, formatDescription, savedVao, savedVbo);
            }
        }
    }

    public static void endFrame() {
        if (persistentBuffer != null) {
            persistentBuffer.postDraw();
        }
    }

    public static void destroy() {
        for (DrawState state : DRAW_STATES.values()) {
            if (state.persistentVao != 0) {
                GLStateManager.glDeleteVertexArrays(state.persistentVao);
            }
            if (state.orphanVao != 0) {
                GLStateManager.glDeleteVertexArrays(state.orphanVao);
            }
            if (state.orphanBuffer != null) {
                state.orphanBuffer.destroy();
            }
        }
        DRAW_STATES.clear();

        if (persistentBuffer != null) {
            persistentBuffer.destroy();
            persistentBuffer = null;
        }
        initialized = false;
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (StreamingOptions.usePersistentStreaming() && RenderSystem.supportsBufferStorage()) {
            try {
                persistentBuffer = new PersistentStreamingBuffer();
                LOGGER.info("Persistent BufferBuilder streaming buffer created ({}MB)", PersistentStreamingBuffer.DEFAULT_CAPACITY / (1024 * 1024));
            } catch (Exception e) {
                LOGGER.warn("Failed to create persistent BufferBuilder streaming buffer, using orphan fallback", e);
                persistentBuffer = null;
            }
        } else {
            LOGGER.info("Persistent BufferBuilder streaming disabled; using orphan streaming");
        }
    }

    private static DrawState ensureDrawState(VertexFormat format) {
        DrawState state = DRAW_STATES.get(format);
        if (state != null) {
            return state;
        }

        state = new DrawState();
        state.vertexFlags = VanillaVertexBufferRenderer.vertexFlags(format);
        state.orphanBuffer = new OrphanStreamingBuffer();
        state.orphanVao = VanillaVertexBufferRenderer.createStreamingVertexArray(format, state.orphanBuffer.getBufferId());

        if (persistentBuffer != null) {
            state.persistentVao = VanillaVertexBufferRenderer.createStreamingVertexArray(format, persistentBuffer.getBufferId());
        }

        DRAW_STATES.put(format, state);
        return state;
    }

    private static final class DrawState {
        private int vertexFlags;
        private int orphanVao;
        private int persistentVao;
        private OrphanStreamingBuffer orphanBuffer;
    }
}
