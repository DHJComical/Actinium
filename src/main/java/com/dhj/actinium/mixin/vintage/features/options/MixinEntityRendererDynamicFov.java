package com.dhj.actinium.mixin.vintage.features.options;

import com.dhj.actinium.runtime.ActiniumRuntime;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes vanilla's dynamic field-of-view factor when the user disables it.
 *
 * <p>Vanilla builds the factor in {@code AbstractClientPlayer.getFovModifier} (flight, movement
 * speed, and a bow-draw reduction), smooths it here in {@code updateFovModifierHand}, and applies
 * it to {@code fovSetting} only inside {@code getFOVModifier(float, boolean)} when
 * {@code useFOVSetting} is true. Skipping the smoothing step leaves both hand fields at their
 * initial {@code 1.0F}, so the interpolation at the use site yields exactly {@code 1.0F} and the
 * world/sky FOV stays pinned to {@code fovSetting} — without a multiply-then-divide round trip
 * that could drift by a float ulp.
 *
 * <p>This deliberately keeps the two neighbouring effects alive: the death-camera division and the
 * {@code Material.WATER} scaling inside {@code getFOVModifier} are independent of the dynamic
 * factor, so they keep working while the option is off. Calls made with
 * {@code useFOVSetting == false} (hand rendering, Iris' {@code HandRenderer}) never consumed the
 * factor to begin with.
 *
 * <p>This is the only caller of the method and it runs once per frame from
 * {@code updateRenderer()}, so cancelling it has no other side effect. With the option on, the
 * method is left untouched and the default behaviour is bit-for-bit identical to vanilla.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererDynamicFov {

    @Shadow
    private float fovModifierHand;

    @Shadow
    private float fovModifierHandPrev;

    @Inject(method = "updateFovModifierHand", at = @At("HEAD"), cancellable = true)
    private void actinium$freezeDynamicFov(CallbackInfo ci) {
        if (ActiniumRuntime.options().quality.dynamicFov) {
            return;
        }

        this.fovModifierHand = 1.0F;
        this.fovModifierHandPrev = 1.0F;
        ci.cancel();
    }
}
