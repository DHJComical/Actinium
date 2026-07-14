package net.coderbot.iris.pipeline.transform;

import net.coderbot.iris.gl.shader.ShaderType;
import net.coderbot.iris.pipeline.transform.parameter.CeleritasTerrainParameters;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.grammar.GLSLParserBaseListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static Transformer transformVertex(String source) {
        Transformer transformer = new Transformer(ShaderParser.parseShader(source).full());
        CeleritasTerrainParameters parameters = new CeleritasTerrainParameters(Patch.CELERITAS_TERRAIN);
        parameters.type = ShaderType.VERTEX;
        CeleritasTransformer.transformVertex(transformer, parameters);
        return transformer;
    }

    private static RegionOffsetListener inspect(Transformer transformer) {
        RegionOffsetListener listener = new RegionOffsetListener();
        transformer.mutateTree(tree -> ParseTreeWalker.DEFAULT.walk(listener, tree));
        return listener;
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
