package com.gtnewhorizon.gtnhlib.client.renderer;

import java.util.function.BooleanSupplier;

public final class RuntimeOptionsBridge {
    private static volatile BooleanSupplier allowDirectMemoryAccess = () -> true;

    private RuntimeOptionsBridge() {
    }

    public static boolean allowDirectMemoryAccess() {
        return allowDirectMemoryAccess.getAsBoolean();
    }

    public static void setAllowDirectMemoryAccess(BooleanSupplier supplier) {
        allowDirectMemoryAccess = supplier != null ? supplier : () -> true;
    }
}
