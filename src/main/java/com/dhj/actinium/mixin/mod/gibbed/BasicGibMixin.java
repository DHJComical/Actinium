package com.dhj.actinium.mixin.mod.gibbed;

import com.dhj.actinium.compat.gibbed.ActiniumModelRenderer;
import com.dhj.actinium.debug.ActiniumDiagnostics;
import fonnymunkey.gibbed.util.IModelRenderer;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "fonnymunkey.gibbed.client.gib.BasicGib", remap = false)
public abstract class BasicGibMixin {
    @Redirect(
        method = "render(Lfonnymunkey/gibbed/client/gib/RenderGib;Lfonnymunkey/gibbed/client/gib/EntityGib;DDDF[F)V",
        at = @At(
            value = "INVOKE",
            target = "Lfonnymunkey/gibbed/util/IModelRenderer;gibbed$renderSingular(F)V"
        )
    )
    private void actinium$renderSingleGibPart(IModelRenderer renderer, float scale) {
        if (renderer instanceof ActiniumModelRenderer) {
            ActiniumDiagnostics.recordGibbedRenderPath("single-immediate");
            ((ActiniumModelRenderer) renderer).actinium$renderGibbedSingleModelPart(scale);
            return;
        }

        ActiniumDiagnostics.recordGibbedRenderPath("single-fallback");
        renderer.gibbed$renderSingular(scale);
    }

    @Redirect(
        method = "render(Lfonnymunkey/gibbed/client/gib/RenderGib;Lfonnymunkey/gibbed/client/gib/EntityGib;DDDF[F)V",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V",
            remap = true
        )
    )
    private void actinium$renderGibPartWithChildren(ModelRenderer renderer, float scale) {
        if (renderer instanceof ActiniumModelRenderer) {
            ActiniumDiagnostics.recordGibbedRenderPath("full-immediate");
            ((ActiniumModelRenderer) renderer).actinium$renderGibbedModel(scale);
            return;
        }

        ActiniumDiagnostics.recordGibbedRenderPath("full-fallback");
        renderer.render(scale);
    }
}
