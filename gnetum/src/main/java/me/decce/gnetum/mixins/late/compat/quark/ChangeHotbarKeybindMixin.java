package me.decce.gnetum.mixins.late.compat.quark;

import me.decce.gnetum.Gnetum;
import me.decce.gnetum.compat.quark.QuarkCompat;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.quark.management.feature.ChangeHotbarKeybind;

@Pseudo
@Mixin(value = ChangeHotbarKeybind.class, remap = false)
public abstract class ChangeHotbarKeybindMixin {
    @Shadow
    protected abstract float getRealHeight(float part);

    @Inject(method = "hudPre", at = @At("HEAD"), cancellable = true)
    private void gnetum$hudPre(RenderGameOverlayEvent.Pre event, CallbackInfo ci) {
        if (Gnetum.config.isEnabled()) {
            QuarkCompat.shift = -this.getRealHeight(event.getPartialTicks()) + 22.0F;
            ci.cancel();
        }
    }
}
