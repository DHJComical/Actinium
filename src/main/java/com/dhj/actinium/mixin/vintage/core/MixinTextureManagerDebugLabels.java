package com.dhj.actinium.mixin.vintage.core;

import com.gtnewhorizons.angelica.glsm.GLDebug;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureManager.class)
public class MixinTextureManagerDebugLabels {
    @Inject(method = "loadTexture", at = @At("RETURN"))
    private void celeritas$nameTextureObject(ResourceLocation resource, ITextureObject texture, CallbackInfoReturnable<Boolean> cir) {
        if (texture == null || texture == TextureUtil.MISSING_TEXTURE || GLStateManager.isRecordingDisplayList()) {
            return;
        }

        int currentTexture = GLStateManager.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int textureId = texture.getGlTextureId();
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GLDebug.nameObject(GL11.GL_TEXTURE, textureId, resource.toString());
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
    }
}
