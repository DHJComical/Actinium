package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;

public final class LightVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | LIGHT_INDEX]);
        return 4;
    }

    @Override
    public int writeAttribute(long pointer, DirectTessellator tessellator) {
        memPutInt(pointer, tessellator.getPackedBrightness());
        return 4;
    }

    @Override
    public int readAttribute(long pointer, DirectTessellator tessellator) {
        tessellator.setPackedBrightnessRaw(memGetInt(pointer));
        return 4;
    }
}
