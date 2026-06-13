package org.taumc.celeritas.mixin.fontrenderer;

import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;
import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.mixins.interfaces.FontRendererAccessor;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer implements FontRendererAccessor, IFontParameters {
    @Shadow private int[] colorCode;
    @Shadow private float alpha;
    @Shadow private float red;
    @Shadow private float blue;
    @Shadow private float green;
    @Shadow protected int[] charWidth;
    @Shadow private boolean unicodeFlag;
    @Shadow protected float posX;
    @Shadow protected float posY;
    @Shadow @Final protected ResourceLocation locationFontTexture;
    @Shadow private boolean bidiFlag;

    @Shadow protected abstract String bidiReorder(String text);
    @Shadow protected abstract void bindTexture(ResourceLocation location);

    @Unique private BatchingFontRenderer actinium$batcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void actinium$injectBatcher(GameSettings settings, ResourceLocation fontLocation, TextureManager texManager,
        boolean unicodeMode, CallbackInfo ci) {
        actinium$batcher = new BatchingFontRenderer((FontRenderer) (Object) this, this.charWidth, this.colorCode, this.locationFontTexture, texManager);
    }

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void actinium$drawStringBatched(String text, float x, float y, int argb, boolean dropShadow,
        CallbackInfoReturnable<Integer> cir) {
        if (GLStateManager.getListMode() == 0) {
            cir.setReturnValue(angelica$drawStringBatched(text, (int) x, (int) y, argb, dropShadow));
        }
    }

    @Inject(method = "renderString", at = @At("HEAD"), cancellable = true)
    private void actinium$renderStringBatched(String text, float x, float y, int argb, boolean dropShadow,
        CallbackInfoReturnable<Integer> cir) {
        if (GLStateManager.getListMode() == 0) {
            cir.setReturnValue(angelica$drawStringBatched(text, (int) x, (int) y, argb, dropShadow));
        }
    }

    @Override
    public int angelica$drawStringBatched(String text, int x, int y, int argb, boolean dropShadow) {
        if (text == null) {
            return 0;
        }
        if (this.bidiFlag) {
            text = this.bidiReorder(text);
        }
        if ((argb & 0xfc000000) == 0) {
            argb |= 0xff000000;
        }

        this.red = (argb >> 16 & 255) / 255.0F;
        this.blue = (argb >> 8 & 255) / 255.0F;
        this.green = (argb & 255) / 255.0F;
        this.alpha = (argb >> 24 & 255) / 255.0F;
        GLStateManager.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.posX = x;
        this.posY = y;
        return (int) actinium$batcher.drawString(x, y, argb, dropShadow, unicodeFlag, text, 0, text.length());
    }

    @Override
    public BatchingFontRenderer angelica$getBatcher() {
        return actinium$batcher;
    }

    @Override
    public void angelica$bindTexture(ResourceLocation location) {
        this.bindTexture(location);
    }

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    private void actinium$getCharWidth(char c, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) angelica$getBatcher().getCharWidthFine(c));
    }

    @Override public float actinium$getGlyphScaleX() { return angelica$getBatcher().getGlyphScaleX(); }
    @Override public float actinium$getGlyphScaleY() { return angelica$getBatcher().getGlyphScaleY(); }
    @Override public float actinium$getGlyphSpacing() { return angelica$getBatcher().getGlyphSpacing(); }
    @Override public float actinium$getWhitespaceScale() { return angelica$getBatcher().getWhitespaceScale(); }
    @Override public float actinium$getShadowOffset() { return angelica$getBatcher().getShadowOffset(); }
    @Override public float actinium$getCharWidthFine(char chr) { return angelica$getBatcher().getCharWidthFine(chr); }
}
