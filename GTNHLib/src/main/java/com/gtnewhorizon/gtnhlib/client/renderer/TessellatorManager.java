package com.gtnewhorizon.gtnhlib.client.renderer;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.Tessellator;

public final class TessellatorManager {
    public static final int DEFAULT_BUFFER_SIZE = 0x8000;
    private static final int DIRECT_TESSELLATOR_STACK_DEPTH = 16;

    private static final DirectTessellator mainInstance = new DirectTessellator(DEFAULT_BUFFER_SIZE);
    private static final CallbackTessellator mainCallbackInstance = new CallbackTessellator(DEFAULT_BUFFER_SIZE);
    private static final DirectTessellator[] directTessellators = new DirectTessellator[DIRECT_TESSELLATOR_STACK_DEPTH];

    private static int directTessellatorIndex = -1;
    private static boolean mainInstanceInStack = false;

    private TessellatorManager() {
    }

    private static DirectTessellator getDirectTessellator() {
        return directTessellators[directTessellatorIndex];
    }

    private static boolean hasDirectTessellator() {
        return directTessellatorIndex != -1;
    }

    private static boolean isMainTessellator(DirectTessellator tessellator) {
        return tessellator == mainInstance || tessellator == mainCallbackInstance;
    }

    private static void setDirectTessellator(DirectTessellator tessellator) {
        if (++directTessellatorIndex >= DIRECT_TESSELLATOR_STACK_DEPTH) {
            throw new IllegalStateException("DirectTessellator stack overflow");
        }
        mainInstanceInStack = mainInstanceInStack || isMainTessellator(tessellator);
        directTessellators[directTessellatorIndex] = tessellator;
    }

    public static DirectTessellator startCapturingDirect() {
        if (!mainInstanceInStack) {
            setDirectTessellator(mainInstance);
            return mainInstance;
        }
        final DirectTessellator tessellator = new DirectTessellator(DEFAULT_BUFFER_SIZE);
        setDirectTessellator(tessellator);
        return tessellator;
    }

    public static DirectTessellator startCapturingDirect(int capacity) {
        if (!mainInstanceInStack && DEFAULT_BUFFER_SIZE >= capacity) {
            setDirectTessellator(mainInstance);
            return mainInstance;
        }
        final DirectTessellator tessellator = new DirectTessellator(capacity);
        setDirectTessellator(tessellator);
        return tessellator;
    }

    public static DirectTessellator startCapturingDirect(VertexFormat format) {
        final DirectTessellator tessellator = startCapturingDirect();
        tessellator.setVertexFormat(format);
        return tessellator;
    }

    @Beta
    public static CallbackTessellator startCapturingDirect(DirectDrawCallback callback) {
        if (!mainInstanceInStack) {
            final CallbackTessellator tessellator = mainCallbackInstance;
            tessellator.setDrawCallback(callback);
            setDirectTessellator(tessellator);
            return tessellator;
        }
        final CallbackTessellator tessellator = new CallbackTessellator(DEFAULT_BUFFER_SIZE);
        tessellator.setDrawCallback(callback);
        setDirectTessellator(tessellator);
        return tessellator;
    }

    public static void startCapturingDirect(DirectTessellator tessellator) {
        setDirectTessellator(tessellator);
    }

    public static void stopCapturingDirect() {
        if (!hasDirectTessellator()) {
            throw new IllegalStateException("Tried to stop capturing when not capturing!");
        }
        final DirectTessellator tessellator = getDirectTessellator();
        directTessellators[directTessellatorIndex--] = null;
        if (isMainTessellator(tessellator)) {
            mainInstanceInStack = false;
        }
        tessellator.onRemovedFromStack();
    }

    public static IVertexArrayObject stopCapturingDirectToVBO(VertexBufferType bufferType) {
        if (!hasDirectTessellator()) {
            throw new IllegalStateException("Tried to stop capturing when not capturing!");
        }
        final DirectTessellator tessellator = getDirectTessellator();
        final IVertexArrayObject vbo = tessellator.uploadToVBO(bufferType);
        stopCapturingDirect();
        return vbo;
    }

    public static boolean shouldInterceptDraw(Tessellator tess) {
        return ((ITessellatorInstance) tess).gtnhlib$isCompiling() || hasDirectTessellator();
    }

    public static void cleanup() {
        while (hasDirectTessellator()) {
            stopCapturingDirect();
        }
    }
}
