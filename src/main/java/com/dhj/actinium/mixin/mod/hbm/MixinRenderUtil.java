package com.dhj.actinium.mixin.mod.hbm;

import com.dhj.actinium.compat.hbm.HbmRenderStateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes HBM's private render-state scopes through GLSM's authoritative state stack. */
@Mixin(targets = "com.hbm.util.RenderUtil", remap = false)
public abstract class MixinRenderUtil {
    @Inject(method = "pushAttrib(I)V", at = @At("HEAD"), cancellable = true)
    private static void actinium$pushAttrib(int hbmMask, CallbackInfo ci) {
        HbmRenderStateCompat.pushAttrib(hbmMask);
        ci.cancel();
    }

    @Inject(method = "popAttrib()V", at = @At("HEAD"), cancellable = true)
    private static void actinium$popAttrib(CallbackInfo ci) {
        HbmRenderStateCompat.popAttrib();
        ci.cancel();
    }
}
