package me.decce.gnetum.mixins.early;

import me.decce.gnetum.FramebufferManager;
import me.decce.gnetum.Gnetum;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("RETURN"))
    private void gnetum$loadWorld(CallbackInfo ci) {
        if (Gnetum.config.isEnabled()){
            Gnetum.getFpsCounter().reset();
            FramebufferManager.getInstance().reset();
        }
    }
}
