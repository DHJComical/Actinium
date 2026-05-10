package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.gl.framebuffer.MinecraftFramebufferHelper;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(Framebuffer.class)
public class FramebufferIrisMixin implements IRenderTargetExt {
    @Unique
    private int actinium$depthBufferVersion;

    @Unique
    private int actinium$colorBufferVersion;

    @Unique
    private int actinium$depthTextureId = -1;

    @Shadow
    public boolean useDepth;

    @Shadow
    private boolean stencilEnabled;

    @Inject(method = "deleteFramebuffer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;unbindFramebuffer()V", shift = At.Shift.AFTER))
    private void actinium$onDestroyBuffers(CallbackInfo ci) {
        this.actinium$depthBufferVersion++;
        this.actinium$colorBufferVersion++;
    }

    @Inject(method = "bindFramebuffer(Z)V", at = @At("RETURN"))
    private void actinium$restoreMinecraftFramebufferBuffers(boolean setViewport, CallbackInfo ci) {
        MinecraftFramebufferHelper.restoreMinecraftFramebufferBuffers();
    }

    @Inject(method = "unbindFramebuffer()V", at = @At("RETURN"))
    private void actinium$restoreDefaultFramebufferBuffers(CallbackInfo ci) {
        MinecraftFramebufferHelper.restoreDefaultFramebufferBuffers();
    }

    @Inject(method = "deleteFramebuffer()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/shader/Framebuffer;depthBuffer:I", shift = At.Shift.BEFORE, ordinal = 0))
    private void actinium$deleteDepthTexture(CallbackInfo ci) {
        if (this.actinium$depthTextureId > -1) {
            GL11.glDeleteTextures(this.actinium$depthTextureId);
            this.actinium$depthTextureId = -1;
        }
    }

    @Redirect(method = "createFramebuffer(II)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/shader/Framebuffer;useDepth:Z"))
    private boolean actinium$skipVanillaDepthRenderbuffer(Framebuffer framebuffer) {
        return false;
    }

    @Inject(
        method = "createFramebuffer(II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferClear()V", shift = At.Shift.BEFORE, ordinal = 1)
    )
    private void actinium$createDepthTexture(int width, int height, CallbackInfo ci) {
        if (!this.useDepth) {
            return;
        }

        this.actinium$depthTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.actinium$depthTextureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);

        if (this.stencilEnabled) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, (IntBuffer) null);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.actinium$depthTextureId, 0);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, this.actinium$depthTextureId, 0);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.actinium$depthTextureId, 0);
        }

        this.actinium$depthBufferVersion++;
    }

    @Override
    public int iris$getDepthBufferVersion() {
        return this.actinium$depthBufferVersion;
    }

    @Override
    public int iris$getColorBufferVersion() {
        return this.actinium$colorBufferVersion;
    }

    @Override
    public int iris$getDepthTextureId() {
        return this.actinium$depthTextureId;
    }
}
