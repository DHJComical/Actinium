package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.render.EndPortalBatchRenderer;
import net.coderbot.iris.debug.IrisGlDebug;
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer;
import net.minecraft.tileentity.TileEntityEndPortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityEndPortalRenderer.class)
public abstract class TileEntityEndPortalRendererIrisMixin {
    @Unique
    private static int actinium$portalLogCount;

    @Shadow
    protected abstract int getPasses(double distanceSq);

    @Shadow
    protected abstract float getOffset();

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void actinium$renderGtnhPortal(
            TileEntityEndPortal te,
            double x,
            double y,
            double z,
            float partialTicks,
            int destroyStage,
            float alpha,
            CallbackInfo ci
    ) {
        ci.cancel();

        if (IrisGlDebug.shouldLogPortalRenderEvents() && actinium$portalLogCount++ < 8) {
            IrisGlDebug.logDebugInfo("end-portal-projective type={} pos=[{},{},{}]", te.getClass().getName(), x, y, z);
        }

        double distanceSq = x * x + y * y + z * z;
        int passes = this.getPasses(distanceSq);
        float topOffset = this.getOffset();

        if (!EndPortalBatchRenderer.enqueue(te, x, y, z, passes, topOffset)) {
            EndPortalBatchRenderer.renderImmediate(te, x, y, z, passes, topOffset);
        }
    }
}
