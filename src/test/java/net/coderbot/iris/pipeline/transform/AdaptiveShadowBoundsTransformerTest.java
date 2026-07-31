package net.coderbot.iris.pipeline.transform;

import com.gtnewhorizons.angelica.glsm.GlslTransformUtils;
import net.coderbot.iris.gl.shader.ShaderType;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.grammar.GLSLParserBaseListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveShadowBoundsTransformerTest {
    @Test
    void insertsBoundsGuardIntoRecognizedPcfHelper() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            float texture2DShadow2x2(sampler2D shadowtex, vec3 shadowPos) {
                float shadowSample = texture(shadowtex, shadowPos.xy).x;
                return shadowSample + 0.0 / shadowMapResolution;
            }
            void main() {
                gl_FragColor = vec4(texture2DShadow2x2(shadowtex, vec3(0.0)), 1.0);
            }
        """);

        assertTrue(transformer.hasVariable("shadowMapResolution"));
        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        List<GLSLParser.Function_definitionContext> functions = functions(format(transformer));
        GLSLParser.Function_definitionContext helper = findFunction(functions, "texture2DShadow2x2");
        GLSLParser.Function_definitionContext main = findFunction(functions, "main");

        assertEquals(1, selections(helper).size(), format(transformer));
        assertTrue(selections(helper).getFirst().contains("shadowPos.x"));
        assertTrue(selections(helper).getFirst().contains("shadowMapResolution"));
        assertTrue(returnExpressions(helper).contains("1.0"));
        assertTrue(calls(main).contains("texture2DShadow2x2"), format(transformer));
    }

    @Test
    void insertsVec3GuardIntoSampleFilteredShadow() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            vec3 SampleFilteredShadow(vec3 shadowPos, float offset, float subsurface) {
                return vec3(texture(shadowtex0, shadowPos.xy).x + offset + subsurface);
            }
            void main() {
                gl_FragColor = vec4(SampleFilteredShadow(vec3(0.0), 0.0, 0.0), 1.0);
            }
        """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        GLSLParser.Function_definitionContext helper = findFunction(functions(format(transformer)), "SampleFilteredShadow");
        assertEquals(1, selections(helper).size());
        assertTrue(selections(helper).getFirst().contains("shadowPos.z"));
        assertTrue(returnExpressions(helper).contains("vec3(1.0)"));
    }

    @Test
    void insertsGuardsIntoBothPcfHelpersInOneShader() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            float texture2DShadow2x2(sampler2D shadowtex, vec3 shadowPos) {
                return texture(shadowtex, shadowPos.xy).x + 0.0 / shadowMapResolution;
            }
            vec3 SampleFilteredShadow(vec3 shadowPos, float offset, float subsurface) {
                return vec3(texture(shadowtex0, shadowPos.xy).x + offset + subsurface);
            }
            void main() {
                gl_FragColor = vec4(
                    texture2DShadow2x2(shadowtex, vec3(0.0))
                    + SampleFilteredShadow(vec3(0.0), 0.0, 0.0),
                    1.0
                );
            }
        """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        List<GLSLParser.Function_definitionContext> functions = functions(format(transformer));
        assertEquals(1, selections(findFunction(functions, "texture2DShadow2x2")).size());
        assertEquals(1, selections(findFunction(functions, "SampleFilteredShadow")).size());
    }

    @Test
    void leavesFunctionWithExistingBoundsGuardUntouched() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            float texture2DShadow2x2(sampler2D shadowtex, vec3 shadowPos) {
                if (abs(shadowPos.x) < 1.0 - 1.5 / shadowMapResolution
                    && abs(shadowPos.y) < 1.0 - 1.5 / shadowMapResolution
                    && abs(shadowPos.z) < 6.0) {
                    return texture(shadowtex, shadowPos.xy).x;
                }
                return 1.0;
            }
            void main() {
                gl_FragColor = vec4(texture2DShadow2x2(shadowtex, vec3(0.0)), 1.0);
            }
            """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        GLSLParser.Function_definitionContext helper = findFunction(functions(format(transformer)), "texture2DShadow2x2");
        assertEquals(1, selections(helper).size());
        assertEquals(1, returnExpressions(helper).stream().filter("1.0"::equals).count());
    }

    @Test
    void ignoresWrongSignaturesAndNonPcfFunctions() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            float texture2DShadow2x2(sampler2D shadowtex, vec2 shadowPos) {
                return texture(shadowtex, shadowPos).x + 0.0 / shadowMapResolution;
            }
            vec4 sampleColor(sampler2D colorTexture, vec2 uv) {
                return texture(colorTexture, uv);
            }
            void main() {
                gl_FragColor = sampleColor(colorTexture, vec2(0.0));
            }
            """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        List<GLSLParser.Function_definitionContext> functions = functions(format(transformer));
        assertEquals(0, selections(findFunction(functions, "texture2DShadow2x2")).size());
        assertEquals(0, selections(findFunction(functions, "sampleColor")).size());
    }

    @Test
    void ignoresPcfNameWithoutShadowCoordinateParameter() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            float texture2DShadow2x2(sampler2D shadowtex) {
                return texture(shadowtex, vec2(0.0)).x + 0.0 / shadowMapResolution;
            }
            void main() {
                gl_FragColor = vec4(texture2DShadow2x2(shadowtex), 1.0);
            }
        """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        GLSLParser.Function_definitionContext helper = findFunction(functions(format(transformer)), "texture2DShadow2x2");
        assertEquals(0, selections(helper).size());
    }

    @Test
    void leavesShaderWithoutShadowResolutionUntouched() {
        Transformer transformer = transformer("""
            #version 330 core
            float texture2DShadow2x2(sampler2D shadowtex, vec3 shadowPos) {
                return texture(shadowtex, shadowPos.xy).x;
            }
            void main() {
                gl_FragColor = vec4(texture2DShadow2x2(shadowtex, vec3(0.0)), 1.0);
            }
        """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.FRAGMENT);

        GLSLParser.Function_definitionContext helper = findFunction(functions(format(transformer)), "texture2DShadow2x2");
        assertEquals(0, selections(helper).size());
    }

    @Test
    void leavesVertexShaderUntouched() {
        Transformer transformer = transformer("""
            #version 330 core
            const float shadowMapResolution = 2048.0;
            float texture2DShadow2x2(sampler2D shadowtex, vec3 shadowPos) {
                return texture(shadowtex, shadowPos.xy).x + 0.0 / shadowMapResolution;
            }
            void main() {
                gl_Position = vec4(texture2DShadow2x2(shadowtex, vec3(0.0)));
            }
        """);

        AdaptiveShadowBoundsTransformer.transform(transformer, ShaderType.VERTEX);

        GLSLParser.Function_definitionContext helper = findFunction(functions(format(transformer)), "texture2DShadow2x2");
        assertEquals(0, selections(helper).size());
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

    private static List<GLSLParser.Function_definitionContext> functions(String source) {
        ShaderParser.ParsedShader parsed = ShaderParser.parseShader(source);
        assertEquals(0, parsed.parser().getNumberOfSyntaxErrors(), source);
        List<GLSLParser.Function_definitionContext> functions = new ArrayList<>();
        ParseTreeWalker.DEFAULT.walk(new GLSLParserBaseListener() {
            @Override
            public void enterFunction_definition(GLSLParser.Function_definitionContext context) {
                functions.add(context);
            }
        }, parsed.full());
        return functions;
    }

    private static GLSLParser.Function_definitionContext findFunction(
        List<GLSLParser.Function_definitionContext> functions,
        String name
    ) {
        return functions.stream()
            .filter(function -> name.equals(function.function_prototype().IDENTIFIER().getText()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing function " + name));
    }

    private static List<String> selections(GLSLParser.Function_definitionContext function) {
        List<String> conditions = new ArrayList<>();
        ParseTreeWalker.DEFAULT.walk(new GLSLParserBaseListener() {
            @Override
            public void enterSelection_statement(GLSLParser.Selection_statementContext context) {
                conditions.add(context.expression().getText());
            }
        }, function);
        return conditions;
    }

    private static List<String> returnExpressions(GLSLParser.Function_definitionContext function) {
        List<String> expressions = new ArrayList<>();
        ParseTreeWalker.DEFAULT.walk(new GLSLParserBaseListener() {
            @Override
            public void enterJump_statement(GLSLParser.Jump_statementContext context) {
                if (context.RETURN() != null && context.expression() != null) {
                    expressions.add(context.expression().getText());
                }
            }
        }, function);
        return expressions;
    }

    private static List<String> calls(GLSLParser.Function_definitionContext function) {
        List<String> names = new ArrayList<>();
        ParseTreeWalker.DEFAULT.walk(new GLSLParserBaseListener() {
            @Override
            public void enterPostfix_expression(GLSLParser.Postfix_expressionContext context) {
                if (context.function_call_parameters() == null || context.getChildCount() == 0) {
                    return;
                }
                names.add(context.getChild(0).getText());
            }
        }, function);
        return names;
    }
}
