package org.taumc.celeritas.mixin.core.startup;

import net.coderbot.iris.Iris;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameSettings.class)
public abstract class MixinGameSettingsIrisInit {
    @Unique
    private static boolean celeritas$irisInitialized;

    @Inject(method = "loadOptions", at = @At("HEAD"))
    private void celeritas$initializeIris(CallbackInfo ci) {
        if (celeritas$irisInitialized || !Iris.enabled) {
            return;
        }

        celeritas$irisInitialized = true;
        Iris.INSTANCE.onEarlyInitialize();
    }
}
