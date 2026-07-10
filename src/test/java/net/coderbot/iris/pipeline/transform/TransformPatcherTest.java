package net.coderbot.iris.pipeline.transform;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformPatcherTest {
    @BeforeAll
    static void provideHeadlessGlslCapability() throws ReflectiveOperationException {
        var maxGlslVersion = RenderSystem.class.getDeclaredField("maxGlslVersion");
        maxGlslVersion.setAccessible(true);
        maxGlslVersion.setInt(null, 460);
    }

    @Test
    void compositePatchUpgradesLegacyFragmentOutput() {
        String vertex = """
            #version 120
            varying vec2 texcoord;
            void main() {
                texcoord = gl_MultiTexCoord0.xy;
                gl_Position = ftransform();
            }
            """;
        String fragment = """
            #version 120
            varying vec2 texcoord;
            void main() {
                gl_FragColor = vec4(texcoord, 0.0, 1.0);
            }
            """;

        Map<PatchShaderType, String> patched = TransformPatcher.patchComposite(vertex, null, fragment);

        assertNotNull(patched);
        assertTrue(patched.get(PatchShaderType.VERTEX).contains("void main"));
        String patchedFragment = patched.get(PatchShaderType.FRAGMENT);
        assertTrue(patchedFragment.contains("out vec4 iris_FragData0"), patchedFragment);
        assertTrue(patchedFragment.contains("iris_FragData0 = vec4"), patchedFragment);
    }
}
