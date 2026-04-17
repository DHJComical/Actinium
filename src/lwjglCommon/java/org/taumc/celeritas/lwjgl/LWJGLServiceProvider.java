package org.taumc.celeritas.lwjgl;

/**
 * Provides the LWJGL3-backed service implementation used by Cleanroom builds.
 */
public final class LWJGLServiceProvider {
    public static final LWJGLService LWJGL = createInstance();
    public static final int POINTER_SIZE = LWJGL.getPointerSize();
    public static final long NULL = 0L;

    private LWJGLServiceProvider() {}

    static LWJGLService constructInstance(String className) {
        try {
            var clz = Class.forName(className);
            var method = clz.getDeclaredMethod("create");
            return (LWJGLService)method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static LWJGLService createInstance() {
        return constructInstance("org.taumc.celeritas.lwjgl.lwjgl3.LWJGL3Service");
    }
}
