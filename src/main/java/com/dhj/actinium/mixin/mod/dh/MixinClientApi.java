package com.dhj.actinium.mixin.mod.dh;

import com.dhj.actinium.compat.dh.DistantHorizonsCompat;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prepares Actinium's render state before Distant Horizons performs its single vanilla LOD render. */
@Mixin(value = ClientApi.class, remap = false)
public abstract class MixinClientApi {
    @Inject(
        method = "renderLods()V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        expect = 1,
        remap = false
    )
    private void actinium$prepareVanillaLodRender(CallbackInfo ci) {
        if (!DistantHorizonsCompat.prepareVanillaLodRender(
            Minecraft.getMinecraft().world,
            ClientApi.RENDER_STATE.partialTickTime
        )) {
            ci.cancel();
        }
    }
}
