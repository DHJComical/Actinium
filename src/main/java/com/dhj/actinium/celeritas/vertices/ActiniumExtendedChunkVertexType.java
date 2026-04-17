package com.dhj.actinium.celeritas.vertices;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.HashMap;
import java.util.Map;

public class ActiniumExtendedChunkVertexType implements ChunkVertexType {
    public static final ChunkVertexType BASE_TYPE = ChunkMeshFormats.VANILLA_LIKE;
    public static final int STRIDE = 48;
    public static final float MID_TEX_SCALE = 1.0f / 32768.0f;

    public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(STRIDE)
            .addAllElements(BASE_TYPE.getVertexFormat())
            .addElement("mc_midTexCoord", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
            .addElement("at_tangent", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, true, false)
            .addElement("iris_Normal", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 3, true, false)
            .addElement("mc_Entity", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .addElement("at_midBlock", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, false, false)
            .build();

    public static int encodeMidTexture(float u, float v) {
        return (Math.round(u * 32768.0f) & 0xFFFF)
                | ((Math.round(v * 32768.0f) & 0xFFFF) << 16);
    }

    @Override
    public ChunkVertexEncoder createEncoder() {
        return new ActiniumExtendedChunkVertexEncoder();
    }

    @Override
    public float getPositionScale() {
        return BASE_TYPE.getPositionScale();
    }

    @Override
    public float getPositionOffset() {
        return BASE_TYPE.getPositionOffset();
    }

    @Override
    public float getTextureScale() {
        return BASE_TYPE.getTextureScale();
    }

    @Override
    public GlVertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public Map<String, String> getDefines() {
        Map<String, String> defines = new HashMap<>(ChunkVertexType.super.getDefines());
        defines.put("ACTINIUM_EXTENDED_VERTEX_FORMAT", "");
        defines.put("ACTINIUM_MID_TEX_SCALE", String.valueOf(MID_TEX_SCALE));
        return defines;
    }
}
