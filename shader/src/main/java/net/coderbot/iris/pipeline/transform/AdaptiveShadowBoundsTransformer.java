package net.coderbot.iris.pipeline.transform;

import com.gtnewhorizons.angelica.glsm.GlslTransformUtils;
import com.gtnewhorizons.angelica.glsm.debug.GLSMPerfDebug;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.shader.ShaderType;
import net.coderbot.iris.pipeline.AdaptiveShadowBoundsStats;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.grammar.GLSLParserBaseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adds an early return around the two PCF helper shapes used by common shader packs.
 *
 * <p>The GLSL transformation library does not expose a generic function-body insertion API.
 * This transformer therefore only handles functions with a known name, return type, parameter
 * shape, and shadow-map coordinate parameter. Unknown sampling code is left untouched.</p>
 */
final class AdaptiveShadowBoundsTransformer {
    private static final List<String> PCF_FUNCTION_NAMES = List.of(
        "texture2DShadow2x2",
        "SampleFilteredShadow"
    );

    private AdaptiveShadowBoundsTransformer() {
    }

    static void transform(Transformer root, ShaderType shaderType) {
        final boolean instrumentationEnabled = AdaptiveShadowBoundsStats.isInstrumentationEnabled();
        final int instrumentationBinding = instrumentationEnabled ? AdaptiveShadowBoundsStats.getActiveBinding() : -1;
        transform(root, shaderType, instrumentationEnabled, instrumentationBinding);
    }

    static boolean mayInjectRuntimeStats(String source) {
        return source.contains("shadowMapResolution")
            && (source.contains("texture2DShadow2x2") || source.contains("SampleFilteredShadow"));
    }

