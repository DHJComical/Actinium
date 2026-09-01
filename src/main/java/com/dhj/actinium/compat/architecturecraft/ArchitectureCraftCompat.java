package com.dhj.actinium.compat.architecturecraft;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
 * Compatibility for ArchitectureCraft (mod id {@code architecturecraft}; the TridentMC and Spocel
 * releases share it).
 *
 * <p>ArchitectureCraft never relies on baked models for its world rendering. Its blockstates only
 * publish a {@code normal} variant (the runtime definition comes from its generated resource pack),
 * so every {@code facing=*} state resolves to Forge's {@code FancyMissingModel} in the bake
 * registry, and the real geometry is emitted by a {@code BlockRendererDispatcher} replacement that
 * {@code CustomBlockDispatcher.inject()} installs into {@code Minecraft.blockRenderDispatcher}
 * during postInit. The vanilla chunk rebuild calls that entry point and the blocks stay visible;
 * Actinium's fast chunk path reads {@code BlockModelShapes.getModelForState} directly, hits the
 * missing model and skips the block (#101). The dispatcher replacement must therefore not be
 * bypassed: the compat forces ArchitectureCraft blocks down the vanilla dispatcher fallback,
 * whose {@code RenderTargetWorld} target writes into a {@code BufferBuilder} without touching GL,
 * which keeps it safe on mesh worker threads.</p>
 */
public final class ArchitectureCraftCompat {
    /**
     * The mod ID used by both maintained ArchitectureCraft releases.
     */
    public static final String MODID = "architecturecraft";
    public static final boolean IS_LOADED = Loader.isModLoaded(MODID);

    private ArchitectureCraftCompat() {
    }

    /**
     * Returns whether {@code block} belongs to ArchitectureCraft and must be rendered through the
     * vanilla {@code BlockRendererDispatcher}, which ArchitectureCraft replaces with its own
     * dispatcher providing the real shape geometry.
     */
    public static boolean shouldForceVanillaRender(Block block) {
        if (!IS_LOADED) {
            return false;
        }
        return ArchitectureCraftRenderRouting.isArchitectureCraftNamespace(block.getRegistryName());
    }
}
