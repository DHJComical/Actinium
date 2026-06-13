package me.decce.gnetum.util.time;

import org.lwjgl.glfw.GLFW;

public class GlfwTimeSource implements TimeSource {
    public static final long NANOS_IN_A_SECOND = 1_000_000_000L;

    @Override
    public long getNanos() {
        return (long) (GLFW.glfwGetTime() * NANOS_IN_A_SECOND);
    }

    public static boolean isAvailable() {
        return classExists("org.lwjgl.glfw.GLFW");
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
