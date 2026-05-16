package org.taumc.celeritas.mixin.core;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(WorldVertexBufferUploader.class)
public class WorldVertexBufferUploaderCoreProfileMixin {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void celeritas$coreProfileDraw(BufferBuilder bufferBuilder, CallbackInfo ci) {
        if (bufferBuilder.getVertexCount() <= 0) {
            bufferBuilder.reset();
            ci.cancel();
            return;
        }

        int flags = celeritas$vertexFlags(bufferBuilder.getVertexFormat());
        if (flags < 0) {
            return;
        }

        ByteBuffer buffer = bufferBuilder.getByteBuffer();
        TessellatorStreamingDrawer.drawPacked(
            buffer,
            bufferBuilder.getDrawMode(),
            flags,
            bufferBuilder.getVertexCount()
        );
        bufferBuilder.reset();
        ci.cancel();
    }

    private static int celeritas$vertexFlags(VertexFormat format) {
        if (format == DefaultVertexFormats.POSITION_COLOR) {
            return VertexFlags.convertToFlags(false, true, false, false);
        }

        if (format == DefaultVertexFormats.POSITION_TEX) {
            return VertexFlags.convertToFlags(true, false, false, false);
        }

        if (format == DefaultVertexFormats.POSITION_TEX_COLOR) {
            return VertexFlags.convertToFlags(true, true, false, false);
        }

        return -1;
    }
}
