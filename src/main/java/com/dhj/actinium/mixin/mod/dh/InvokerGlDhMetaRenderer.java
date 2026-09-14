package com.dhj.actinium.mixin.mod.dh;

import com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer;
import com.seibel.distanthorizons.common.render.openGl.glObject.texture.GlDhDepthTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes Distant Horizons' meta-renderer texture state for the F3 debug overlay. */
@Mixin(value = GlDhMetaRenderer.class, remap = false)
public interface InvokerGlDhMetaRenderer {
    @Accessor("textureWidth")
    int actinium$getTextureWidth();

    @Accessor("textureHeight")
    int actinium$getTextureHeight();

    @Accessor("depthTexture")
    GlDhDepthTexture actinium$getDepthTexture();
}
