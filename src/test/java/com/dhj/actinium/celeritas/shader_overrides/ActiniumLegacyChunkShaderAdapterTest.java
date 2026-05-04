package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ActiniumLegacyChunkShaderAdapterTest {
    @Test
    void translateKeepsMidTexCoordAsVec4() {
        String source = """
                #version 120
                attribute vec4 at_tangent;
                attribute vec4 mc_Entity;
                attribute vec4 mc_midTexCoord;
                varying vec2 v_TexCoord;
                void main() {
                    v_TexCoord = mc_midTexCoord.xy;
                    gl_Position = ftransform();
                }
                """;

        String translated = ActiniumLegacyChunkShaderAdapter.translate(ShaderType.VERTEX, ActiniumTerrainPass.GBUFFER_SOLID, source, 0);

        assertTrue(translated.contains("vec4 actinium_mc_midTexCoord"), "mc_midTexCoord should be widened to vec4");
        assertTrue(translated.contains("actinium_mc_midTexCoord = vec4(actinium_MidTexCoord, 0.0, 1.0);"), "mid tex assignment should expand to vec4");
        assertTrue(translated.contains("void actinium_pack_main()"), "main should be renamed");
        assertFalse(translated.contains("attribute "), "legacy attributes should be upgraded");
        assertFalse(translated.contains("varying "), "legacy varyings should be upgraded");
    }

    @Test
    void translateFragmentsRenameLegacyColorOutputs() {
        String source = """
                #version 120
                varying vec2 v_TexCoord;
                void main() {
                    gl_FragColor = vec4(v_TexCoord, 0.0, 1.0);
                }
                """;

        String translated = ActiniumLegacyChunkShaderAdapter.translate(ShaderType.FRAGMENT, ActiniumTerrainPass.GBUFFER_TRANSLUCENT, source, 0);

        assertTrue(translated.contains("out vec4 fragColor0"), "fragment output should be upgraded");
        assertTrue(translated.contains("void actinium_pack_main()"), "main should be renamed");
    }
}
