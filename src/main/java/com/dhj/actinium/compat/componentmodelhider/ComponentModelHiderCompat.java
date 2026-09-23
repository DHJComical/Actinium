package com.dhj.actinium.compat.componentmodelhider;

import com.gtnewhorizon.gtnhlib.compat.Mods;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Compatibility for the Component Model Hider (mod id {@code component_model_hider}), the standalone
 * extraction of Multiblocked's block-model hiding API that Modular Machinery CE drives through
 * {@code com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData}.
 *
 * <p>The hider performs its hiding from inside the vanilla chunk rebuild: its {@code RenderChunkMixin}
 * redirects the {@code BlockRendererDispatcher#renderBlock} call in
 * {@code RenderChunk#rebuildChunk}, returns {@code false} for a disabled position so the block emits
 * no geometry, and arms {@code MultiblockWorldSavedData.isBuildingChunk} around that call. The armed
 * flag is what makes the rest of the hider work: {@code MultiblockWorldSavedData#isModelDisabled}
 * reports a position as disabled only while it is set, which is the signal its
 * {@code BlockModelRenderer} / {@code ForgeBlockModelRenderer} redirects and its CodeChickenLib hooks
 * consume, so a hidden block stops culling the faces of its neighbours while everything outside a
 * chunk build (the block breaking and damage overlays) keeps drawing normally. Its
 * {@code TileEntityRendererDispatcher#getRenderer} injection is not gated at all and consults the
 * disabled-position set directly.</p>
 *
 * <p>Its {@code BlockVisitor} transformer is not part of that in 1.12.2: it looks for a
 * {@code Block.doesSideBlockRendering} method to hook, and 1.12.2 has no such method - its face
 * culling lives in {@code Block#shouldSideBeRendered}, which ends in
 * {@code !blockAccess.getBlockState(pos.offset(side)).isOpaqueCube()}. The neighbour rule is therefore
 * carried by the {@code BlockModelRenderer} redirects alone.</p>
 *
 * <p>Actinium meshes terrain in {@code ChunkBuilderMeshingTask} and never calls
 * {@code RenderChunk#rebuildChunk}, so neither half of that contract ever ran: the hidden blocks were
 * still meshed and, because the Actinium fast mesher calls {@code shouldSideBeRendered} itself instead
 * of going through {@code BlockModelRenderer}, their neighbours were still culled. This class
 * re-applies both halves from the Actinium mesher - the per-position skip, and the neighbour rule,
 * plus the build gate that hands the {@code BlockModelRenderer} and CodeChickenLib decisions back to
 * the hider's own hooks on the vanilla dispatcher fallback path - and changes nothing else about how a
 * block is meshed.</p>
 *
 * <p>No hider class is ever touched while the mod is absent: {@link #IS_LOADED} is read from
 * {@link Mods}, and every foreign reference lives in {@link ComponentModelHiderBridge}, which is only
 * loaded once that flag is true. Hot-path call sites test {@link #IS_LOADED} first, so a client
 * without the mod neither loads the bridge nor allocates neighbour positions.</p>
 */
public final class ComponentModelHiderCompat {
    /**
     * The mod id the hider ships under (CurseForge project 940949, file 4885858).
     */
    public static final String MODID = "component_model_hider";

    /**
     * Whether the hider is present. Read once here so that a call site can decide with a constant
     * instead of probing the mod list per block.
     */
    public static final boolean IS_LOADED = Mods.COMPONENT_MODEL_HIDER;

    private ComponentModelHiderCompat() {
    }

    /**
     * Returns whether the block model at {@code pos} is currently hidden. Only answers while
     * {@link #beginBuild()} is in effect on the calling thread, which is the same condition the
     * hider's own {@code isModelDisabled} applies, so in-world overlays outside a chunk build keep
     * rendering hidden positions.
     */
    public static boolean isHidden(BlockPos pos) {
        if (!IS_LOADED) {
            return false;
        }
        return ComponentModelHiderBridge.isHidden(pos);
    }

    /**
     * Returns whether the neighbour of {@code pos} towards {@code dir} is hidden, in which case the
     * face looking at it must still be drawn. This is the second half of the hider's contract, and
     * the Actinium fast mesher has to apply it itself: vanilla only learns the rule from the hider's
     * redirects inside {@code BlockModelRenderer}, which that mesher does not run, so without this a
     * hidden block still occludes and punches a see-through hole into every adjacent block.
     *
     * <p>The offset is taken only when the hider is present, so a client without the mod pays a
     * single constant read per culled face.</p>
     */
    public static boolean isNeighbourHidden(BlockPos pos, EnumFacing dir) {
        if (!IS_LOADED) {
            return false;
        }
        return ComponentModelHiderBridge.isHidden(pos.offset(dir));
    }

    /**
     * Arms the hider's culling hooks for one chunk mesh build on the calling thread. Mirrors what
     * {@code RenderChunkMixin} did around each vanilla {@code renderBlock} call, but for the whole
     * build: inside the build the flag only affects the culling queries the hider hooks, and no
     * other thread is affected because it is thread-local.
     */
    public static void beginBuild() {
        if (IS_LOADED) {
            ComponentModelHiderBridge.beginBuild();
        }
    }

    /**
     * Disarms the hooks armed by {@link #beginBuild()}, restoring the state a fresh worker thread
     * starts from.
     */
    public static void endBuild() {
        if (IS_LOADED) {
            ComponentModelHiderBridge.endBuild();
        }
    }
}
