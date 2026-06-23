package com.dhj.actinium.render;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.debug.GLSMDebug;
import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.embeddedt.embeddium.api.debug.RenderDebugHooksHolder;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class VanillaBufferBuilderRenderer {
    private static final Map<VertexFormat, Integer> VAOS = new HashMap<>();
    private static final Map<VertexFormat, Integer> VBOS = new HashMap<>();
    private static final Map<VertexFormat, Integer> VERTEX_FLAGS = new HashMap<>();

    private VanillaBufferBuilderRenderer() {
    }

    public static void draw(BufferBuilder bufferBuilder, String debugSource) {
        if (BufferBuilderStreamingDrawer.isEnabled()) {
            BufferBuilderStreamingDrawer.draw(bufferBuilder, debugSource);
            return;
        }

        if (bufferBuilder.getVertexCount() <= 0) {
            bufferBuilder.reset();
            return;
        }

        VertexFormat format = bufferBuilder.getVertexFormat();
        int vertexCount = bufferBuilder.getVertexCount();
        int drawMode = bufferBuilder.getDrawMode();
        int stride = format.getSize();
        int byteCount = vertexCount * stride;
        ByteBuffer buffer = bufferBuilder.getByteBuffer().duplicate();
        buffer.position(0);
        buffer.limit(byteCount);

        drawRaw(buffer, format, vertexCount, drawMode, debugSource);
        bufferBuilder.reset();
    }

    public static void drawRaw(ByteBuffer buffer, VertexFormat format, int vertexCount, int drawMode, String debugSource) {
        if (BufferBuilderStreamingDrawer.isEnabled()) {
            BufferBuilderStreamingDrawer.drawRaw(buffer, format, vertexCount, drawMode, debugSource);
            return;
        }

        if (vertexCount <= 0) {
            return;
        }

        ensureDrawState(format);
        int vao = VAOS.get(format);
        int vbo = VBOS.get(format);
        int vertexFlags = VERTEX_FLAGS.get(format);
        int stride = format.getSize();
        int byteCount = vertexCount * stride;
        ByteBuffer upload = buffer.duplicate();
        upload.position(0);
        upload.limit(byteCount);

        int savedVbo = GLStateManager.getBoundVBO();
        GLStateManager.glBindVertexArray(vao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GLStateManager.glBufferData(GL15.GL_ARRAY_BUFFER, upload, GL15.GL_STREAM_DRAW);

        boolean logDrawDiagnostics = GLSMDebug.shouldLogDrawDiagnostics();
        boolean checkDrawErrors = RenderDebugHooksHolder.shouldCaptureGlState();
        String formatDescription = logDrawDiagnostics || checkDrawErrors ? format.toString() : null;
        if (logDrawDiagnostics) {
            GLSMDebug.logBufferBuilderUpload(formatDescription, drawMode, vertexFlags, stride, vertexCount, byteCount, vao, vbo);
        }

        GLStateManager.prepareWideLineEmulation(drawMode);
        ShaderManager.getInstance().preDraw(vertexFlags);
        if (checkDrawErrors) {
            RenderDebugHooksHolder.checkDrawError("bufferbuilder:after-predraw", debugSource, drawMode, vertexFlags, stride, vertexCount, formatDescription, vao, vbo);
        }
        VanillaVertexBufferRenderer.drawArrays(drawMode, vertexCount);
        if (checkDrawErrors) {
            RenderDebugHooksHolder.checkDrawError("bufferbuilder:after-draw", debugSource, drawMode, vertexFlags, stride, vertexCount, formatDescription, vao, vbo);
        }

        GLStateManager.glBindVertexArray(0);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        if (checkDrawErrors) {
            RenderDebugHooksHolder.checkDrawError("bufferbuilder:after-restore", debugSource, drawMode, vertexFlags, stride, vertexCount, formatDescription, vao, vbo);
        }
    }

    private static void ensureDrawState(VertexFormat format) {
        if (!VAOS.containsKey(format)) {
            int vbo = GLStateManager.glGenBuffers();
            int vertexFlags = VanillaVertexBufferRenderer.vertexFlags(format);
            int vao = VanillaVertexBufferRenderer.createStreamingVertexArray(format, vbo);
            VAOS.put(format, vao);
            VBOS.put(format, vbo);
            VERTEX_FLAGS.put(format, vertexFlags);
        }
    }
}

