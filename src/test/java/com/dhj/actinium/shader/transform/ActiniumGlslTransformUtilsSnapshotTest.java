package com.dhj.actinium.shader.transform;

import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiniumGlslTransformUtilsSnapshotTest {
    @Test
    void snapshotChunkVertexSnippet() throws IOException {
        String source = readResource("actinium/shader/transform/chunk_vertex_input.glsl");
        String formatted = ActiniumGlslTransformUtils.getFormattedShader(ShaderParser.parseShader(source).full(), "#version 330 core\n");
        assertEquals(readResource("actinium/shader/transform/chunk_vertex_formatted.snapshot"), formatted);
    }

    @Test
    void snapshotTextureReplacement() throws IOException {
        String input = readResource("actinium/shader/transform/texture_replace_input.txt");
        assertEquals(readResource("actinium/shader/transform/texture_replace.snapshot"), ActiniumGlslTransformUtils.replaceTexture(input));
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = ActiniumGlslTransformUtilsSnapshotTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + path);
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
