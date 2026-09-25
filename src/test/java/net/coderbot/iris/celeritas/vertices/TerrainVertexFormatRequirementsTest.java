package net.coderbot.iris.celeritas.vertices;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainVertexFormatRequirementsTest {
    @Test
    void unionsOnlyAttributesReadByAnyTerrainOrShadowVertexProgram() {
        TerrainVertexFormatRequirements requirements = TerrainVertexFormatRequirements.analyze(List.of(
            "in vec3 iris_Normal; void main() { gl_Position = vec4(0.0); }",
            "in uint mc_Entity; in vec2 mc_midTexCoord; in vec4 at_midBlock; void main() { uint id = mc_Entity; vec2 mid = mc_midTexCoord; vec4 block = at_midBlock; gl_Position = vec4(float(id) + mid.x + block.x); }",
            "in vec3 iris_Normal; in vec4 at_tangent; void main() { vec3 normal = iris_Normal; vec4 tangent = at_tangent; gl_Position = vec4(normal + tangent.xyz, 1.0); }"
        ));

        assertTrue(requirements.requires(TerrainVertexFormatRequirements.Attribute.MC_ENTITY));
        assertTrue(requirements.requires(TerrainVertexFormatRequirements.Attribute.MID_TEX_COORD));
        assertTrue(requirements.requires(TerrainVertexFormatRequirements.Attribute.TANGENT));
        assertTrue(requirements.requires(TerrainVertexFormatRequirements.Attribute.MID_BLOCK));
        assertTrue(requirements.requires(TerrainVertexFormatRequirements.Attribute.NORMAL));
    }

    @Test
    void ignoresDeclarationOnlyNormalInjectedByCeleritasTransformer() {
        TerrainVertexFormatRequirements requirements = TerrainVertexFormatRequirements.analyze(List.of(
            "in vec3 iris_Normal; void main() { gl_Position = vec4(0.0); }"
        ));

        assertFalse(requirements.requires(TerrainVertexFormatRequirements.Attribute.NORMAL));
        ExtendedChunkVertexType vertexType = new ExtendedChunkVertexType(requirements);
        assertEquals(28, vertexType.getVertexFormat().getStride());
        assertFalse(vertexType.getVertexFormat().getAttributes().stream()
                .anyMatch(attribute -> attribute.getName().equals("a_RdhFactor")));
        assertFalse(vertexType.createEncoder().supportsBilinearCorrection());
    }

    @Test
    void keepsCompleteFormatWhenTransformedSourceIsUnavailable() {
        TerrainVertexFormatRequirements requirements = TerrainVertexFormatRequirements.analyze(List.of(""));

        for (TerrainVertexFormatRequirements.Attribute attribute : TerrainVertexFormatRequirements.Attribute.values()) {
            assertTrue(requirements.requires(attribute));
        }
        assertEquals(48, new ExtendedChunkVertexType(requirements).getVertexFormat().getStride());
    }

    @Test
    void buildsOnlyRequiredAttributesWithAlignedStride() {
        TerrainVertexFormatRequirements requirements = TerrainVertexFormatRequirements.of(
            TerrainVertexFormatRequirements.Attribute.MID_TEX_COORD,
            TerrainVertexFormatRequirements.Attribute.NORMAL
        );
        ExtendedChunkVertexType vertexType = new ExtendedChunkVertexType(requirements);

        assertEquals(36, vertexType.getVertexFormat().getStride());
        assertEquals(28, vertexType.getVertexFormat().getAttribute("mc_midTexCoord").getPointer());
        assertEquals(32, vertexType.getVertexFormat().getAttribute("iris_Normal").getPointer());
    }
}
