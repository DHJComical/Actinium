package org.taumc.celeritas.mixin.core;

import com.gtnewhorizons.angelica.client.rendering.DeferredDrawBatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.VanillaBufferBuilderRenderer;

@Mixin(Tessellator.class)
public class MixinTessellator {
    @Shadow
    @Final
    private BufferBuilder buffer;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDraw(CallbackInfo ci) {
        if (DeferredDrawBatcher.capture(this.buffer)) {
            ci.cancel();
            return;
        }

        this.buffer.finishDrawing();
        VanillaBufferBuilderRenderer.draw(this.buffer, "Tessellator");
        ci.cancel();
    }
}
