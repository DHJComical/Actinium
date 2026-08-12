package net.coderbot.iris.debug;

/**
 * Debug/regression switches consumed by the shader pipeline.
 *
 * <p>Read live from a bridge registered by the host mod (Actinium), so
 * in-game config changes take effect without a restart and the shader module
 * does not depend on the host's runtime.
 */
public final class IrisDebugOptions {

    /** Bridge implemented by the host mod; reads its own config live. */
    public interface Bridge {
        boolean pbrDebugEnabled();
        boolean enableActiniumGlDebug();
        boolean enableCloudControlDebug();
        boolean enableFrameGlErrorCheck();
        boolean enablePostRenderGlErrorCheck();
        boolean enableActiniumPerfDebug();
        boolean enableActiniumGpuPerfDebug();
        boolean ignoreFramebufferErrors();
        boolean enableIris();
        boolean enableCeleritas();
        boolean defineIsIris();
        boolean enableHardcodedCustomUniforms();
        boolean disableF3Additions();
        boolean useTotalWorldTime();
        void cycleAnimationsMode();
    }

    private static volatile Bridge bridge;

    private IrisDebugOptions() {
    }

    public static void setBridge(Bridge newBridge) {
        bridge = newBridge;
    }

    public static boolean pbrDebugEnabled() {
        Bridge b = bridge;
        return b != null && b.pbrDebugEnabled();
    }

    public static boolean enableActiniumGlDebug() {
        Bridge b = bridge;
        return b != null && b.enableActiniumGlDebug();
    }

    public static boolean enableCloudControlDebug() {
        Bridge b = bridge;
        return b != null && b.enableCloudControlDebug();
    }

    public static boolean enableFrameGlErrorCheck() {
        Bridge b = bridge;
        return b != null && b.enableFrameGlErrorCheck();
    }

    public static boolean enablePostRenderGlErrorCheck() {
        Bridge b = bridge;
        return b != null && b.enablePostRenderGlErrorCheck();
    }

    public static boolean enableActiniumPerfDebug() {
        Bridge b = bridge;
        return b != null && b.enableActiniumPerfDebug();
    }

    public static boolean enableActiniumGpuPerfDebug() {
        Bridge b = bridge;
        return b != null && b.enableActiniumGpuPerfDebug();
    }

    public static boolean ignoreFramebufferErrors() {
        Bridge b = bridge;
        return b != null && b.ignoreFramebufferErrors();
    }

    public static boolean enableIris() {
        Bridge b = bridge;
        // Default matches ActiniumConfig's initial value; read once at Iris class load.
        return b != null ? b.enableIris() : true;
    }

    public static boolean enableCeleritas() {
        Bridge b = bridge;
        return b != null ? b.enableCeleritas() : true;
    }

    public static boolean defineIsIris() {
        Bridge b = bridge;
        return b != null ? b.defineIsIris() : true;
    }

    public static boolean enableHardcodedCustomUniforms() {
        Bridge b = bridge;
        return b != null && b.enableHardcodedCustomUniforms();
    }

    public static boolean disableF3Additions() {
        Bridge b = bridge;
        return b != null && b.disableF3Additions();
    }

    public static boolean useTotalWorldTime() {
        Bridge b = bridge;
        return b != null && b.useTotalWorldTime();
    }

    public static void cycleAnimationsMode() {
        Bridge b = bridge;
        if (b != null) {
            b.cycleAnimationsMode();
        }
    }
}
