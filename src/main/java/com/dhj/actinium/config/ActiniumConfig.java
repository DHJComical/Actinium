package com.dhj.actinium.config;

public final class ActiniumConfig {
    public static boolean enableIris = true;
    public static boolean enableCeleritas = true;
    public static boolean enableThreadedChunkBuilding = true;
    public static int chunkBuilderThreadCount = 0;
    public static boolean useVanillaChunkTracking = false;
    public static boolean disableF3Additions = false;
    public static boolean speedupAnimations = false;
    public static boolean useTotalWorldTime = false;
    public static boolean enableHardcodedCustomUniforms = false;
    public static boolean enablePBRDebug = false;
    public static boolean defineIsIris = true;
    public static int itemRendererCacheSize = 512;

    private ActiniumConfig() {
    }
}
