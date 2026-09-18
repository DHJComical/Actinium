package com.dhj.actinium.mixin.mod.obscuretooltips;

import com.dhj.actinium.render.GuiGlStateBoundary;
import dev.obscuria.tooltips.client.component.ArmorPreviewComponent;
import dev.obscuria.tooltips.client.component.TooltipComponent;
import dev.obscuria.tooltips.client.render.GuiGraphics;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.TooltipRenderer", remap = false)
public abstract class MixinTooltipRenderer {
    // Keeps world-render Iris state (armor item ids, enchant glint conditions) out of the
    // armor-stand entity pass that Obscure Tooltips runs inside the tooltip GUI surface.
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ldev/obscuria/tooltips/client/component/TooltipComponent;renderImage(Lnet/minecraft/client/gui/FontRenderer;IILdev/obscuria/tooltips/client/render/GuiGraphics;)V"
        ),
        remap = false
    )
    private static void actinium$renderImageWithEntitySurface(
        TooltipComponent component,
        FontRenderer font,
        int x,
        int y,
        GuiGraphics graphics
    ) {
        if (!(component instanceof ArmorPreviewComponent)) {
            component.renderImage(font, x, y, graphics);
            return;
        }
        GuiGlStateBoundary.EntitySurfaceState surface = GuiGlStateBoundary.beginEntitySurface();
        try {
            component.renderImage(font, x, y, graphics);
        } finally {
            surface.restore();
        }
    }
}
