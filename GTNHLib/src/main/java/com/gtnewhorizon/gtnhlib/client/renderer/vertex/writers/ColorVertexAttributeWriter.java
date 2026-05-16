package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;

public final class ColorVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | COLOR_INDEX]);
        return 4;
    }

    @Override
    public int writeAttribute(long pointer, DirectTessellator tessellator) {
        memPutInt(pointer, tessellator.getPackedColor());
        return 4;
    }

    @Override
    public int readAttribute(long pointer, DirectTessellator tessellator) {
        tessellator.setPackedColorRaw(memGetInt(pointer));
        return 4;
    }
}
