package com.gtnewhorizon.gtnhlib.client.opengl;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VaoFunctions;
import net.coderbot.iris.debug.IrisGlDebug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import java.nio.IntBuffer;

public final class UniversalVAO {

    private static final Logger LOGGER = LogManager.getLogger("UniversalVAO");
    private static final ThreadLocal<VaoFunctions> FUNCTIONS = ThreadLocal.withInitial(() -> getImplementation(GLContext.getCapabilities()));

    private UniversalVAO() {
    }

    public static VaoFunctions getImplementation(ContextCapabilities caps) {
        if (caps.OpenGL30) {
            IrisGlDebug.logDebugInfo("universal-vao mode=gl30");
            return new VaoGL3();
        } else if (caps.GL_ARB_vertex_array_object) {
            IrisGlDebug.logDebugInfo("universal-vao mode=arb-gl30");
            return new VaoGL3();
        }

        VaoFunctions appleLwjgl3Fallback = VaoAppleLwjgl3Fallback.tryCreate();
        if (appleLwjgl3Fallback != null) {
            return appleLwjgl3Fallback;
        }

        LOGGER.warn("No VAO implementation available");
        IrisGlDebug.logDebugInfo("universal-vao mode=missing");
        return null;
    }

    public static void reinitializeGlContext() {
        FUNCTIONS.remove();
    }

    public static int getVertexArrayBinding() {
        return FUNCTIONS.get().getCurrentBinding();
    }

    public static int genVertexArrays() {
        return FUNCTIONS.get().glGenVertexArrays();
    }

    public static void genVertexArrays(IntBuffer output) {
        FUNCTIONS.get().glGenVertexArrays(output);
    }

    public static void deleteVertexArrays(int id) {
        FUNCTIONS.get().glDeleteVertexArrays(id);
    }

    public static void deleteVertexArrays(IntBuffer ids) {
        FUNCTIONS.get().glDeleteVertexArrays(ids);
    }

    public static boolean isVertexArray(int array) {
        return FUNCTIONS.get().glIsVertexArray(array);
    }

    public static void bindVertexArray(int id) {
        FUNCTIONS.get().glBindVertexArray(id);
    }

    private static final class VaoGL3 implements VaoFunctions {

        @Override
        public int getCurrentBinding() {
            return GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        }

        @Override
        public int glGenVertexArrays() {
            return GL30.glGenVertexArrays();
        }

        @Override
        public void glGenVertexArrays(IntBuffer output) {
            GL30.glGenVertexArrays(output);
        }

        @Override
        public void glDeleteVertexArrays(int id) {
            GL30.glDeleteVertexArrays(id);
        }

        @Override
        public void glDeleteVertexArrays(IntBuffer ids) {
            GL30.glDeleteVertexArrays(ids);
        }

        @Override
        public boolean glIsVertexArray(int id) {
            return GL30.glIsVertexArray(id);
        }

        @Override
        public void glBindVertexArray(int id) {
            GL30.glBindVertexArray(id);
        }
    }

}
