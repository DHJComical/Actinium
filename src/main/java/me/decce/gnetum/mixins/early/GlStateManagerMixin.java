//Courtesy of Moulberry
//Adapted from https://github.com/Moulberry/MCHUDCaching/blob/33750bed95cfbbf6e63dca90bdb9c9d767a218e2/src/main/java/io/github/moulberry/hudcaching/mixins/MixinGlStateManager.java
//under the CC BY 3.0 license (https://github.com/Moulberry/MCHUDCaching/blob/33750bed95cfbbf6e63dca90bdb9c9d767a218e2/LICENSE)

package me.decce.gnetum.mixins.early;

import me.decce.gnetum.Gnetum;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
    @Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true)
    private static void gnetum$blendFunc(int srcFactor, int dstFactor, CallbackInfo ci) {
        if (Gnetum.rendering) {
            GlStateManager.tryBlendFuncSeparate(srcFactor, dstFactor, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ci.cancel();
        }
    }
}
