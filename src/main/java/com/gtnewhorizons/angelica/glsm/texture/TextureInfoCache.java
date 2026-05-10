package com.gtnewhorizons.angelica.glsm.texture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureInfoCache {
    public static final TextureInfoCache INSTANCE = new TextureInfoCache();

    private final Map<Integer, TextureInfo> cache = new ConcurrentHashMap<>();

    private TextureInfoCache() {
    }

    public TextureInfo getInfo(int textureId) {
        return this.cache.computeIfAbsent(textureId, TextureInfo::new);
    }

    public void putInternalFormat(int textureId, int internalFormat) {
        TextureInfo info = getInfo(textureId);
        info.internalFormat = internalFormat;
    }

    public void onTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, java.nio.Buffer pixels) {
        if (level == 0) {
            TextureInfo info = getInfo(com.gtnewhorizons.angelica.glsm.GLStateManager.getBoundTextureForServerState());
            info.internalFormat = internalFormat;
            info.width = width;
            info.height = height;
        }
    }
}
