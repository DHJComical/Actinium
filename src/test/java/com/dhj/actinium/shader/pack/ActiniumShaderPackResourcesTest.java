package com.dhj.actinium.shader.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ActiniumShaderPackResourcesTest {
    @Test
    void parseDrawBuffersPrefersLastDirective() {
        String shader = """
                #version 120
                /* DRAWBUFFERS:01 */
                void main() {}
                /* RENDERTARGETS:234 */
                """;

        assertArrayEquals(new int[]{2, 3, 4}, ActiniumShaderPackResources.parseDrawBuffers(shader));
    }

    @Test
    void parseDrawBuffersDefaultsToColortex1() {
        assertArrayEquals(new int[]{1}, ActiniumShaderPackResources.parseDrawBuffers("#version 120\nvoid main() {}"));
    }
}
