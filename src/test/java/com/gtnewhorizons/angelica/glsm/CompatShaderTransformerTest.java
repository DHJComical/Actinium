package com.gtnewhorizons.angelica.glsm;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.grammar.GLSLLexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatShaderTransformerTest {

    @Test
    void portalGunStyleFragmentDropsGlesPrecisionGuardForDesktopCore() {
        String source = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            uniform float closeAlpha;
            #define PI 3.1415926535897932384626433832795

            void main(void) {
                vec2 coord = gl_TexCoord[0].xy;
                gl_FragColor = vec4(coord, sin(PI * closeAlpha), 1.0);
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);
        ShaderInspection inspection = inspectShader(transformed);

        assertEquals(0, countTokens(transformed, GLSLLexer.PRECISION), transformed);
        assertEquals(0, inspection.preprocessorSyntaxErrors, transformed);
        assertEquals(0, inspection.shaderSyntaxErrors, transformed);
        assertEquals(0, countTokens(transformed, GLSLLexer.IFDEF_DIRECTIVE), transformed);
        assertEquals(0, countTokens(transformed, GLSLLexer.ENDIF_DIRECTIVE), transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.DEFINE_DIRECTIVE), transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.VERSION_DIRECTIVE), transformed);
        assertOrdered(transformed, "#version", "#define PI", "uniform float closeAlpha");
    }

    @Test
    void ordinaryConditionalPreprocessorDirectivesRemainBalanced() {
        String source = """
            #define USE_BLUE 1
            #ifdef USE_BLUE
            #define PORTAL_COLOR vec4(0.0, 0.5, 1.0, 1.0)
            #else
            #define PORTAL_COLOR vec4(1.0, 0.5, 0.0, 1.0)
            #endif
            #define SCALE_COLOR(color) \\
                ((color) * 0.5)

            void main(void) {
                gl_FragColor = SCALE_COLOR(PORTAL_COLOR);
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);
        ShaderInspection inspection = inspectShader(transformed);

        assertEquals(0, inspection.preprocessorSyntaxErrors, transformed);
        assertEquals(0, inspection.shaderSyntaxErrors, transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.IFDEF_DIRECTIVE), transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.ELSE_DIRECTIVE), transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.ENDIF_DIRECTIVE), transformed);
        assertEquals(4, countTokens(transformed, GLSLLexer.DEFINE_DIRECTIVE), transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.MACRO_ESC_NEWLINE), transformed);
        assertOrdered(
            transformed,
            "#define USE_BLUE",
            "#ifdef USE_BLUE",
            "#else",
            "#endif",
            "#define SCALE_COLOR",
            "void main"
        );
    }

    @Test
    void commentedGlesPrecisionGuardIsRemovedAsOneBlock() {
        String source = """
            #ifdef GL_ES // mobile profile
            precision highp int; // default integer precision
            #endif // GL_ES

            void main() {
                gl_FragColor = vec4(1.0);
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);
        ShaderInspection inspection = inspectShader(transformed);

        assertEquals(0, inspection.preprocessorSyntaxErrors, transformed);
        assertEquals(0, inspection.shaderSyntaxErrors, transformed);
        assertEquals(0, countTokens(transformed, GLSLLexer.PRECISION), transformed);
        assertEquals(0, countTokens(transformed, GLSLLexer.IFDEF_DIRECTIVE), transformed);
        assertEquals(0, countTokens(transformed, GLSLLexer.ENDIF_DIRECTIVE), transformed);
    }

    @Test
    void conditionalGlslRemainsBetweenItsDirectives() {
        String source = """
            #ifdef USE_PORTAL_COLOR
            varying vec4 portalColor;
            #else
            varying vec4 fallbackColor;
            #endif

            void main() {
                gl_FragColor = portalColor;
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);

        assertEquals("#version 330 core\n" + source, transformed);
        assertOrdered(
            transformed,
            "#ifdef USE_PORTAL_COLOR",
            "varying vec4 portalColor",
            "#else",
            "varying vec4 fallbackColor",
            "#endif",
            "void main"
        );
    }

    @Test
    void defineAndUndefKeepTheirPointInSource() {
        String source = """
            #define PORTAL_SCALE 1.0
            const float firstScale = PORTAL_SCALE;
            #undef PORTAL_SCALE
            #define PORTAL_SCALE 2.0
            const float secondScale = PORTAL_SCALE;

            void main() {
                gl_FragColor = vec4(firstScale, secondScale, 0.0, 1.0);
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);

        assertEquals("#version 330 core\n" + source, transformed);
        assertOrdered(
            transformed,
            "#define PORTAL_SCALE 1.0",
            "const float firstScale",
            "#undef PORTAL_SCALE",
            "#define PORTAL_SCALE 2.0",
            "const float secondScale"
        );
    }

    @Test
    void directiveTextInsideBlockCommentIsNotActivated() {
        String source = """
            /*
            #define HIDDEN_PORTAL_COLOR vec4(1.0)
            */
            #define ACTIVE_PORTAL_COLOR vec4(0.0)

            void main() {
                gl_FragColor = ACTIVE_PORTAL_COLOR;
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);
        ShaderInspection inspection = inspectShader(transformed);

        assertEquals(0, inspection.preprocessorSyntaxErrors, transformed);
        assertEquals(0, inspection.shaderSyntaxErrors, transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.DEFINE_DIRECTIVE), transformed);
        assertOrdered(transformed, "#version", "#define ACTIVE_PORTAL_COLOR", "void main");
    }

    @Test
    void extensionAndContinuedMacroStayInPreambleOrder() {
        String source = """
            #version 120
            #extension GL_ARB_texture_rectangle : enable
            #define HALF_COLOR(color) \\
                ((color) * 0.5)

            void main() {
                gl_FragColor = HALF_COLOR(vec4(1.0));
            }
            """;

        String transformed = CompatShaderTransformer.transform(source, true);
        ShaderInspection inspection = inspectShader(transformed);

        assertEquals(0, inspection.preprocessorSyntaxErrors, transformed);
        assertEquals(0, inspection.shaderSyntaxErrors, transformed);
        assertEquals(1, countTokens(transformed, GLSLLexer.MACRO_ESC_NEWLINE), transformed);
        assertOrdered(
            transformed,
            "#version 330 core",
            "#extension GL_ARB_texture_rectangle : enable",
            "#define HALF_COLOR",
            "((color) * 0.5)",
            "void main"
        );
    }

    private static int countTokens(String source, int tokenType) {
        GLSLLexer lexer = new GLSLLexer(CharStreams.fromString(source));
        int count = 0;
        for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {
            if (token.getType() == tokenType) {
                count++;
            }
        }
        return count;
    }

    private static ShaderInspection inspectShader(String source) {
        ShaderParser.ParsedShader shader = ShaderParser.parseShader(source);
        return new ShaderInspection(shader.preParser().getNumberOfSyntaxErrors(), shader.parser().getNumberOfSyntaxErrors());
    }

    private static void assertOrdered(String source, String... fragments) {
        int previousOffset = -1;
        for (String fragment : fragments) {
            int offset = source.indexOf(fragment, previousOffset + 1);
            assertTrue(offset > previousOffset, "Expected '" + fragment + "' after offset " + previousOffset + " in:\n" + source);
            previousOffset = offset;
        }
    }

    private record ShaderInspection(
        int preprocessorSyntaxErrors,
        int shaderSyntaxErrors
    ) {}
}
