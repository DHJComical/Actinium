package org.taumc.celeritas.mixin.core;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.VertexBufferUploader;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBufferUploader.class)
public class MixinVertexBufferUploader {
    @Shadow
    private VertexBuffer vertexBuffer;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDraw(BufferBuilder bufferBuilder, CallbackInfo ci) {
        this.vertexBuffer.bufferData(bufferBuilder.getByteBuffer());
        bufferBuilder.reset();
        ci.cancel();
    }
}
