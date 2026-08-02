package net.coderbot.iris.pipeline.transform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatibilityTransformerCaveSkyholeTest {
    @Test
    void caveSkyholePatchKeepsCloudsVisible() {
        String source = """
            void main() {
                float skyhole = 1.0;
                VolumetricClouds.rgb *= 1.0-skyhole;
                VolumetricClouds.a = mix(VolumetricClouds.a, 1.0, skyhole);
                color = mix(color, cavefog, isSky ? skyhole * caveDetection * caveFactor: 0.0);
            }
            """;

        String patched = CompatibilityTransformer.patchCaveSkyholeClouds(source);

        assertEquals("""
            void main() {
                float skyhole = 1.0;
                VolumetricClouds.rgb *= 1.0;
                VolumetricClouds.a = mix(VolumetricClouds.a, 1.0, 0.0);
                color = mix(color, cavefog, isSky ? 0.0 : 0.0);
            }
            """, patched);
    }
}
