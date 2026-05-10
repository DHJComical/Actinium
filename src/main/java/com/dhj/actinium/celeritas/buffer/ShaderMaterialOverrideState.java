package com.dhj.actinium.celeritas.buffer;

/**
 * Tracks the current Iris shader material override for vanilla fallback quads.
 */
public final class ShaderMaterialOverrideState {
    private static final ThreadLocal<Integer> CURRENT_BLOCK_ID = ThreadLocal.withInitial(() -> -1);

    private ShaderMaterialOverrideState() {
    }

    public static void setBlockId(int blockId) {
        CURRENT_BLOCK_ID.set(blockId);
    }

    public static int getBlockId() {
        return CURRENT_BLOCK_ID.get();
    }

    public static void clear() {
        CURRENT_BLOCK_ID.set(-1);
    }
}
