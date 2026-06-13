//Courtesy of Moulberry
//Adapted from https://github.com/Moulberry/MCHUDCaching/blob/33750bed95cfbbf6e63dca90bdb9c9d767a218e2/src/main/java/io/github/moulberry/hudcaching/mixins/MixinFrameBuffer.java
//under the CC BY 3.0 license (https://github.com/Moulberry/MCHUDCaching/blob/33750bed95cfbbf6e63dca90bdb9c9d767a218e2/LICENSE)

package me.decce.gnetum.mixins.early;

import me.decce.gnetum.FramebufferManager;
import me.decce.gnetum.Gnetum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public class FramebufferMixin {
    @Inject(method = "bindFramebuffer", at = @At("HEAD"), cancellable = true)
    public void gnetum$bindFramebuffer(boolean setViewport, CallbackInfo ci)
    {
        Framebuffer framebuffer = (Framebuffer) (Object) this;
        if (Gnetum.rendering && framebuffer == Minecraft.getMinecraft().getFramebuffer()){
            ci.cancel();
            FramebufferManager.getInstance().bind(setViewport);
        }
    }
}
