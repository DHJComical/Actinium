package net.coderbot.iris.pipeline.transform;

import net.coderbot.iris.gl.shader.ShaderType;
import net.coderbot.iris.pipeline.transform.parameter.CeleritasTerrainParameters;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.ShaderPrinter;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.grammar.GLSLParserBaseListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CeleritasTransformerTest {
    @Test
    void legacyChunkOffsetDeclarationIsReplacedByCanonicalUniform() {
        Transformer transformer = transformVertex("""
            #version 430 compatibility
            uniform vec3 chunkOffset;

            vec3 readRegionOffset() {
                return chunkOffset;
            }

            void main() {
                gl_Position = vec4(readRegionOffset(), 1.0);
            }
            """);

        RegionOffsetListener listener = inspect(transformer);

        assertEquals(1, listener.regionOffsetUniformDeclarations);
        assertEquals(0, listener.legacyChunkOffsetIdentifiers);
        assertEquals(1, listener.regionOffsetReferencesInPackFunction);
    }

    @Test
    void canonicalRegionOffsetUniformIsInjectedWhenLegacyDeclarationIsAbsent() {
        Transformer transformer = transformVertex("""
            #version 430 compatibility
            void main() {
                gl_Position = vec4(0.0);
            }
            """);

        RegionOffsetListener listener = inspect(transformer);

        assertEquals(1, listener.regionOffsetUniformDeclarations);
        assertEquals(0, listener.legacyChunkOffsetIdentifiers);
    }

    @Test
    void worldSpacePositionOverwriteIsReplacedWithClipSpaceTransform() {
        Transformer transformer = transformVertex("""
            #version 430 compatibility
            void main() {
                vec3 worldpos = vec3(0.0);
                gl_Position = iris_ftransform();
                gl_Position = vec4(worldpos, 0.0);
            }
            """);

        String output = format(transformer);

        assertEquals(2, occurrences(output, "gl_Position = iris_ftransform();"), output);
        assertFalse(output.contains("vec4(worldpos, 0.0)"), output);
    }

    @Test
    void geometryStageDoesNotReprojectCeleritasClipSpacePositions() {
        Transformer transformer = transformGeometry("""
            #version 430 core
            layout(triangles) in;
            layout(triangle_strip, max_vertices = 3) out;

            uniform mat4 gbufferModelView;

            vec4 toClipSpace3(vec3 viewSpacePosition) {
                return vec4(viewSpacePosition, -viewSpacePosition.z);
            }

            void main() {
                vec4 vertex = gl_in[0].gl_Position;
                vertex = toClipSpace3(mat3(gbufferModelView) * vec3(vertex) + gbufferModelView[3].xyz);
                gl_Position = vertex;
                EmitVertex();
                EndPrimitive();
            }
            """);

        String output = format(transformer);

        assertEquals(1, occurrences(output, "vertex = vertex;"), output);
        assertFalse(output.contains("toClipSpace3 ( mat3 ( gbufferModelView )"), output);
    }

    private static Transformer transformVertex(String source) {
        Transformer transformer = new Transformer(ShaderParser.parseShader(source).full());
        CeleritasTerrainParameters parameters = new CeleritasTerrainParameters(Patch.CELERITAS_TERRAIN);
        parameters.type = ShaderType.VERTEX;
        CeleritasTransformer.transformVertex(transformer, parameters);
        return transformer;
    }

    private static Transformer transformGeometry(String source) {
        Transformer transformer = new Transformer(ShaderParser.parseShader(source).full());
        CeleritasTerrainParameters parameters = new CeleritasTerrainParameters(Patch.CELERITAS_TERRAIN);
        parameters.type = ShaderType.GEOMETRY;
        CeleritasTransformer.transform(transformer, parameters, 460);
        return transformer;
    }

    private static RegionOffsetListener inspect(Transformer transformer) {
        RegionOffsetListener listener = new RegionOffsetListener();
        transformer.mutateTree(tree -> ParseTreeWalker.DEFAULT.walk(listener, tree));
        return listener;
    }

    private static String format(Transformer transformer) {
        StringBuilder output = new StringBuilder();
        transformer.mutateTree(tree -> output.append(ShaderPrinter.getFormattedShader(tree)));
        return output.toString();
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static final class RegionOffsetListener extends GLSLParserBaseListener {
        private int regionOffsetUniformDeclarations;
        private int legacyChunkOffsetIdentifiers;
        private int regionOffsetReferencesInPackFunction;
        private boolean insidePackFunction;

        @Override
        public void enterFunction_definition(GLSLParser.Function_definitionContext context) {
            insidePackFunction = "readRegionOffset".equals(context.function_prototype().IDENTIFIER().getText());
        }

        @Override
        public void exitFunction_definition(GLSLParser.Function_definitionContext context) {
            insidePackFunction = false;
        }

        @Override
        public void enterSingle_declaration(GLSLParser.Single_declarationContext context) {
            GLSLParser.Typeless_declarationContext declaration = context.typeless_declaration();
            GLSLParser.Fully_specified_typeContext type = context.fully_specified_type();
            if (declaration == null || declaration.IDENTIFIER() == null || type.type_qualifier() == null) {
                return;
            }

            if ("u_RegionOffset".equals(declaration.IDENTIFIER().getText())
                && "uniform".equals(type.type_qualifier().getText())
                && "vec3".equals(type.type_specifier().getText())) {
                regionOffsetUniformDeclarations++;
            }
        }

        @Override
        public void visitTerminal(TerminalNode node) {
            if (node.getSymbol().getType() != GLSLLexer.IDENTIFIER) {
                return;
            }

            if ("chunkOffset".equals(node.getText())) {
                legacyChunkOffsetIdentifiers++;
            } else if (insidePackFunction && "u_RegionOffset".equals(node.getText())) {
                regionOffsetReferencesInPackFunction++;
            }
        }
    }
}
