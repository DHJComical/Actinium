package org.taumc.celeritas.mixin.core;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.QuadConverter;
import com.gtnewhorizons.angelica.glsm.debug.GLSMDebug;
import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;
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
        celeritas$drawArrays(bufferBuilder.getDrawMode(), bufferBuilder.getVertexCount());

        GLStateManager.glBindVertexArray(0);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
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
        int vao = GLStateManager.glGenVertexArrays();
        int vbo = GLStateManager.glGenBuffers();
        int vertexFlags = celeritas$vertexFlags(format);

        GLStateManager.glBindVertexArray(vao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        int stride = format.getSize();
        for (int i = 0; i < format.getElementCount(); i++) {
            VertexFormatElement element = format.getElement(i);
            int location = celeritas$attributeLocation(element);
            if (location < 0) {
                continue;
            }

            GLStateManager.glEnableVertexAttribArray(location);
            GLStateManager.glVertexAttribPointer(
                    location,
                    element.getElementCount(),
                    element.getType().getGlConstant(),
                    celeritas$isNormalized(element),
                    stride,
                    format.getOffset(i));
        }

        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GLStateManager.glBindVertexArray(0);
        celeritas$vaos.put(format, vao);
        celeritas$vbos.put(format, vbo);
        celeritas$vertexFlagsByFormat.put(format, vertexFlags);
    }

    @Unique
    private static int celeritas$attributeLocation(VertexFormatElement element) {
        return switch (element.getUsage()) {
            case POSITION -> 0;
            case COLOR -> 1;
            case UV -> element.getIndex() == 0 ? 2 : element.getIndex() == 1 ? 3 : -1;
            case NORMAL -> 4;
            case GENERIC -> element.getIndex();
            default -> -1;
        };
    }

    @Unique
    private static boolean celeritas$isNormalized(VertexFormatElement element) {
        return element.getUsage() == VertexFormatElement.EnumUsage.COLOR
                || element.getUsage() == VertexFormatElement.EnumUsage.NORMAL;
    }

    @Unique
    private static int celeritas$vertexFlags(VertexFormat format) {
        boolean hasTexture = false;
        boolean hasColor = false;
        boolean hasNormal = false;
        boolean hasBrightness = false;

        for (VertexFormatElement element : format.getElements()) {
            switch (element.getUsage()) {
                case COLOR -> hasColor = true;
                case NORMAL -> hasNormal = true;
                case UV -> {
                    if (element.getIndex() == 0) {
                        hasTexture = true;
                    } else if (element.getIndex() == 1) {
                        hasBrightness = true;
                    }
                }
                default -> {
                }
            }
        }

        return VertexFlags.convertToFlags(hasTexture, hasColor, hasNormal, hasBrightness);
    }

    @Unique
    private static void celeritas$drawArrays(int drawMode, int vertexCount) {
        if (drawMode == GL11.GL_QUADS) {
            QuadConverter.drawQuadsAsTriangles(0, vertexCount);
        } else if (drawMode == GL11.GL_QUAD_STRIP) {
            GLStateManager.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, vertexCount & ~1);
        } else if (drawMode == GL11.GL_POLYGON) {
            GLStateManager.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, vertexCount);
        } else {
            GLStateManager.glDrawArrays(drawMode, 0, vertexCount);
        }
    }
}
