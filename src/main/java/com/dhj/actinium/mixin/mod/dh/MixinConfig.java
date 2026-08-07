package com.dhj.actinium.mixin.mod.dh;

import com.dhj.actinium.compat.dh.DistantHorizonsCompat;
import com.seibel.distanthorizons.core.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Config.class, remap = false)
public abstract class MixinConfig {
    @Inject(method = "<clinit>", at = @At("HEAD"), remap = false)
    private static void ensureClientBindingsBeforeConfigInitialization(CallbackInfo ci) {
        DistantHorizonsCompat.ensureClientBindings();
    }
}
