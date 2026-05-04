package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.junit.jupiter.api.Test;

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
        assertTrue(translated.contains("void actinium_pack_main()"), "main should be renamed");
    }
}
