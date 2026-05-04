package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(translated.contains("v_TexCoord = actinium_mc_midTexCoord . xy ;"), "mid tex references should still resolve through the widened vec4");
        assertTrue(translated.contains("void actinium_pack_main()"), "main should be renamed");
        assertFalse(translated.contains("attribute "), "legacy attributes should be upgraded");
        assertFalse(translated.contains("varying "), "legacy varyings should be upgraded");
    }

    @Test
    void translatePromotesVec2MidTexCoordToCompatVec4() {
        String source = """
                #version 120
                attribute vec2 mc_midTexCoord;
                varying vec2 v_TexCoord;
                void main() {
                    v_TexCoord = mc_midTexCoord;
                    gl_Position = ftransform();
                }
                """;

        String translated = ActiniumLegacyChunkShaderAdapter.translate(ShaderType.VERTEX, ActiniumTerrainPass.GBUFFER_SOLID, source, 0);

        assertTrue(translated.contains("vec4 actinium_mc_midTexCoord"), "compat declaration should always be vec4");
        assertTrue(translated.contains("actinium_mc_midTexCoord = vec4(actinium_MidTexCoord, 0.0, 1.0);"), "compat assignment should always rebuild a vec4");
        assertTrue(translated.contains("v_TexCoord = actinium_mc_midTexCoord ;") || translated.contains("v_TexCoord = actinium_mc_midTexCoord . xy ;"),
                "vec2 inputs should continue to compile against the compat mid tex value");
    }

    @Test
    void translatePreservesLegacyMultiTexCoord3AliasWhenUsed() {
        String source = """
                #version 120
                varying vec2 v_TexCoord;
                void main() {
                    v_TexCoord = gl_MultiTexCoord3.xy;
                    gl_Position = ftransform();
                }
                """;

        String translated = ActiniumLegacyChunkShaderAdapter.translate(ShaderType.VERTEX, ActiniumTerrainPass.GBUFFER_SOLID, source, 0);

        assertFalse(translated.contains("gl_MultiTexCoord3"), "legacy mid tex alias should not leak through translation");
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
