package com.dhj.actinium.mixin.mod.dh;

import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClientWrapper.class, remap = false)
public class MixinMinecraftClientWrapper {
    @Unique
    private static final Logger actinium$LOGGER = LogManager.getLogger("ActiniumDHCompat");

    @Inject(method = "disableVanillaClouds", at = @At("HEAD"), cancellable = true, remap = false)
    private void actinium$keepVanillaCloudSetting(CallbackInfo ci) {
        // Actinium's video options remain the authority for vanilla cloud rendering.
        actinium$LOGGER.info("Keeping the configured vanilla cloud setting while Distant Horizons is active");
        ci.cancel();
    }
}
