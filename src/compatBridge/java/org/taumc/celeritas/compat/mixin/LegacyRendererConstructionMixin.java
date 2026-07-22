package org.taumc.celeritas.compat.mixin;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import com.dhj.actinium.render.terrain.compile.light.LightDataCache;
import com.dhj.actinium.render.terrain.compile.pipeline.VintageBlockRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.taumc.celeritas.compat.LegacyRendererFactory;

/**
 * Makes the legacy renderer binary name the live renderer while the compatibility mod is installed.
 */
@Mixin(value = VintageChunkBuildContext.class, remap = false)
public abstract class LegacyRendererConstructionMixin {
    @Redirect(
            method = "<init>(Lnet/minecraft/client/multiplayer/WorldClient;Lorg/embeddedt/embeddium/impl/render/chunk/RenderPassConfiguration;)V",
            at = @At(value = "NEW", target = "Lcom/dhj/actinium/render/terrain/compile/pipeline/VintageBlockRenderer;")
    )
    private VintageBlockRenderer actinium$createLegacyRenderer(VintageChunkBuildContext context,
                                                               LightDataCache cache,
                                                               WorldClient world,
                                                               RenderPassConfiguration<?> configuration) {
        return LegacyRendererFactory.create(context, cache);
    }
}
