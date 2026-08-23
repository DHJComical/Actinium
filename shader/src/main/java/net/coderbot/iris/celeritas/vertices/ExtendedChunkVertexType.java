package net.coderbot.iris.celeritas.vertices;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Objects;

public class ExtendedChunkVertexType implements ChunkVertexType {
    private static final String RDH_FACTOR_ATTRIBUTE = "a_RdhFactor";
    public static final ChunkVertexType BASE_TYPE = ChunkMeshFormats.VANILLA_LIKE;
    public static final float MID_TEX_SCALE = 1.0f / 32768.0f;

    private final TerrainVertexFormatRequirements requirements;
    private final GlVertexFormat vertexFormat;

    public ExtendedChunkVertexType() {
        this(TerrainVertexFormatRequirements.all());
    }

    public ExtendedChunkVertexType(TerrainVertexFormatRequirements requirements) {
        this.requirements = Objects.requireNonNull(requirements, "Vertex format requirements must not be null");
        this.vertexFormat = createVertexFormat(this.requirements);
    }

    public static int encodeMidTexture(float u, float v) {
        return (Math.round(u * 32768.0f) & 0xFFFF)
                | ((Math.round(v * 32768.0f) & 0xFFFF) << 16);
    }

    @Override
    public ChunkVertexEncoder createEncoder() {
        return new ExtendedChunkVertexEncoder(this);
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
        return this.vertexFormat;
    }

    public boolean requires(TerrainVertexFormatRequirements.Attribute attribute) {
        return this.requirements.requires(attribute);
    }

    private static GlVertexFormat createVertexFormat(TerrainVertexFormatRequirements requirements) {
        int stride = baseVertexStrideWithoutBilinearCorrection();
        for (TerrainVertexFormatRequirements.Attribute attribute : TerrainVertexFormatRequirements.Attribute.values()) {
            if (requirements.requires(attribute)) {
                stride += attributeSize(attribute);
            }
        }
        stride = (stride + 3) & ~3;

        GlVertexFormat.Builder builder = GlVertexFormat.builder(stride)
                .addElements(BASE_TYPE.getVertexFormat().getAttributes().stream()
                        .filter(attribute -> !RDH_FACTOR_ATTRIBUTE.equals(attribute.getName()))
                        .toList());
        if (requirements.requires(TerrainVertexFormatRequirements.Attribute.MID_TEX_COORD)) {
            builder.addElement("mc_midTexCoord", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false);
        }
        if (requirements.requires(TerrainVertexFormatRequirements.Attribute.TANGENT)) {
            builder.addElement("at_tangent", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, true, false);
        }
        if (requirements.requires(TerrainVertexFormatRequirements.Attribute.NORMAL)) {
            builder.addElement("iris_Normal", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 3, true, false);
        }
        if (requirements.requires(TerrainVertexFormatRequirements.Attribute.MC_ENTITY)) {
            builder.addElement("mc_Entity", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true);
        }
        if (requirements.requires(TerrainVertexFormatRequirements.Attribute.MID_BLOCK)) {
            builder.addElement("at_midBlock", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, false, false);
        }
        return builder.build();
    }

    private static int baseVertexStrideWithoutBilinearCorrection() {
        return BASE_TYPE.getVertexFormat().getAttributes().stream()
                .filter(attribute -> !RDH_FACTOR_ATTRIBUTE.equals(attribute.getName()))
                .mapToInt(attribute -> attribute.getPointer() + attribute.getSize())
                .max()
                .orElseThrow(() -> new IllegalStateException("Base terrain vertex format has no usable attributes"));
    }

    private static int attributeSize(TerrainVertexFormatRequirements.Attribute attribute) {
        return switch (attribute) {
            case MC_ENTITY, MID_TEX_COORD, TANGENT, MID_BLOCK -> 4;
            case NORMAL -> 3;
        };
    }
}
