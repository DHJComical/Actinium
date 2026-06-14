package org.taumc.celeritas.mixin.core;

import com.gtnewhorizons.angelica.client.rendering.DeferredDrawBatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import org.taumc.celeritas.impl.render.VanillaBufferBuilderRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldVertexBufferUploader.class)
public class MixinWorldVertexBufferUploader {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDraw(BufferBuilder bufferBuilder, CallbackInfo ci) {
        if (DeferredDrawBatcher.capture(bufferBuilder)) {
            ci.cancel();
            return;
        }

        if (TessellatorManager.shouldInterceptBufferBuilderDraw()) {
            TessellatorManager.interceptBufferBuilderDraw(bufferBuilder);
            ci.cancel();
            return;
        }

        VanillaBufferBuilderRenderer.draw(bufferBuilder, "WorldVertexBufferUploader");
        ci.cancel();
    }
}
