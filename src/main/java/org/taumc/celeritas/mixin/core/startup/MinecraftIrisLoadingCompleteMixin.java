package org.taumc.celeritas.mixin.core.startup;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftIrisLoadingCompleteMixin {
    @Unique
    private static boolean celeritas$firstInitComplete;

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fml/client/SplashProgress;drawVanillaScreen(Lnet/minecraft/client/renderer/texture/TextureManager;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void celeritas$onLoadingComplete(CallbackInfo ci) {
        if (Iris.enabled && !celeritas$firstInitComplete && GLStateManager.isMainThread()) {
            celeritas$firstInitComplete = true;
            Iris.onLoadingComplete();
        }
    }
}
