package com.dhj.actinium.mixin.vintage.core;

import com.dhj.actinium.render.terrain.TileEntityBatchDrawGuard;
import com.dhj.actinium.runtime.ActiniumRuntime;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import org.embeddedt.embeddium.api.shader.buffer.BufferBuilderExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileEntityRendererDispatcher.class)
public class MixinTileEntityRendererDispatcherBatch {
    @Unique
    private static boolean actinium$warnedFinishedBatch;

    @Redirect(
        method = "drawBatch",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V")
    )
    private void actinium$drawBatchIfBuilding(Tessellator tessellator) {
        BufferBuilder buffer = tessellator.getBuffer();
        boolean building = ((BufferBuilderExtension) buffer).actinium$isDrawing();
        if (!TileEntityBatchDrawGuard.drawIfBuilding(building, tessellator::draw)
            && !actinium$warnedFinishedBatch) {
            actinium$warnedFinishedBatch = true;
            ActiniumRuntime.logger().warn(
                "Skipping an already-finished Forge tile entity batch after a nested renderer flushed it"
            );
        }
    }
}
