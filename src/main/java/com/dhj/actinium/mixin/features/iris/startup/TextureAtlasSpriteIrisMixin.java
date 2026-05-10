package com.dhj.actinium.mixin.features.iris.startup;

import net.coderbot.iris.texture.pbr.PBRSpriteHolder;
import net.coderbot.iris.texture.pbr.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteIrisMixin implements TextureAtlasSpriteExtension {
    @Unique
    private PBRSpriteHolder actinium$pbrHolder;

    @Override
    public PBRSpriteHolder getPBRHolder() {
        return this.actinium$pbrHolder;
    }

    @Override
    public PBRSpriteHolder getOrCreatePBRHolder() {
        if (this.actinium$pbrHolder == null) {
            this.actinium$pbrHolder = new PBRSpriteHolder();
        }

        return this.actinium$pbrHolder;
    }
}
