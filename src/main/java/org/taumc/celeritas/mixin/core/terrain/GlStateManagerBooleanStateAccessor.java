package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.BooleanState.class)
public interface GlStateManagerBooleanStateAccessor {
    @Accessor("currentState")
    boolean celeritas$getCurrentState();
}
