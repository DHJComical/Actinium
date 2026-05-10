package com.gtnewhorizons.angelica.glsm.texture;

import org.lwjgl.opengl.GL11;

public class TextureInfo {
    private final int id;
    int internalFormat = -1;
    int width = -1;
    int height = -1;

    TextureInfo(int id) {
        this.id = id;
    }

    public int getInternalFormat() {
        if (this.internalFormat == -1) {
            this.internalFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
        }
        return this.internalFormat;
    }

    public int getWidth() {
        if (this.width == -1) {
            this.width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        }
        return this.width;
    }

    public int getHeight() {
        if (this.height == -1) {
            this.height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        }
        return this.height;
    }
}
