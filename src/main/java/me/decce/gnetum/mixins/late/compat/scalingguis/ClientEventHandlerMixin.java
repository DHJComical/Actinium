package me.decce.gnetum.mixins.late.compat.scalingguis;

import me.decce.gnetum.Gnetum;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spazley.scalingguis.handlers.ClientEventHandler;

@Mixin(value = ClientEventHandler.class, remap = false)
public class ClientEventHandlerMixin {
	@Inject(method = "onPreRenderGameOverlay", at = @At("HEAD"), cancellable = true)
	private void gnetum$cancelHudTakeOver(RenderGameOverlayEvent.Pre e, CallbackInfo ci) {
		if (Gnetum.config.isEnabled()) {
			ci.cancel();
		}
	}
}
