package com.dhj.actinium.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectiveTexCoordWriterTest {
    private static final VertexFormatElement PROJECTIVE_UV = new VertexFormatElement(
        0,
        VertexFormatElement.EnumType.FLOAT,
        VertexFormatElement.EnumUsage.UV,
        4
    );

    @Test
    void writesFourFloatsIntoAnActualBufferBuilderVertexLayout() {
        VertexFormat format = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(PROJECTIVE_UV)
            .addElement(DefaultVertexFormats.TEX_2S)
            .addElement(DefaultVertexFormats.NORMAL_3B)
            .addElement(DefaultVertexFormats.PADDING_1B);
        BufferBuilder builder = new BufferBuilder(64);
        builder.begin(7, format);
        builder.pos(1.0D, 2.0D, 3.0D).color(0.1F, 0.2F, 0.3F, 1.0F);

        ProjectiveTexCoordWriter.write(builder.getByteBuffer(), format, 2, 0, 4.25F, -3.5F, 8.0F, 2.0F);

        ByteBuffer raw = builder.getByteBuffer();
        int offset = format.getOffset(2);
        assertEquals(4.25F, raw.getFloat(offset));
        assertEquals(-3.5F, raw.getFloat(offset + Float.BYTES));
        assertEquals(8.0F, raw.getFloat(offset + Float.BYTES * 2));
        assertEquals(2.0F, raw.getFloat(offset + Float.BYTES * 3));
    }

    @Test
    void rejectsTheOrdinaryTwoComponentUvLayout() {
        VertexFormat format = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.TEX_2F);
        BufferBuilder builder = new BufferBuilder(32);
        builder.begin(7, format);
        builder.pos(0.0D, 0.0D, 0.0D);

        assertThrows(
            IllegalStateException.class,
            () -> ProjectiveTexCoordWriter.write(builder.getByteBuffer(), format, 1, 0, 1.0F, 2.0F, 3.0F, 4.0F)
        );
    }
}
