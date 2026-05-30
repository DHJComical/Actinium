package com.dhj.actinium.mixin.features.iris;

import net.minecraft.client.model.TexturedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TexturedQuad.class)
public interface TexturedQuadAccessor {
    @Accessor("invertNormal")
    boolean actinium$isInvertNormal();
}
