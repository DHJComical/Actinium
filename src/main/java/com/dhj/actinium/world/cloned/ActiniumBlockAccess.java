package com.dhj.actinium.world.cloned;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import com.dhj.actinium.compat.fluidlogged.FluidloggedBlockAccess;

/**
 * Contains extensions to the vanilla {@link IBlockAccess}.
 */
public interface ActiniumBlockAccess extends IBlockAccess, FluidloggedBlockAccess {
    int getBlockTint(BlockPos pos, BiomeColorHelper.ColorResolver resolver);
}
