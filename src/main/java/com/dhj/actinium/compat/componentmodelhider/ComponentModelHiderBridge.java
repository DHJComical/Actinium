package com.dhj.actinium.compat.componentmodelhider;

import com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.util.math.BlockPos;

/**
 * The hider's own API surface, isolated in its own class so that {@link ComponentModelHiderCompat}
 * and its call sites link while the mod is absent: this class is only loaded once
 * {@link ComponentModelHiderCompat#IS_LOADED} is true.
 *
 * <p>It deliberately exposes nothing beyond the three calls the mesher needs, and it delegates the
 * hiding predicate to the hider's own {@code MultiblockWorldSavedData#isModelDisabled} rather than
 * reading the disabled-position set directly, so the gating semantics stay owned by the mod.</p>
 */
final class ComponentModelHiderBridge {
    private ComponentModelHiderBridge() {
    }

    /**
     * Returns whether {@code pos} is hidden. Consulted only while the hider's build flag is armed by
     * {@link #beginBuild()}; outside a build the mod's own gate reports every position as enabled.
     */
    static boolean isHidden(BlockPos pos) {
        return MultiblockWorldSavedData.isModelDisabled(pos);
    }

    /**
     * Sets the hider's per-thread "a chunk is being meshed" flag, which is what arms its
     * {@code Block#doesSideBlockRendering} hook, its {@code BlockModelRenderer} redirects and its
     * CodeChickenLib hooks.
     */
    static void beginBuild() {
        MultiblockWorldSavedData.isBuildingChunk.set(Boolean.TRUE);
    }

    /**
     * Clears the flag set by {@link #beginBuild()}. The thread local is initialised to false, so
     * clearing it restores the state an unarmed build thread observes.
     */
    static void endBuild() {
        MultiblockWorldSavedData.isBuildingChunk.set(Boolean.FALSE);
    }
}
