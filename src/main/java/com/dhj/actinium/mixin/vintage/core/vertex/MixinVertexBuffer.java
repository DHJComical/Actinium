package com.dhj.actinium.mixin.vintage.core.vertex;

import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dhj.actinium.render.VanillaVertexBufferRenderer;

import java.nio.ByteBuffer;

@Mixin(VertexBuffer.class)
public class MixinVertexBuffer {
    @Shadow
    private int glBufferId;

    @Shadow
    @Final
    private VertexFormat vertexFormat;

    @Shadow
    private int count;

    @Inject(method = "bufferData", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileBufferData(ByteBuffer data, CallbackInfo ci) {
        VanillaVertexBufferRenderer.uploadVertexBuffer(this.vertexFormat, this.glBufferId, data, GL15.GL_STATIC_DRAW);
        this.count = data.limit() / this.vertexFormat.getSize();
        ci.cancel();
    }

    @Inject(method = "drawArrays", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDrawArrays(int mode, CallbackInfo ci) {
        VanillaVertexBufferRenderer.drawVertexBuffer(this.vertexFormat, this.glBufferId, this.count, mode);
        ci.cancel();
    }

    @Inject(method = "deleteGlBuffers", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDeleteGlBuffers(CallbackInfo ci) {
        if (this.glBufferId >= 0) {
            VanillaVertexBufferRenderer.deleteVertexBuffer(this.glBufferId);
            this.glBufferId = -1;
        }
        ci.cancel();
    }
}

