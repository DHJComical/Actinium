package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor {
    @Accessor("fogState")
    static GlStateManager.FogState celeritas$getFogState() {
        throw new AssertionError();
    }
}
