package com.dhj.actinium.mixin.mod.ccl;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.stacks.BlendStateStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Compatibility bridge for CodeChickenLib's {@code GlStateTracker}.
 *
 * <p>CCL's tracker saves/restores GL state by reading the vanilla
 * {@code net.minecraft.client.renderer.GlStateManager} field cache. Under
 * Actinium's GLSM redirector those methods never run, so the field cache stays
 * frozen at its Java initial values; DE's per-frame HUD drawing therefore resets
 * the real GL state (blend/alpha/depth/cull/lighting) to those defaults every
 * frame, corrupting clouds, grass tinting and the main menu. This bridge reads
 * and restores the real GLSM tracked state instead. Loaded only when
 * CodeChickenLib is present (see MixinLate).
 */
@Mixin(targets = "codechicken.lib.render.state.GlStateTracker")
public abstract class MixinGlStateTracker {

    @Unique
    private static final Deque<Saved> actinium$stateStack = new ArrayDeque<>();

    /**
     * @author Actinium
     * @reason Save the real GLSM tracked state instead of the frozen vanilla field cache.
     */
    @Overwrite
    public static void pushState() {
        final Saved state = new Saved();
        state.alphaTest = GLStateManager.getAlphaTest().isEnabled();
        state.alphaFunc = GLStateManager.getAlphaState().getFunction();
        state.alphaRef = GLStateManager.getAlphaState().getReference();
        state.lighting = GLStateManager.getLightingState().isEnabled();
        final BlendStateStack blend = GLStateManager.getBlendState();
        state.blend = blend.isEnabled();
        state.blendSrcRgb = blend.getSrcRgb();
        state.blendDstRgb = blend.getDstRgb();
        state.blendSrcAlpha = blend.getSrcAlpha();
        state.blendDstAlpha = blend.getDstAlpha();
        state.depthTest = GLStateManager.getDepthState().isEnabled();
        state.depthFunc = GLStateManager.getDepthState().getFunc();
        state.depthMask = GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK) != 0;
        state.cull = GLStateManager.getCullState().isEnabled();
        state.cullMode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        state.rescaleNormal = GLStateManager.getRescaleNormalState().isEnabled();
        actinium$stateStack.push(state);
    }

    /**
     * @author Actinium
     * @reason Restore the real GLSM tracked state instead of the frozen vanilla field cache.
     */
    @Overwrite
    public static void popState() {
        if (actinium$stateStack.isEmpty()) {
            throw new IllegalStateException("Unable to pop the GL state as there is no saved state!");
        }
        final Saved state = actinium$stateStack.pop();
        if (state.alphaTest) {
            GLStateManager.enableAlphaTest();
        } else {
            GLStateManager.disableAlphaTest();
        }
        GLStateManager.glAlphaFunc(state.alphaFunc, state.alphaRef);
        if (state.lighting) {
            GLStateManager.enableLighting();
        } else {
            GLStateManager.disableLighting();
        }
        if (state.blend) {
            GLStateManager.enableBlend();
        } else {
            GLStateManager.disableBlend();
        }
        GLStateManager.tryBlendFuncSeparate(state.blendSrcRgb, state.blendDstRgb, state.blendSrcAlpha, state.blendDstAlpha);
        if (state.depthTest) {
            GLStateManager.enableDepthTest();
        } else {
            GLStateManager.disableDepthTest();
        }
        GLStateManager.glDepthFunc(state.depthFunc);
        GLStateManager.glDepthMask(state.depthMask);
        if (state.cull) {
            GLStateManager.enableCull();
        } else {
            GLStateManager.disableCull();
        }
        GLStateManager.glCullFace(state.cullMode);
        if (state.rescaleNormal) {
            GLStateManager.enableRescaleNormal();
        } else {
            GLStateManager.disableRescaleNormal();
        }
    }

    @Unique
    private static final class Saved {
        boolean alphaTest;
        int alphaFunc;
        float alphaRef;
        boolean lighting;
        boolean blend;
        int blendSrcRgb;
        int blendDstRgb;
        int blendSrcAlpha;
        int blendDstAlpha;
        boolean depthTest;
        int depthFunc;
        boolean depthMask;
        boolean cull;
        int cullMode;
        boolean rescaleNormal;
    }
}
