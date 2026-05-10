package com.dhj.actinium.mixin.features.iris.startup;

import net.coderbot.iris.texture.pbr.PBRAtlasHolder;
import net.coderbot.iris.texture.pbr.TextureAtlasExtension;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureMap.class)
public abstract class TextureMapIrisMixin extends AbstractTexture implements TextureAtlasExtension {
    @Unique
    private PBRAtlasHolder actinium$pbrHolder;

    @Inject(method = "updateAnimations()V", at = @At("TAIL"))
    private void actinium$cyclePbrAnimationFrames(CallbackInfo ci) {
        if (this.actinium$pbrHolder != null) {
            this.actinium$pbrHolder.cycleAnimationFrames();
        }
    }

    @Override
    public PBRAtlasHolder getPBRHolder() {
        return this.actinium$pbrHolder;
    }

    @Override
    public PBRAtlasHolder getOrCreatePBRHolder() {
        if (this.actinium$pbrHolder == null) {
            this.actinium$pbrHolder = new PBRAtlasHolder();
        }

        return this.actinium$pbrHolder;
    }
}
