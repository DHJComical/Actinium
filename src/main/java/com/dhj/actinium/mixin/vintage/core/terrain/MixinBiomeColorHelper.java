package com.dhj.actinium.mixin.vintage.core.terrain;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import com.dhj.actinium.runtime.ActiniumRuntime;
import com.dhj.actinium.world.biome.BiomeColorNoise;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;

@Mixin(value = BiomeColorHelper.class, priority = 1200)
public class MixinBiomeColorHelper {
    /**
     * @author embeddedt
     * @reason reduce allocation rate, use Sodium's biome cache, use configurable biome blending,
     *         and apply biome color position noise
     */
    @Overwrite
    private static int getColorAtPos(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        if (blockAccess instanceof ActiniumBlockAccess) {
            // Use Sodium's more efficient biome cache. Noise injection for this path happens in
            // the biome color cache layer, NOT here, or the two would stack.
            return ((ActiniumBlockAccess)blockAccess).getBlockTint(pos, colorResolver);
        }

        int radius = ActiniumRuntime.options().quality.legacyBiomeBlendRadius;
        if (radius == 0) {
            // Noise is keyed to the block's own position, not to any blend sample position.
            return BiomeColorNoise.applyForResolver(colorResolver, pos.getX(), pos.getY(), pos.getZ(),
                    colorResolver.getColorAtPos(blockAccess.getBiome(pos), pos));
        } else {
            int blockCount = (radius * 2 + 1) * (radius * 2 + 1);

            int i = 0;
            int j = 0;
            int k = 0;

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            for(int z = -radius; z <= radius; z++) {
                for(int x = -radius; x <= radius; x++) {
                    mutablePos.setPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
                    int l = colorResolver.getColorAtPos(blockAccess.getBiome(mutablePos), mutablePos);
                    i += (l & 16711680) >> 16;
                    j += (l & 65280) >> 8;
                    k += l & 255;
                }
            }

            // The noise is applied once to the blended result: applying it per blend sample would
            // let the average cancel the variation out (larger radius = weaker effect).
            return BiomeColorNoise.applyForResolver(colorResolver, pos.getX(), pos.getY(), pos.getZ(),
                    (i / blockCount & 255) << 16 | (j / blockCount & 255) << 8 | k / blockCount & 255);
        }
    }
}
