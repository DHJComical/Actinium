package com.gtnewhorizon.gtnhlib.compat;

import com.cleanroommc.discovery.CleanroomModDiscoverer;

public final class Mods {
    public static final boolean ARCHITECTURECRAFT = isModPresent("architecturecraft");
    public static final boolean CHUNKANIMATOR = isModPresent("chunkanimator");
    public static final boolean DEPTHSUPDATE = isModPresent("depthsupdate");
    public static final boolean DISTANTHORIZONS = isModPresent("distanthorizons");
    public static final boolean FLUIDLOGGED_API = isModPresent("fluidlogged_api");
    public static final boolean FLUXLOADING = isModPresent("fluxloading");
    public static final boolean NEOFONTRENDER = isModPresent("neofontrender");
    public static final boolean RFP2 = isModPresent("rfp2");
    public static final boolean SNOWREALMAGIC = isModPresent("snowrealmagic");

    private Mods() {}

    public static boolean isModPresent(String modId) {
        return CleanroomModDiscoverer.instance().isModPresent(modId);
    }
}
