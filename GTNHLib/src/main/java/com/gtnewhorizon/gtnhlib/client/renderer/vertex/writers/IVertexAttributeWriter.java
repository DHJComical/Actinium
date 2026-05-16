package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;

public interface IVertexAttributeWriter {

    int writeAttribute(long pointer, int[] data, int index);

    int writeAttribute(long pointer, DirectTessellator tessellator);

    // Populate the Tessellator using the buffer's contents
    int readAttribute(long pointer, DirectTessellator tessellator);

    default int writeAttributeTransformed(long pointer, int[] data, int index, Matrix4fc transform, Vector3f scratch) {
        return writeAttribute(pointer, data, index);
    }
}
