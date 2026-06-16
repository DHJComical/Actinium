package com.dhj.actinium.mixin.features.iris;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.color.ItemColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderItem.class)
public interface RenderItemAccessor {
    @Accessor("itemColors")
    ItemColors actinium$getItemColors();
}
