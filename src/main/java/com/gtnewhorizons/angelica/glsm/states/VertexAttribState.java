package com.gtnewhorizons.angelica.glsm.states;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * Per-VAO vertex attribute pointer state tracking (Mesa-style).
 * Ported from Angelica.
 */
public class VertexAttribState {
    public static final int MAX_ATTRIBS = 16;

    private static final Int2ObjectOpenHashMap<Attrib[]> vaoAttribs = new Int2ObjectOpenHashMap<>();
    private static Attrib[] current;
    private static final ArrayDeque<Attrib[]> pool = new ArrayDeque<>();
    private static int clientSideEnabledCount;

    public static void init(int defaultVAO) {
        vaoAttribs.clear();
        pool.clear();
        current = allocAttribArray();
        vaoAttribs.put(defaultVAO, current);
        clientSideEnabledCount = 0;
    }

    public static void onBindVertexArray(int newVAO) {
        current = vaoAttribs.computeIfAbsent(newVAO, k -> allocAttribArray());
        recomputeClientSideCount();
    }

    public static void onDeleteVertexArray(int vaoId) {
        final Attrib[] arr = vaoAttribs.remove(vaoId);
        if (arr != null) {
            pool.addLast(arr);
        }
    }

    public static void reset() {
        vaoAttribs.clear();
        pool.clear();
        current = null;
        clientSideEnabledCount = 0;
    }

    private static Attrib[] allocAttribArray() {
        Attrib[] arr = pool.pollFirst();
        if (arr != null) {
            for (Attrib a : arr) a.reset();
            return arr;
        }
        arr = new Attrib[MAX_ATTRIBS];
        for (int i = 0; i < MAX_ATTRIBS; i++) arr[i] = new Attrib();
        return arr;
    }

    public static void set(int index, int size, int type, boolean normalized, int stride, long offset, int vboId) {
        if (index < 0 || index >= MAX_ATTRIBS) return;
        final Attrib a = current[index];
        final boolean was = a.isClientSide();
        a.size = size;
        a.type = type;
        a.normalized = normalized;
        a.stride = stride;
        a.offset = offset;
        a.vboId = vboId;
        a.clientPointer = null;
        if (was) clientSideEnabledCount--;
    }

    public static void set(int index, int size, int type, boolean normalized, int stride, ByteBuffer pointer, int vboId) {
        if (index < 0 || index >= MAX_ATTRIBS) return;
        final Attrib a = current[index];
        final boolean was = a.isClientSide();
        a.size = size;
        a.type = type;
        a.normalized = normalized;
        a.stride = stride;
        a.offset = 0;
        a.vboId = vboId;
        a.clientPointer = (vboId == 0) ? pointer : null;
        final boolean now = a.isClientSide();
        if (was != now) clientSideEnabledCount += now ? 1 : -1;
    }

    public static void setEnabled(int index, boolean enabled) {
        if (index < 0 || index >= MAX_ATTRIBS) return;
        final Attrib a = current[index];
        if (a.clientPointer != null && a.enabled != enabled) {
            clientSideEnabledCount += enabled ? 1 : -1;
        }
        a.enabled = enabled;
    }

    public static Attrib get(int index) {
        return current[index];
    }

    private static void recomputeClientSideCount() {
        int count = 0;
        for (int i = 0; i < MAX_ATTRIBS; i++) {
            if (current[i].isClientSide()) count++;
        }
        clientSideEnabledCount = count;
    }

    public static boolean hasVBOBoundAttrib() {
        for (int i = 0; i < MAX_ATTRIBS; i++) {
            if (current[i].enabled && current[i].vboId != 0) return true;
        }
        return false;
    }

    public static boolean hasAnyClientSideEnabledAttrib() {
        return clientSideEnabledCount > 0;
    }

    public static class Attrib {
        public boolean enabled;
        public int size;
        public int type;
        public boolean normalized;
        public int stride;
        public long offset;
        public int vboId;
        public ByteBuffer clientPointer;

        public boolean isClientSide() {
            return enabled && clientPointer != null;
        }

        public void reset() {
            enabled = false;
            size = 0;
            type = 0;
            normalized = false;
            stride = 0;
            offset = 0;
            vboId = 0;
            clientPointer = null;
        }

        public int effectiveStride() {
            return (stride != 0) ? stride : size * typeSizeBytes();
        }

        public int typeSizeBytes() {
            return glTypeSizeBytes(type);
        }

        public static int glTypeSizeBytes(int glType) {
            switch (glType) {
                case GL11.GL_FLOAT:
                case GL11.GL_INT:
                case GL11.GL_UNSIGNED_INT: return 4;
                case GL11.GL_DOUBLE: return 8;
                case GL11.GL_SHORT:
                case GL11.GL_UNSIGNED_SHORT: return 2;
                case GL11.GL_BYTE:
                case GL11.GL_UNSIGNED_BYTE: return 1;
                default: return 4;
            }
        }

        public float readComponent(ByteBuffer buf, int base, int component) {
            switch (type) {
                case GL11.GL_FLOAT: return buf.getFloat(base + component * 4);
                case GL11.GL_DOUBLE: return (float) buf.getDouble(base + component * 8);
                case GL11.GL_INT: {
                    final int v = buf.getInt(base + component * 4);
                    return normalized ? v / (float) Integer.MAX_VALUE : (float) v;
                }
                case GL11.GL_UNSIGNED_INT: {
                    final int v = buf.getInt(base + component * 4);
                    return normalized ? (v & 0xFFFFFFFFL) / (float) 0xFFFFFFFFL : (float) (v & 0xFFFFFFFFL);
                }
                case GL11.GL_SHORT: {
                    final short v = buf.getShort(base + component * 2);
                    return normalized ? v / (float) Short.MAX_VALUE : (float) v;
                }
                case GL11.GL_UNSIGNED_SHORT: {
                    final int v = buf.getShort(base + component * 2) & 0xFFFF;
                    return normalized ? v / (float) 0xFFFF : (float) v;
                }
                case GL11.GL_BYTE: {
                    final byte v = buf.get(base + component);
                    return normalized ? v / 127.0f : (float) v;
                }
                case GL11.GL_UNSIGNED_BYTE: {
                    final int v = buf.get(base + component) & 0xFF;
                    return normalized ? v / 255.0f : (float) v;
                }
                default: return 0.0f;
            }
        }
    }
}
