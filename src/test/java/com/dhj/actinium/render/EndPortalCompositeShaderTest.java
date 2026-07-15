package com.dhj.actinium.render;

import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EndPortalCompositeShaderTest {
    @Test
    void compositorShadersParseAsCompleteGlslTranslationUnits() {
        assertNotNull(ShaderParser.parseShader(
            ShaderLoader.getShaderSource("actinium:end_portal_composite.vert")
        ).full());
        assertNotNull(ShaderParser.parseShader(
            ShaderLoader.getShaderSource("actinium:end_portal_composite.frag")
        ).full());
    }
}
