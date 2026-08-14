package com.dhj.actinium.mixin.mod.lumenized;

import gregtech.client.utils.RenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The vanilla main framebuffer uses a plain GL_DEPTH_COMPONENT24 renderbuffer. The
 * non-stencil branch of Lumenized's hookDepthBuffer binds that renderbuffer to
 * GL_DEPTH_STENCIL_ATTACHMENT, which does not match its format, leaving the bloom FBO
 * without a usable depth attachment — every bloom pass then draws through walls.
 *
 * <p>Re-binding the same renderbuffer to GL_DEPTH_ATTACHMENT (the format it actually
 * has) is idempotent with the first bind in the method and makes depth testing work.
 */
@Mixin(value = RenderUtil.class, remap = false)
public abstract class MixinRenderUtilDepth {
    @ModifyArg(
        method = "hookDepthBuffer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OpenGlHelper;glFramebufferRenderbuffer(IIII)V",
            ordinal = 2,
            remap = true
        ),
        index = 1,
        remap = false
    )
    private static int actinium$useMatchingDepthAttachment(int attachment) {
        return 36096;
    }
}
