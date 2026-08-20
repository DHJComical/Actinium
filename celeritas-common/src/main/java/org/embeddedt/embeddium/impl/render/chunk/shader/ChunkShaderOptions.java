package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

import java.util.List;

public record ChunkShaderOptions(List<ChunkShaderComponent.Factory<?>> components, TerrainRenderPass pass) {
    private static final String RDH_FACTOR_ATTRIBUTE = "a_RdhFactor";

    public ShaderConstants constants() {
        return this.constants(true);
    }

    /**
     * Builds shader defines, optionally omitting core-only features for the legacy GLSL downgrade path.
     */
    public ShaderConstants constants(boolean includeBilinearCorrection) {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        for (var component : components) {
            constants.addAll(component.getDefines());
        }

        if (this.pass.supportsFragmentDiscard()) {
            constants.add("USE_FRAGMENT_DISCARD");
        }

        if (this.pass.hasNoLightmap()) {
            constants.add("CELERITAS_NO_LIGHTMAP");
        }

        if (includeBilinearCorrection && this.pass.vertexType().getVertexFormat().getAttributes().stream()
                .anyMatch(attribute -> RDH_FACTOR_ATTRIBUTE.equals(attribute.getName()))) {
            constants.add("USE_BILINEAR_CORRECTION");
        }

        constants.addAll(pass.extraDefines());

        var vertexType = pass.vertexType();
        var primitiveType = pass.primitiveType();

        vertexType.getDefines().forEach(constants::add);
        constants.addAll(primitiveType.getDefines());

        return constants.build();
    }
}
