package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;

public final class TextureVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | TEX_X_INDEX]);
        memPutInt(pointer + 4, data[index | TEX_Y_INDEX]);
        return 8;
    }

    @Override
    public int writeAttribute(long pointer, DirectTessellator tessellator) {
        memPutFloat(pointer, (float) tessellator.getLastTextureU());
        memPutFloat(pointer + 4, (float) tessellator.getLastTextureV());
        return 8;
    }

    @Override
    public int readAttribute(long pointer, DirectTessellator tessellator) {
        tessellator.setLastTextureUVRaw(memGetFloat(pointer), memGetFloat(pointer + 4));
        return 8;
    }
}
