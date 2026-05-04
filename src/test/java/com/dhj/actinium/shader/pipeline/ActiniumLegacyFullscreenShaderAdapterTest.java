package com.dhj.actinium.shader.pipeline;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiniumLegacyFullscreenShaderAdapterTest {
    @Test
    void translateVertexWrapsMainAndTexCoords() {
        String source = """
                #version 120
                attribute vec3 a_Position;
                varying vec2 v_TexCoord;
                void main() {
                    v_TexCoord = a_Position.xy;
                    gl_Position = ftransform();
                }
                """;

        String translated = ActiniumLegacyFullscreenShaderAdapter.translate(ShaderType.VERTEX, source);

        assertTrue(translated.contains("void actinium_pack_main()"), "main should be renamed");
        assertTrue(translated.contains("out vec4 actinium_TexCoord0"), "vertex preamble should expose texcoord outputs");
        assertTrue(translated.contains("attribute vec3 a_Position") || translated.contains("in vec3 a_Position"), "legacy input should be preserved or upgraded");
    }

    @Test
    void translateFragmentAddsLegacyOutputs() {
        String source = """
                #version 120
                varying vec2 v_TexCoord;
                void main() {
                    gl_FragColor = vec4(v_TexCoord, 0.0, 1.0);
                }
                """;

        String translated = ActiniumLegacyFullscreenShaderAdapter.translate(ShaderType.FRAGMENT, source);

        assertTrue(translated.contains("out vec4 fragColor0"), "fragment output should be upgraded");
        assertTrue(translated.contains("void actinium_pack_main()"), "main should be renamed");
    }
}
