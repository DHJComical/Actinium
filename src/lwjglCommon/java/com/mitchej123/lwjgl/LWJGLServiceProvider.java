package com.mitchej123.lwjgl;

import com.gtnewhorizons.angelica.config.SystemProperties;

public final class LWJGLServiceProvider {
    public static final LWJGLService LWJGL = createInstance();
    public static final int POINTER_SIZE = LWJGL.getPointerSize();
    public static final long NULL = 0L;

    private LWJGLServiceProvider() {
    }

    static LWJGLService constructInstance(String className) {
        try {
            var clz = Class.forName(className);
            var method = clz.getDeclaredMethod("create");
            return (LWJGLService) method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static LWJGLService createInstance() {
        if (SystemProperties.USE_SDL_GPU) {
            // SDL GPU mode has no real GL context; route GL calls through the SDL backend.
            // Reflective instantiation keeps this class (also compiled by the glsm source set)
            // free of a compile-time dependency on the sdl-gpu module.
            try {
                var clz = Class.forName("com.gtnewhorizons.angelica.sdlgpu.SDLGPULWJGLService");
                return (LWJGLService) clz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Failed to create SDL GPU LWJGL service", e);
            }
        }
        return constructInstance("com.mitchej123.lwjgl.lwjgl3.LWJGL3Service");
    }
}

