package com.gtnewhorizons.angelica.glsm.ffp;

import com.gtnewhorizons.angelica.glsm.GlslTransformUtils;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.grammar.GLSLParserBaseListener;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VertexShaderGeneratorTest {
    private static final int BIT_HAS_VERTEX_TEX = 17;

    @Test
    void primaryTextureAttributeAcceptsCompleteHomogeneousCoordinates() {
        VertexKey key = VertexKey.fromPacked(1L << BIT_HAS_VERTEX_TEX);
        String shader = VertexShaderGenerator.generate(key);
        Transformer transformer = new Transformer(ShaderParser.parseShader(shader).full());
        Map<String, GLSLParser.Single_declarationContext> inputs = transformer.findQualifiers(GLSLLexer.IN);
        TexCoordAssignmentListener assignment = inspectTexCoordAssignment(transformer);

        assertEquals("vec4", typeOf(inputs.get("a_TexCoord0")));
        assertEquals(1, assignment.assignments);
        assertEquals("a_TexCoord0", assignment.source);

        StringBuilder formatted = new StringBuilder();
        transformer.mutateTree(tree -> formatted.append(
            GlslTransformUtils.getFormattedShader(tree, "#version 330 core\n")
        ));
        ShaderParser.parseShader(formatted.toString()).full();
    }

    private static TexCoordAssignmentListener inspectTexCoordAssignment(Transformer transformer) {
        TexCoordAssignmentListener listener = new TexCoordAssignmentListener();
        transformer.mutateTree(tree -> ParseTreeWalker.DEFAULT.walk(listener, tree));
        return listener;
    }

    private static String typeOf(GLSLParser.Single_declarationContext declaration) {
        return declaration.fully_specified_type().type_specifier().type_specifier_nonarray().getText();
    }

    private static final class TexCoordAssignmentListener extends GLSLParserBaseListener {
        private int assignments;
        private String source;

        @Override
        public void enterAssignment_expression(GLSLParser.Assignment_expressionContext context) {
            if (context.assignment_operator() == null || !"v_TexCoord0".equals(context.unary_expression().getText())) {
                return;
            }

            assignments++;
            source = context.assignment_expression().getText();
        }
    }
}
