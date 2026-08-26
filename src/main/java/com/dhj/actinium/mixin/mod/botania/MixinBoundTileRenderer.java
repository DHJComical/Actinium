package com.dhj.actinium.mixin.mod.botania;

import com.dhj.actinium.compat.botania.BotaniaGlStateCompat;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.client.core.handler.BoundTileRenderer;

/**
 * Restores GL_TEXTURE_2D enablement after Botania's bound-tile wireframe pass.
 *
 * <p>Same GLSM state-cache leak as {@code BlockHighlightRenderHandler}: the handler
 * disables texture sampling to draw coordinate-bound outlines and can leave the active
 * texture unit disabled when it returns, so the following held-item pass renders flat
 * white. Re-enable the units so item rendering samples the atlas again.</p>
 */
@Mixin(value = BoundTileRenderer.class, remap = false)
public abstract class MixinBoundTileRenderer {

    @Inject(method = "onWorldRenderLast", at = @At("RETURN"))
    private static void actinium$restoreTextureAfterBoundTiles(RenderWorldLastEvent event, CallbackInfo ci) {
        BotaniaGlStateCompat.restoreTextureEnablement();
    }
}
