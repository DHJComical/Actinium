package com.dhj.actinium.render;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

import java.nio.ByteBuffer;

/**
 * Validates and writes the raw UV4 layout used by projective portal vertices.
 */
public final class ProjectiveTexCoordWriter {
    private ProjectiveTexCoordWriter() {
    }

    /**
     * Writes a homogeneous primary UV element at the active BufferBuilder vertex and format index.
     *
     * @param buffer target raw vertex buffer
     * @param format active vertex format
     * @param formatIndex active element index
     * @param vertexCount active vertex index
     * @param s homogeneous S coordinate
     * @param t homogeneous T coordinate
     * @param r homogeneous R coordinate
     * @param q homogeneous Q coordinate
     */
    public static void write(
        ByteBuffer buffer,
        VertexFormat format,
        int formatIndex,
        int vertexCount,
        float s,
        float t,
        float r,
        float q
    ) {
        VertexFormatElement element = format.getElement(formatIndex);
        if (element.getUsage() != VertexFormatElement.EnumUsage.UV
            || element.getIndex() != 0
            || element.getType() != VertexFormatElement.EnumType.FLOAT
            || element.getElementCount() != 4) {
            throw new IllegalStateException("Expected primary UV FLOAT[4], got " + element);
        }

        int offset = vertexCount * format.getSize() + format.getOffset(formatIndex);
        buffer.putFloat(offset, s);
        buffer.putFloat(offset + Float.BYTES, t);
        buffer.putFloat(offset + Float.BYTES * 2, r);
        buffer.putFloat(offset + Float.BYTES * 3, q);
    }
}
