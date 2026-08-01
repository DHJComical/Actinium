package net.coderbot.iris.shaderpack;

import net.coderbot.iris.gl.texture.InternalTextureFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackRenderTargetDirectivesTest {
    @Test
    void defaultsColortex5ToHdrHistoryFormat() {
        PackRenderTargetDirectives directives = new PackRenderTargetDirectives(
                PackRenderTargetDirectives.BASELINE_SUPPORTED_RENDER_TARGETS);

        assertEquals(
                InternalTextureFormat.RGBA16F,
                directives.getRenderTargetSettings().get(5).getInternalFormat());
    }
}
