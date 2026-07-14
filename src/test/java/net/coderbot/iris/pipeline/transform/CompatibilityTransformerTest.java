package net.coderbot.iris.pipeline.transform;

import com.gtnewhorizons.angelica.glsm.GlslTransformUtils;
import net.coderbot.iris.gbuffer_overrides.matching.InputAvailability;
import net.coderbot.iris.pipeline.transform.parameter.AttributeParameters;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatibilityTransformerTest {
    @Test
    void groupedTransformAddsParseableOutputsForMissingFragmentInputs() {
        String vertex = """
            #version 330 core
            void main() {
                gl_Position = vec4(0.0);
            }
            """;
        String fragment = """
            #version 330 core
            in vec3 color;
            in vec2 texCoord;
            flat in float isMoon;
            layout(location = 0) out vec4 fragmentColor;
            void main() {
                fragmentColor = vec4(color * (texCoord.x + isMoon), 1.0);
            }
            """;

        EnumMap<PatchShaderType, Transformer> stages = new EnumMap<>(PatchShaderType.class);
        Transformer vertexTransformer = transformer(vertex);
        stages.put(PatchShaderType.VERTEX, vertexTransformer);
        stages.put(PatchShaderType.FRAGMENT, transformer(fragment));

        CompatibilityTransformer.transformGrouped(
            stages,
            new AttributeParameters(Patch.ATTRIBUTES, false, new InputAvailability(true, false, false))
        );

        String transformedVertex = format(vertexTransformer);
        Transformer reparsedVertex = transformer(transformedVertex);
        Map<String, GLSLParser.Single_declarationContext> outputs = reparsedVertex.findQualifiers(GLSLLexer.OUT);

        assertEquals(Set.of("color", "texCoord", "isMoon"), outputs.keySet());
        assertEquals("vec3", typeOf(outputs.get("color")));
        assertEquals("vec2", typeOf(outputs.get("texCoord")));
        assertEquals("float", typeOf(outputs.get("isMoon")));
        assertEquals("flatoutfloat", outputs.get("isMoon").fully_specified_type().getText());
    }

    private static Transformer transformer(String source) {
        return new Transformer(ShaderParser.parseShader(source).full());
    }

    private static String format(Transformer transformer) {
        StringBuilder output = new StringBuilder();
        transformer.mutateTree(tree -> output.append(
            GlslTransformUtils.getFormattedShader(tree, "#version 330 core\n")
        ));
        return output.toString();
    }

    private static String typeOf(GLSLParser.Single_declarationContext declaration) {
        return declaration.fully_specified_type().type_specifier().type_specifier_nonarray().getText();
    }
}
