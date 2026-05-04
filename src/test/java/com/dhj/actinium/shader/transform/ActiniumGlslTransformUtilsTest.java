package com.dhj.actinium.shader.transform;

import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiniumGlslTransformUtilsTest {
    @Test
    void replaceTextureLeavesTextureCallsitesUntouched() {
        String input = "texture2D(tex, uv) + texture2DLod(tex, uv, 0.0) + texture(tex, uv) + nottexture(tex)";

        String output = ActiniumGlslTransformUtils.replaceTexture(input);

        assertEquals(input, output);
    }

    @Test
    void restoreReservedWordsRestoresTextureIdentifier() {
        assertEquals("texture(texture(texture, uv))", ActiniumGlslTransformUtils.restoreReservedWords("actinium_renamed_texture(texture(actinium_renamed_texture, uv))"));
    }

    @Test
    void formatShaderUsesHeaderAndParsedTree() {
        String source = """
                #version 120
                void main() {
                    gl_Position = vec4(1.0);
                }
                """;

        String result = ActiniumGlslTransformUtils.getFormattedShader(ShaderParser.parseShader(source).full(), "#version 330 core\n");

        assertTrue(result.startsWith("#version 330 core\n"));
        assertTrue(result.contains("void main ( ) {"));
        assertTrue(result.contains("gl_Position = vec4 ( 1.0 ) ;"));
    }
}
