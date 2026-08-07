package com.dhj.actinium.mixin.mod.dh;

import com.dhj.actinium.compat.dh.DistantHorizonsCompat;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DependencySetup.class, remap = false)
public abstract class MixinDependencySetup {
    @Inject(method = "createClientBindings", at = @At("HEAD"), cancellable = true, remap = false)
    private static void skipDuplicateClientBindings(CallbackInfo ci) {
        if (DistantHorizonsCompat.hasClientBindings()) {
            ci.cancel();
        }
    }
}
