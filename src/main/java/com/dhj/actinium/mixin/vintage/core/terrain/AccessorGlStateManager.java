package com.dhj.actinium.mixin.vintage.core.terrain;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessors for vanilla {@link GlStateManager} state that GLSM keeps a parallel copy of.
 */
@Mixin(GlStateManager.class)
public interface AccessorGlStateManager {
    @Accessor("fogState")
    static GlStateManager.FogState celeritas$getFogState() {
        throw new AssertionError();
    }
}