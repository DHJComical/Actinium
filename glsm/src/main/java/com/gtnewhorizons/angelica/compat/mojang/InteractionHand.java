package com.gtnewhorizons.angelica.compat.mojang;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.item.ItemStack;

public enum InteractionHand {
    MAIN_HAND,
    OFF_HAND;

    public ItemStack getItemInHand(EntityPlayer player) {
        return player.getHeldItem(this == OFF_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
    }
}
