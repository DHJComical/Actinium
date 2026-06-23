package com.dhj.actinium.mixin.vintage.core.terrain;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.FogState.class)
public interface AccessorGlStateManagerFogState {
    @Accessor("fog")
    GlStateManager.BooleanState celeritas$getFog();

    @Accessor("mode")
    int celeritas$getMode();

    @Accessor("density")
    float celeritas$getDensity();

    @Accessor("start")
    float celeritas$getStart();

    @Accessor("end")
    float celeritas$getEnd();
}
