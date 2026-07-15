package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.render.EndPortalRenderer;
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
            IrisGlDebug.logDebugInfo("end-portal-core-profile type={} pos=[{},{},{}]", te.getClass().getName(), x, y, z);
        }

        double distanceSq = x * x + y * y + z * z;
        int vanillaLayerCount = this.getPasses(distanceSq);

        EndPortalRenderer.render(te, x, y, z, this.getOffset(), vanillaLayerCount);
    }
}
