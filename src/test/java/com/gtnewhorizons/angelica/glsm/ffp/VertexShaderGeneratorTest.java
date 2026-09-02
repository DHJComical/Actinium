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
import static org.junit.jupiter.api.Assertions.assertTrue;

class VertexShaderGeneratorTest {
    private static final int BIT_HAS_VERTEX_TEX = 17;
    private static final int BIT_TEXTURE = 12;
    private static final int BIT_TEX_MATRIX = 14;
    private static final int BIT_TEXGEN_S = 22;
    private static final int BIT_TEXGEN_T = 25;
    private static final int BIT_TEXGEN_R = 28;

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

    @Test
    void eyeLinearTexGenFeedsEveryPlaneFromSingleConstructor() {
        final long packed = (1L << BIT_TEXTURE) | (1L << BIT_TEX_MATRIX)
            | ((long) VertexKey.TG_EYE_LINEAR << BIT_TEXGEN_S)
            | ((long) VertexKey.TG_EYE_LINEAR << BIT_TEXGEN_T)
            | ((long) VertexKey.TG_EYE_LINEAR << BIT_TEXGEN_R);
        final String shader = VertexShaderGenerator.generate(VertexKey.fromPacked(packed));
        final Transformer transformer = new Transformer(ShaderParser.parseShader(shader).full());
        final Map<String, GLSLParser.Single_declarationContext> uniforms = transformer.findQualifiers(GLSLLexer.UNIFORM);
        final TexGenInitListener texGen = inspectTexGenInit(transformer);

        assertEquals("vec4", typeOf(uniforms.get("u_TexGenEyePlaneS")));
        assertEquals("vec4", typeOf(uniforms.get("u_TexGenEyePlaneT")));
        assertEquals("vec4", typeOf(uniforms.get("u_TexGenEyePlaneR")));

        // Every eye plane must be consumed by the single vec4 constructor. The previous
        // per-component writes to a pre-initialized texGenCoord let NVIDIA's driver
        // dead-code-eliminate u_TexGenEyePlaneS, collapsing BPR's end-portal starfield
        // into stripes.
        assertEquals(0, texGen.componentWrites);
        assertEquals(1, texGen.initializers);
        assertTrue(texGen.initializer.startsWith("vec4("));
        assertTrue(texGen.initializer.contains("dot(eyePos,u_TexGenEyePlaneS)"));
        assertTrue(texGen.initializer.contains("dot(eyePos,u_TexGenEyePlaneT)"));
        assertTrue(texGen.initializer.contains("dot(eyePos,u_TexGenEyePlaneR)"));
    }

    private static TexGenInitListener inspectTexGenInit(Transformer transformer) {
        TexGenInitListener listener = new TexGenInitListener();
        transformer.mutateTree(tree -> ParseTreeWalker.DEFAULT.walk(listener, tree));
        return listener;
    }

    private static final class TexGenInitListener extends GLSLParserBaseListener {
        private int componentWrites;
        private int initializers;
        private String initializer = "";

        @Override
        public void enterAssignment_expression(GLSLParser.Assignment_expressionContext context) {
            if (context.assignment_operator() != null && context.unary_expression().getText().startsWith("texGenCoord.")) {
                componentWrites++;
            }
        }

        @Override
        public void enterSingle_declaration(GLSLParser.Single_declarationContext context) {
            final GLSLParser.Typeless_declarationContext decl = context.typeless_declaration();
            if (decl != null && decl.IDENTIFIER() != null && "texGenCoord".equals(decl.IDENTIFIER().getText())
                && decl.initializer() != null) {
                initializers++;
                initializer = decl.initializer().getText();
            }
        }
    }
}
