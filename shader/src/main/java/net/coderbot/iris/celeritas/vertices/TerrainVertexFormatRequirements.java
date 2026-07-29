package net.coderbot.iris.celeritas.vertices;

import net.coderbot.iris.Iris;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.taumc.glsl.grammar.GLSLLexer;

import java.util.Collection;
import java.util.Objects;

/** Determines which Iris terrain-only vertex attributes are read by transformed shader programs. */
public final class TerrainVertexFormatRequirements {
    /** Optional attributes appended after Celeritas' Vanilla-like base terrain format. */
    public enum Attribute {
        MC_ENTITY("mc_Entity"),
        MID_TEX_COORD("mc_midTexCoord"),
        TANGENT("at_tangent"),
        MID_BLOCK("at_midBlock"),
        NORMAL("iris_Normal");

        private final String shaderName;

        Attribute(String shaderName) {
            this.shaderName = shaderName;
        }
    }

    private static final int ALL_ATTRIBUTES = (1 << Attribute.values().length) - 1;

    private final int flags;

    private TerrainVertexFormatRequirements(int flags) {
        this.flags = flags;
    }

    /** Returns the conservative layout used before transformed terrain sources are available. */
    public static TerrainVertexFormatRequirements all() {
        return new TerrainVertexFormatRequirements(ALL_ATTRIBUTES);
    }

    /** Returns a requirement set containing exactly the supplied optional attributes. */
    public static TerrainVertexFormatRequirements of(Attribute... attributes) {
        Objects.requireNonNull(attributes, "Attributes must not be null");

        int flags = 0;
        for (Attribute attribute : attributes) {
            flags |= bit(Objects.requireNonNull(attribute, "Attribute must not be null"));
        }
        return new TerrainVertexFormatRequirements(flags);
    }

    /**
     * Analyzes every active terrain and shadow vertex shader as one union because section VBOs are shared by passes.
     * A source that cannot be inspected is treated as requiring the complete format.
     */
    public static TerrainVertexFormatRequirements analyze(Collection<String> transformedVertexSources) {
        Objects.requireNonNull(transformedVertexSources, "Transformed vertex sources must not be null");

        int flags = 0;
        for (String source : transformedVertexSources) {
            if (source == null || source.isBlank()) {
                Iris.logger.warn("Celeritas terrain vertex format analysis received an unavailable transformed vertex shader; using the complete format");
                return all();
            }

            try {
                for (Attribute attribute : Attribute.values()) {
                    if (isReferenced(source, attribute)) {
                        flags |= bit(attribute);
                    }
                }
            } catch (RuntimeException exception) {
                Iris.logger.warn("Celeritas terrain vertex format analysis failed; using the complete format", exception);
                return all();
            }
        }

        return new TerrainVertexFormatRequirements(flags);
    }

    /** Returns whether the shared terrain VBO must contain an optional attribute. */
    public boolean requires(Attribute attribute) {
        return (this.flags & bit(Objects.requireNonNull(attribute, "Attribute must not be null"))) != 0;
    }

    private static int bit(Attribute attribute) {
        return 1 << attribute.ordinal();
    }

    private static boolean isReferenced(String source, Attribute attribute) {
        LexerErrorCounter errors = new LexerErrorCounter();
        GLSLLexer lexer = new GLSLLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);

        int occurrences = 0;
        for (Token token : lexer.getAllTokens()) {
            if (token.getType() == GLSLLexer.IDENTIFIER && attribute.shaderName.equals(token.getText())) {
                occurrences++;
            }
        }

        if (errors.count != 0) {
            throw new IllegalArgumentException("Invalid transformed GLSL source");
        }

        // Celeritas always declares iris_Normal. A declaration without a second reference does not need storage.
        return occurrences > 1;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof TerrainVertexFormatRequirements other && this.flags == other.flags;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.flags);
    }

    private static final class LexerErrorCounter extends BaseErrorListener {
        private int count;

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                String message, RecognitionException exception) {
            this.count++;
        }
    }
}
