package me.decce.gnetum.mixins.late.compat.xaerosminimap;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import me.decce.gnetum.FramebufferManager;
import me.decce.gnetum.Gnetum;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.graphics.ImprovedFramebuffer;

@Pseudo
@Mixin(value = ImprovedFramebuffer.class, remap = false)
public class ImprovedFramebufferMixin {
    @Inject(method = "bindFramebuffer", at = @At("HEAD"))
    private static void gnetum$bindFramebuffer(int type, int target, int framebufferIn, CallbackInfo ci, @Local(argsOnly = true, ordinal = 2) LocalIntRef framebuffer) {
        if (Gnetum.rendering && (framebufferIn == Minecraft.getMinecraft().getFramebuffer().framebufferObject || framebufferIn == 0)) {
            framebuffer.set(FramebufferManager.getInstance().id());
        }
    }
}
