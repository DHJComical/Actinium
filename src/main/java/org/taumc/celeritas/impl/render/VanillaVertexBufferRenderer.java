package org.taumc.celeritas.impl.render;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.QuadConverter;
import com.gtnewhorizons.angelica.glsm.debug.GLSMDebug;
import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class VanillaVertexBufferRenderer {
    private static final Map<Integer, Integer> VAOS_BY_VBO = new HashMap<>();
    private static final Map<Integer, Integer> FLAGS_BY_VBO = new HashMap<>();

    private VanillaVertexBufferRenderer() {
    }

    public static void uploadVertexBuffer(VertexFormat format, int vbo, ByteBuffer data, int usage) {
        int savedVbo = GLStateManager.getBoundVBO();
        ByteBuffer upload = data.duplicate();
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GLStateManager.glBufferData(GL15.GL_ARRAY_BUFFER, upload, usage);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        ensureVertexArray(vbo, format);

        GLSMDebug.logVertexBufferUpload(format.toString(), vertexFlags(format), format.getSize(), upload.limit(), vbo, VAOS_BY_VBO.get(vbo));
    }

    public static void drawVertexBuffer(VertexFormat format, int vbo, int count, int mode) {
        if (count <= 0 || vbo < 0) {
            return;
        }

        ensureVertexArray(vbo, format);
        int vao = VAOS_BY_VBO.get(vbo);
        int flags = FLAGS_BY_VBO.get(vbo);
        int savedVao = GLStateManager.getBoundVAO();
        int savedVbo = GLStateManager.getBoundVBO();

        GLStateManager.glBindVertexArray(vao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GLStateManager.prepareWideLineEmulation(mode);
        ShaderManager.getInstance().preDraw(flags);
        IrisGlDebug.checkDrawError("vertexbuffer:after-predraw", "VertexBuffer", mode, flags, format.getSize(), count, format.toString(), vao, vbo);
        GLSMDebug.logVertexBufferDraw(format.toString(), mode, flags, format.getSize(), count, vbo, vao);
        drawArrays(mode, count);
        IrisGlDebug.checkDrawError("vertexbuffer:after-draw", "VertexBuffer", mode, flags, format.getSize(), count, format.toString(), vao, vbo);

        GLStateManager.glBindVertexArray(savedVao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        IrisGlDebug.checkDrawError("vertexbuffer:after-restore", "VertexBuffer", mode, flags, format.getSize(), count, format.toString(), vao, vbo);
    }

    public static void deleteVertexBuffer(int vbo) {
        Integer vao = VAOS_BY_VBO.remove(vbo);
        FLAGS_BY_VBO.remove(vbo);
        if (vao != null) {
            GLStateManager.glDeleteVertexArrays(vao);
        }
        GLStateManager.glDeleteBuffers(vbo);
    }

    public static int createStreamingVertexArray(VertexFormat format, int vbo) {
        int savedVao = GLStateManager.getBoundVAO();
        int savedVbo = GLStateManager.getBoundVBO();
        int vao = GLStateManager.glGenVertexArrays();
        GLStateManager.glBindVertexArray(vao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        setupVertexFormatAttributes(format);
        GLStateManager.glBindVertexArray(savedVao);
        GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        return vao;
    }

    public static void drawArrays(int drawMode, int vertexCount) {
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

    public static int vertexFlags(VertexFormat format) {
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

    private static void ensureVertexArray(int vbo, VertexFormat format) {
        if (VAOS_BY_VBO.containsKey(vbo)) {
            return;
        }

        int vao = createStreamingVertexArray(format, vbo);
        VAOS_BY_VBO.put(vbo, vao);
        FLAGS_BY_VBO.put(vbo, vertexFlags(format));
    }

    private static void setupVertexFormatAttributes(VertexFormat format) {
        int stride = format.getSize();
        for (int i = 0; i < format.getElementCount(); i++) {
            VertexFormatElement element = format.getElement(i);
            int location = attributeLocation(element);
            if (location < 0) {
                continue;
            }

            GLStateManager.glEnableVertexAttribArray(location);
            GLStateManager.glVertexAttribPointer(
                    location,
                    element.getElementCount(),
                    element.getType().getGlConstant(),
                    isNormalized(element),
                    stride,
                    format.getOffset(i));
        }
    }

    private static int attributeLocation(VertexFormatElement element) {
        return switch (element.getUsage()) {
            case POSITION -> 0;
            case COLOR -> 1;
            case UV -> element.getIndex() == 0 ? 2 : element.getIndex() == 1 ? 3 : -1;
            case NORMAL -> 4;
            case GENERIC -> element.getIndex();
            default -> -1;
        };
    }

    private static boolean isNormalized(VertexFormatElement element) {
        return element.getUsage() == VertexFormatElement.EnumUsage.COLOR
                || element.getUsage() == VertexFormatElement.EnumUsage.NORMAL;
    }
}
