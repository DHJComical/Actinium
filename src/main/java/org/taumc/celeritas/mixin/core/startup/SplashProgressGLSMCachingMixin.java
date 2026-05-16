package org.taumc.celeritas.mixin.core.startup;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.recording.ImmediateModeRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("deprecation")
@Mixin(targets = "net/minecraftforge/fml/client/SplashProgress")
public class SplashProgressGLSMCachingMixin {
    @Inject(method = "start", at = @At("HEAD"))
    private static void celeritas$initSplashTessellator(CallbackInfo ci) {
        ImmediateModeRecorder.initSplashTessellator();
    }

    @Inject(method = "finish", at = @At("RETURN"))
    private static void celeritas$finishSplash(CallbackInfo ci) {
        ImmediateModeRecorder.destroySplashTessellator();
        GLStateManager.markSplashComplete();
    }
}
