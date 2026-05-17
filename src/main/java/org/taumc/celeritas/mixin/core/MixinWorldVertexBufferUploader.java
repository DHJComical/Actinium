package org.taumc.celeritas.mixin.core;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.debug.GLSMDebug;
import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.taumc.celeritas.impl.render.VanillaVertexBufferRenderer;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Mixin(WorldVertexBufferUploader.class)
public class MixinWorldVertexBufferUploader {
    @Unique
    private static final Map<VertexFormat, Integer> celeritas$vaos = new HashMap<>();
    @Unique
    private static final Map<VertexFormat, Integer> celeritas$vbos = new HashMap<>();
    @Unique
    private static final Map<VertexFormat, Integer> celeritas$vertexFlagsByFormat = new HashMap<>();

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDraw(BufferBuilder bufferBuilder, CallbackInfo ci) {
        if (bufferBuilder.getVertexCount() <= 0) {
            bufferBuilder.reset();
            ci.cancel();
            return;
        }

        VertexFormat format = bufferBuilder.getVertexFormat();
        celeritas$ensureDrawState(format);
        int vao = celeritas$vaos.get(format);
        int vbo = celeritas$vbos.get(format);
        int vertexFlags = celeritas$vertexFlagsByFormat.get(format);
        int byteCount = bufferBuilder.getVertexCount() * bufferBuilder.getVertexFormat().getSize();
        ByteBuffer buffer = bufferBuilder.getByteBuffer().duplicate();
        buffer.position(0);
        buffer.limit(byteCount);

        int savedVbo = GLStateManager.getBoundVBO();
        GLStateManager.glBindVertexArray(vao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GLStateManager.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STREAM_DRAW);
        GLSMDebug.logBufferBuilderUpload(
                bufferBuilder.getVertexFormat().toString(),
                bufferBuilder.getDrawMode(),
                vertexFlags,
                bufferBuilder.getVertexFormat().getSize(),
                bufferBuilder.getVertexCount(),
                byteCount,
                vao,
                vbo);

        GLStateManager.prepareWideLineEmulation(bufferBuilder.getDrawMode());
        ShaderManager.getInstance().preDraw(vertexFlags);
        IrisGlDebug.checkDrawError(
                "bufferbuilder:after-predraw",
                "WorldVertexBufferUploader",
                bufferBuilder.getDrawMode(),
                vertexFlags,
                bufferBuilder.getVertexFormat().getSize(),
                bufferBuilder.getVertexCount(),
                bufferBuilder.getVertexFormat().toString(),
                vao,
                vbo);
        VanillaVertexBufferRenderer.drawArrays(bufferBuilder.getDrawMode(), bufferBuilder.getVertexCount());
        IrisGlDebug.checkDrawError(
                "bufferbuilder:after-draw",
                "WorldVertexBufferUploader",
                bufferBuilder.getDrawMode(),
                vertexFlags,
                bufferBuilder.getVertexFormat().getSize(),
                bufferBuilder.getVertexCount(),
                bufferBuilder.getVertexFormat().toString(),
                vao,
                vbo);

        GLStateManager.glBindVertexArray(0);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        IrisGlDebug.checkDrawError(
                "bufferbuilder:after-restore",
                "WorldVertexBufferUploader",
                bufferBuilder.getDrawMode(),
                vertexFlags,
                bufferBuilder.getVertexFormat().getSize(),
                bufferBuilder.getVertexCount(),
                bufferBuilder.getVertexFormat().toString(),
                vao,
                vbo);
        bufferBuilder.reset();
        ci.cancel();
    }

    @Unique
    private static void celeritas$ensureDrawState(VertexFormat format) {
        if (!celeritas$vaos.containsKey(format)) {
            celeritas$createDrawState(format);
        }
    }

    @Unique
    private static void celeritas$createDrawState(VertexFormat format) {
        int vbo = GLStateManager.glGenBuffers();
        int vertexFlags = VanillaVertexBufferRenderer.vertexFlags(format);
        int vao = VanillaVertexBufferRenderer.createStreamingVertexArray(format, vbo);
        celeritas$vaos.put(format, vao);
        celeritas$vbos.put(format, vbo);
        celeritas$vertexFlagsByFormat.put(format, vertexFlags);
    }
}
