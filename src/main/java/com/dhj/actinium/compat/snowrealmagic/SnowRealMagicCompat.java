package com.dhj.actinium.compat.snowrealmagic;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
 * Compatibility for {@code Snow! Real Magic!} (mod id {@code snowrealmagic}).
 *
 * <p>SRM replaces a block that can be covered in snow (e.g. a fence) with a snow layer that
 * carries a {@code SnowTile}; the covered block's model is only re-emitted inside
 * {@code BlockRendererDispatcher.renderBlock} (SRM redirects this entry point and re-renders the
 * contained block at {@code RETURN}). Actinium's fast chunk path meshes model blocks straight from
 * their baked model and never calls that entry point, so the covered block disappears from the
 * chunk. Blocks that SRM owns must therefore fall back to the vanilla dispatcher path.
 */
public final class SnowRealMagicCompat {
    /**
     * The mod ID of Snow! Real Magic!.
     */
    public static final String MODID = "snowrealmagic";
    public static final boolean IS_LOADED = Loader.isModLoaded(MODID);

    private SnowRealMagicCompat() {
    }

    /**
     * Returns whether {@code block} is the snow layer as replaced by SRM and therefore must be
     * rendered through the vanilla {@code BlockRendererDispatcher} instead of the fast chunk path.
     *
     * <p>Identified by the {@code minecraft:snow_layer} registry name (SRM registers its
     * {@code ModSnowBlock} under that name), which covers both the vanilla {@link Blocks#SNOW}
     * instance and SRM's replacement without referencing any SRM class.
     */
    public static boolean shouldForceVanillaRender(Block block) {
        if (!IS_LOADED) {
            return false;
        }
        ResourceLocation name = block.getRegistryName();
        return name != null
            && "minecraft".equals(name.getNamespace())
            && "snow_layer".equals(name.getPath());
    }
}
