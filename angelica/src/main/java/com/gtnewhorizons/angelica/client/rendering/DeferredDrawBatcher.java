package com.gtnewhorizons.angelica.client.rendering;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.impl.render.VanillaBufferBuilderRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memAlloc;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memFree;

/**
 * 1.12 BufferBuilder adaptation of Angelica's deferred particle draw path.
 * Captures nested Tessellator.draw() calls while vanilla ParticleManager is inside a particle layer.
 */
public final class DeferredDrawBatcher {
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("actinium.deferredParticleBatching", "true"));
    private static final List<DrawRange> RANGES = new ArrayList<>();
    private static boolean active;
    private static boolean flushing;

    private DeferredDrawBatcher() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void enter() {
        if (!ENABLED || flushing) {
            return;
        }

        active = true;
        RANGES.clear();
    }

    public static boolean capture(BufferBuilder bufferBuilder) {
        if (!active || flushing) {
            return false;
        }

        bufferBuilder.finishDrawing();

        int vertexCount = bufferBuilder.getVertexCount();
        if (vertexCount <= 0) {
            bufferBuilder.reset();
            return true;
        }

        VertexFormat format = bufferBuilder.getVertexFormat();
        int drawMode = bufferBuilder.getDrawMode();
        int byteCount = vertexCount * format.getSize();
        ByteBuffer source = bufferBuilder.getByteBuffer().duplicate();
        source.position(0);
        source.limit(byteCount);

        ByteBuffer copy = memAlloc(byteCount);
        copy.put(source);
        copy.flip();

        RANGES.add(new DrawRange(captureStateKey(), copy, format, vertexCount, drawMode));
        bufferBuilder.reset();
        return true;
    }

    public static void exitAndFlush() {
        if (!active) {
            return;
        }

        active = false;
        if (RANGES.isEmpty()) {
            return;
        }

        flushing = true;
        try {
            RANGES.sort(Comparator.comparingLong(DrawRange::stateKey));
            for (DrawRange range : RANGES) {
                applyStateKey(range.stateKey());
                VanillaBufferBuilderRenderer.drawRaw(
                        range.buffer(),
                        range.format(),
                        range.vertexCount(),
                        range.drawMode(),
                        "DeferredParticle"
                );
            }
        } finally {
            for (DrawRange range : RANGES) {
                memFree(range.buffer());
            }
            RANGES.clear();
            flushing = false;
        }
    }

    static long captureStateKey() {
        int textureId = GLStateManager.getTextures().getTextureUnitBindings(0).getBinding();
        int srcRgb = GLStateManager.getBlendState().getSrcRgb();
        int dstRgb = GLStateManager.getBlendState().getDstRgb();
        boolean blendEnabled = GLStateManager.getBlendMode().isEnabled();
        boolean depthMask = GLStateManager.getDepthState().isEnabled();
        boolean tex0Enabled = GLStateManager.getTextures().getTextureUnitStates(0).isEnabled();
        boolean tex1Enabled = GLStateManager.getTextures().getTextureUnitStates(1).isEnabled();

        return packStateKey(textureId, srcRgb, dstRgb, blendEnabled, depthMask, tex0Enabled, tex1Enabled);
    }

    static long packStateKey(
            int textureId,
            int srcRgb,
            int dstRgb,
            boolean blendEnabled,
            boolean depthMask,
            boolean tex0Enabled,
            boolean tex1Enabled
    ) {
        return ((long) (textureId & 0xFFFFF) << 26)
                | ((long) (srcRgb & 0xFFF) << 14)
                | ((long) (dstRgb & 0xFFF) << 2)
                | (blendEnabled ? 2L : 0L)
                | (depthMask ? 1L : 0L)
                | (tex0Enabled ? (1L << 46) : 0L)
                | (tex1Enabled ? (1L << 47) : 0L);
    }

    static int unpackTextureId(long key) {
        return (int) ((key >> 26) & 0xFFFFF);
    }

    static int unpackSrcRgb(long key) {
        return (int) ((key >> 14) & 0xFFF);
    }

    static int unpackDstRgb(long key) {
        return (int) ((key >> 2) & 0xFFF);
    }

    static boolean unpackBlendEnabled(long key) {
        return (key & 2L) != 0;
    }

    static boolean unpackDepthMask(long key) {
        return (key & 1L) != 0;
    }

    static boolean unpackTex0Enabled(long key) {
        return (key & (1L << 46)) != 0;
    }

    static boolean unpackTex1Enabled(long key) {
        return (key & (1L << 47)) != 0;
    }

    static void applyStateKey(long key) {
        int textureId = unpackTextureId(key);
        int srcRgb = unpackSrcRgb(key);
        int dstRgb = unpackDstRgb(key);
        boolean blendEnabled = unpackBlendEnabled(key);
        boolean depthMask = unpackDepthMask(key);
        boolean tex0Enabled = unpackTex0Enabled(key);
        boolean tex1Enabled = unpackTex1Enabled(key);

        GLStateManager.getTextures().getTextureUnitStates(0).setEnabled(tex0Enabled);
        GLStateManager.getTextures().getTextureUnitStates(1).setEnabled(tex1Enabled);
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GLStateManager.glBlendFunc(srcRgb, dstRgb);
        if (blendEnabled) {
            GLStateManager.enableBlend();
        } else {
            GLStateManager.disableBlend();
        }
        GLStateManager.glDepthMask(depthMask);
    }

    private record DrawRange(long stateKey, ByteBuffer buffer, VertexFormat format, int vertexCount, int drawMode) {
    }
}
