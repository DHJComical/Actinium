package com.dhj.actinium.render.terrain.sprite;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.dhj.actinium.texture.SpriteExtension;

public class SpriteUtil {
    public static void markSpriteActive(TextureAtlasSprite sprite) {
        ((SpriteExtension)sprite).celeritas$markActive();
    }
}