    static void transform(Transformer root, ShaderType shaderType, boolean instrumentationEnabled, int instrumentationBinding) {
        if (shaderType != ShaderType.FRAGMENT || !root.hasVariable("shadowMapResolution")) {
            return;
        }

        final List<FunctionCandidate> candidates = new ArrayList<>();
        root.mutateTree(tree -> ParseTreeWalker.DEFAULT.walk(new GLSLParserBaseListener() {
            @Override
            public void enterFunction_definition(GLSLParser.Function_definitionContext context) {
                final String functionName = context.function_prototype().IDENTIFIER().getText();
                if (!PCF_FUNCTION_NAMES.contains(functionName)) {
                    return;
                }
                FunctionCandidate candidate = FunctionCandidate.from(context);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }, tree));

        int injected = 0;
        int alreadyGuarded = 0;
        int unsupported = 0;
        for (FunctionCandidate candidate : candidates) {
            if (candidate.hasBoundsGuard()) {
                alreadyGuarded++;
                continue;
            }
            if (!candidate.isSupportedPcfBody()) {
                unsupported++;
                continue;
            }

            root.replaceExpression(
                candidate.source(),
                candidate.patchedSource(instrumentationEnabled),
                GLSLParser::function_definition
            );
            injected++;
            debug(candidate.name());
        }

        if (injected > 0 && instrumentationEnabled) {
            root.injectVariable(AdaptiveShadowBoundsStats.declarationForBinding(instrumentationBinding));
        }

        if (GLSMPerfDebug.isEnabled()) {
            Iris.logger.info(
                "[AdaptiveShadowBounds] inspected={} injected={} alreadyGuarded={} unsupported={}",
                candidates.size(), injected, alreadyGuarded, unsupported
            );
        }
    }

    private static void debug(String functionName) {
        if (GLSMPerfDebug.isEnabled()) {
            Iris.logger.info("[AdaptiveShadowBounds] injected function={}", functionName);
        }
    }

    private record Parameter(String name, String type) {
    }

    private record FunctionCandidate(
        String source,
        String name,
        String returnType,
        List<Parameter> parameters,
        String body
    ) {
        private static FunctionCandidate from(GLSLParser.Function_definitionContext context) {
            final GLSLParser.Function_prototypeContext prototype = context.function_prototype();
            final GLSLParser.Function_parametersContext functionParameters = prototype.function_parameters();
            if (functionParameters == null) {
                return null;
            }

            final List<Parameter> parameters = new ArrayList<>();
            for (GLSLParser.Parameter_declarationContext declaration : functionParameters.parameter_declaration()) {
                final GLSLParser.Parameter_declaratorContext parameterDeclarator = declaration.parameter_declarator();
                final String name = parameterDeclarator == null || parameterDeclarator.IDENTIFIER() == null
                    ? null
                    : parameterDeclarator.IDENTIFIER().getText();
                final String type;
                if (parameterDeclarator != null && parameterDeclarator.type_specifier() != null) {
                    type = parameterDeclarator.type_specifier().getText();
                } else if (declaration.parameter_type_specifier() != null
                    && declaration.parameter_type_specifier().type_specifier() != null) {
                    type = declaration.parameter_type_specifier().type_specifier().getText();
                } else {
                    type = "";
                }
                if (name == null || type.isEmpty()) {
                    return null;
                }
                parameters.add(new Parameter(name, type));
            }

            return new FunctionCandidate(
                GlslTransformUtils.getFormattedShader(context, ""),
                prototype.IDENTIFIER().getText(),
                prototype.fully_specified_type().getText(),
                List.copyOf(parameters),
                context.compound_statement_no_new_scope().getText()
            );
        }

        private String patchedSource(boolean instrumentationEnabled) {
            final int bodyStart = source.indexOf('{');
            if (bodyStart < 0) {
                throw new IllegalStateException("PCF helper has no function body: " + name);
            }
            final String coordinate = coordinateName();
            final String defaultValue = "float".equals(returnType) ? "1.0" : "vec3(1.0)";
            final String margin = "1.5 / shadowMapResolution";
            final String bounds = coordinate + ".x > " + margin
                + " && " + coordinate + ".x < 1.0 - " + margin
                + " && " + coordinate + ".y > " + margin
                + " && " + coordinate + ".y < 1.0 - " + margin
                + " && " + coordinate + ".z > 0.0"
                + " && " + coordinate + ".z < 1.0";
            if (!instrumentationEnabled) {
                final String guard = "if (!(" + bounds + ")) return " + defaultValue + ";";
                return source.substring(0, bodyStart + 1) + guard + source.substring(bodyStart + 1);
            }

            final String guard = "if (!(" + bounds + ")) {"
                + AdaptiveShadowBoundsStats.rejectedCounter(name)
                + "return " + defaultValue + ";}";
            return source.substring(0, bodyStart + 1)
                + AdaptiveShadowBoundsStats.callCounter(name)
                + guard
                + source.substring(bodyStart + 1);
        }

        private boolean hasBoundsGuard() {
            if (!hasCoordinateParameter()) {
                return false;
            }
            final String normalized = body.toLowerCase(Locale.ROOT);
            final String coordinate = coordinateName().toLowerCase(Locale.ROOT);
            return normalized.contains("shadowbounds")
                || normalized.contains("issampleinshadowmap")
                || normalized.contains("abs(" + coordinate + ".x)")
                || (normalized.contains(coordinate + ".x>")
                    && normalized.contains(coordinate + ".x<")
                    && normalized.contains(coordinate + ".y>")
                    && normalized.contains(coordinate + ".y<"));
        }

        private boolean isSupportedPcfBody() {
            if (!List.of("float", "vec3").contains(returnType)) {
                return false;
            }
            if ("texture2DShadow2x2".equals(name)) {
                return parameters.size() == 2
                    && "sampler2D".equals(parameters.get(0).type())
                    && "vec3".equals(parameters.get(1).type())
                    && "shadowPos".equals(coordinateName())
                    && "float".equals(returnType)
                    && body.contains("shadowMapResolution")
                    && (body.contains("texture") || body.contains("shadow2D") || body.contains("getShadow"));
            }
            return parameters.size() == 3
                && "vec3".equals(parameters.get(0).type())
                && "float".equals(parameters.get(1).type())
                && "float".equals(parameters.get(2).type())
                && "shadowPos".equals(coordinateName())
                && "vec3".equals(returnType)
                && (body.contains("shadowtex") || body.contains("shadow2D") || body.contains("texture"));
        }

        private String coordinateName() {
            if (hasCoordinateParameter()) {
                return parameters.get(coordinateIndex()).name();
            }
            throw new IllegalStateException("PCF helper has no shadow coordinate parameter: " + name);
        }

        private boolean hasCoordinateParameter() {
            return parameters.size() > coordinateIndex();
        }

        private int coordinateIndex() {
            return "texture2DShadow2x2".equals(name) ? 1 : 0;
        }

    }
}
