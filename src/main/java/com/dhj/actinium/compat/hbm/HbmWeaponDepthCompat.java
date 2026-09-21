package com.dhj.actinium.compat.hbm;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.pipeline.HandRenderer;

/**
 * Keeps HBM's first-person weapon depth handling out of Actinium's shader-pipeline hand pass.
 *
 * <p>HBM's Sedna weapon renderers ({@code com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase})
 * choose their hand-depth strategy from OptiFine's shader state. With a shader pack loaded they ask
 * OptiFine for the vanilla hand-depth treatment; without one they clear the depth buffer so the held
 * weapon can never be clipped by world geometry. That probe goes through
 * {@code com.hbm.util.ShaderHelper}, which binds {@code net.optifine.shaders.Shaders} by name and
 * resolves to a no-op unless the OptiFine class exists. Actinium implements the shader pipeline
 * itself and ships no OptiFine class, so the probe is always negative here and the renderer takes
 * the "no shaders" branch.</p>
 *
 * <p>Taking that branch breaks the frame. {@code setPerspectiveAndRender} runs inside
 * {@link HandRenderer}, which renders the first-person hand against the world depth that the rest of
 * the pipeline reads back, so clearing the depth buffer there discards the world depth for every
 * depth-reading effect that follows. Replacing the clear with the projection compression OptiFine's
 * hand-depth treatment performs keeps the held weapon unclipped without touching the depth
 * contents.</p>
 */
public final class HbmWeaponDepthCompat {
    private HbmWeaponDepthCompat() {
    }

    /**
     * Returns whether HBM must leave the depth buffer alone in the current frame.
     *
     * <p>A loaded shader pack means the depth buffer carries world depth that the shader pipeline
     * reads back, so HBM's depth clear has to be suppressed. Without a shader pack the clear is the
     * renderer's intended way of keeping the held weapon unclipped, and stays in place.</p>
     */
    public static boolean shouldSkipWeaponDepthClear() {
        return IrisApiV0Impl.INSTANCE.isShaderPackInUse();
    }

    /**
     * Applies the hand-depth projection compression HBM expects from OptiFine.
     *
     * <p>HBM calls this between loading an identity projection and rebuilding the hand projection,
     * so the compression multiplies into the weapon projection exactly as it does under OptiFine.
     * The scale is the one {@link HandRenderer} uses for every other first-person item, which keeps
     * the weapon at the same depth as the rest of the hand pass instead of letting geometry clip
     * it.</p>
     */
    public static void applyShaderHandDepth() {
        GLStateManager.glScalef(1.0F, 1.0F, HandRenderer.DEPTH);
    }
}
