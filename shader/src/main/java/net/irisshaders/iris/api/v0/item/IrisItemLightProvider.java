package net.irisshaders.iris.api.v0.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IrisItemLightProvider {
    default int getLightEmission(EntityPlayer player, ItemStack stack) {
        return 0;
    }
}
