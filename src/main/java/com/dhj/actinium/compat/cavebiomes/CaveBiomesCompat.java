package com.dhj.actinium.compat.cavebiomes;

import net.celestiald.cavebiomes.api.WorldHeightAPI;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

/**
 * Compatibility gate for CaveBiomesAPI worlds (Caves Not Cliffs [Backported]).
 *
 * <p>CaveBiomesAPI extends the world height to {@code -64..320} by reshaping
 * {@code Chunk.storageArrays} to 24 sections whose index {@code i} holds world Y
 * {@code i * 16 + minY}. The reshape happens in {@code Chunk}'s constructor for
 * every world while the API is installed (its mixin bakes the extended layout
 * unconditionally), so any code that indexes {@code Chunk.storageArrays} with the
 * semantic {@code sectionY} ({@code worldY >> 4}) reads the underground storage
 * in place of surface storage and meshes cave content at surface positions
 * (issue #43). This is exactly why the API ships its own adapter for the other
 * modern renderer it supports (Nothirium's {@code getSection} translates the
 * section Y by the API's minimum section, cf. NothiriumRenderCompat). Reading
 * chunk data through the API-provided {@code Chunk} methods is safe (the API
 * rewrites them), so only direct {@code storageArrays} indexing needs the same
 * translation.</p>
 *
 * <p>Whether the API's configured range actually extends below 0 or above 256
 * ({@code WorldHeightAPI}), the storage layout follows the same rule
 * {@code sectionY - minSection} and reduces to the identity mapping for the
 * vanilla range, so the index translation is applied whenever the API is
 * installed and never otherwise.</p>
 */
public final class CaveBiomesCompat {
    public static final String MODID = "cavebiomesapi";

    /** Whether CaveBiomesAPI is installed. Backs every API-touching call. */
    public static final boolean IS_LOADED = Loader.isModLoaded(MODID);

    private CaveBiomesCompat() {
    }

    /**
     * Whether the given world uses the extended height range (height above 256
     * or below 0). Kept for callers that gate on the vertical range; the storage
     * index translation itself does not depend on it because the reshaped
     * storage layout applies to every world while the API is installed.
     */
    public static boolean usesExtendedHeight(World world) {
        return IS_LOADED && WorldHeightAPI.usesExtendedHeight(world);
    }

    /**
     * Maps a semantic section Y (the coordinate the renderer works in) to an
     * index into {@code Chunk.storageArrays}. With CaveBiomesAPI installed the
     * layout is shifted by the API's minimum section for every world; without it
     * the mapping is the identity, matching the vanilla 16-section layout.
     *
     * @param sectionY semantic section Y ({@code worldY >> 4}, may be negative
     *                 for worlds with sections below 0)
     * @return the storage array index; the caller is responsible for clamping it
     *         to the array length reported by the chunk
     */
    public static int storageSectionIndex(int sectionY) {
        if (!IS_LOADED) {
            return sectionY;
        }
        return CaveBiomesHeightMapping.storageSectionIndex(WorldHeightAPI.getMinSection(), sectionY);
    }

    /**
     * {@link #storageSectionIndex(int)} with the world kept in the signature for
     * clarity at the call sites; the world does not affect the mapping.
     */
    public static int storageSectionIndex(World world, int sectionY) {
        return storageSectionIndex(sectionY);
    }
}