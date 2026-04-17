package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Accessor("fogColorRed")
    float celeritas$getFogColorRed();

    @Accessor("fogColorGreen")
    float celeritas$getFogColorGreen();

    @Accessor("fogColorBlue")
    float celeritas$getFogColorBlue();
}
