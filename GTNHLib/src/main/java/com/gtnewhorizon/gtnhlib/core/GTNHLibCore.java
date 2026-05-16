package com.gtnewhorizon.gtnhlib.core;

import net.minecraft.launchwrapper.Launch;

public final class GTNHLibCore {
    private GTNHLibCore() {}

    public static boolean isObf() {
        Object value = Launch.blackboard != null ? Launch.blackboard.get("fml.deobfuscatedEnvironment") : null;
        if (value instanceof Boolean deobf) {
            return !deobf;
        }
        return false;
    }
}
