package me.decce.gnetum.mixins.early;

import me.decce.gnetum.FramebufferManager;
import me.decce.gnetum.Gnetum;
import me.decce.gnetum.gl.FramebufferTracker;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Unique
    private int gnetum$previouslyBoundFramebuffer;
    @Unique
    private boolean gnetum$renderingCachedHand;

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At("HEAD"), cancellable = true)
    public void gnetum$preRenderItemInFirstPerson(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_, CallbackInfo ci) {
        if (!Gnetum.config.isEnabled() || Gnetum.passManager.cachingDisabled(Gnetum.HAND_ELEMENT)) {
            return;
        }
        if (Gnetum.passManager.shouldRender(Gnetum.HAND_ELEMENT)) {
            gnetum$previouslyBoundFramebuffer = FramebufferTracker.getCurrentlyBoundFbo();
            Gnetum.passManager.begin();
            FramebufferManager.getInstance().bind();
            this.gnetum$renderingCachedHand = true;
        }
        else {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At("RETURN"))
    public void gnetum$postRenderItemInFirstPerson(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_, CallbackInfo ci) {
        if (this.gnetum$renderingCachedHand) {
            this.gnetum$renderingCachedHand = false;
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, gnetum$previouslyBoundFramebuffer);
            Gnetum.passManager.end();
        }
    }
}
