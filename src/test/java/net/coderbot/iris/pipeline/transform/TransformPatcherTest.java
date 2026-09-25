package net.coderbot.iris.pipeline.transform;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformPatcherTest {
    @BeforeAll
    static void provideHeadlessGlslCapability() {
        RenderSystem.initializeGlslCapabilityForTesting(460);
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

    @Test
    void compositeVertexLegacyGlColorIsRewritten() {
        // Sildur's Enhanced Default reads gl_Color in the composite vertex stage. Under
        // core profile (GLSL >= 330) that builtin does not exist, so it must be renamed
        // to the white-initialized iris_FrontColor the transformer already declares.
        String vertex = """
            #version 120
            varying vec2 texcoord;
            varying vec4 color;
            void main() {
                gl_Position = ftransform();
                texcoord = gl_MultiTexCoord0.xy;
                color = gl_Color;
            }
            """;
        String fragment = """
            #version 120
            varying vec2 texcoord;
            varying vec4 color;
            void main() {
                gl_FragColor = vec4(color.rgb * texcoord.x, 1.0);
            }
            """;

        Map<PatchShaderType, String> patched = TransformPatcher.patchComposite(vertex, null, fragment);

        assertNotNull(patched);
        String patchedVertex = patched.get(PatchShaderType.VERTEX);
        assertTrue(patchedVertex.startsWith("#version 330 core"), patchedVertex);
        // The legacy read must be redirected so it compiles on core profile.
        assertTrue(patchedVertex.replaceAll("\\s+", " ").contains("color = iris_FrontColor ;"), patchedVertex);
        assertTrue(patchedVertex.replaceAll("\\s+", " ").contains("iris_FrontColor = vec4 ( 1.0 ) ;"), patchedVertex);
    }

    @Test
    void terrainVertexGlColorStaysOnCeleritasVertexColor() {
        // Terrain shaders must keep gl_Color routed to Celeritas' _vert_color (the real
        // baked per-vertex color, e.g. grass/foliage tint). CommonTransformer must not
        // intercept it for non-composite patches or every plant turns grey.
        String vertex = """
            #version 120
            varying vec4 color;
            void main() {
                gl_Position = ftransform();
                color = gl_Color;
            }
            """;
        String fragment = """
            #version 120
            varying vec4 color;
            void main() {
                gl_FragColor = vec4(color.rgb, 1.0);
            }
            """;

        Map<PatchShaderType, String> patched = TransformPatcher.patchCeleritasTerrain(vertex, null, fragment);

        assertNotNull(patched);
        String patchedVertex = patched.get(PatchShaderType.VERTEX);
        // The terrain read must be mapped to the baked vertex color, not the composite
        // white front-color.
        assertTrue(patchedVertex.replaceAll("\\s+", " ").contains("color = _vert_color ;"), patchedVertex);
        assertFalse(patchedVertex.replaceAll("\\s+", " ").contains("color = iris_FrontColor"), patchedVertex);
    }
}
