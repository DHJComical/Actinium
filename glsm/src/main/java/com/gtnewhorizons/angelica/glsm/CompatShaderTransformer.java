package com.gtnewhorizons.angelica.glsm;

import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.Transformer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gtnewhorizons.angelica.glsm.backend.BackendManager.RENDER_BACKEND;

/**
 * Compat transformation for mod shaders running under core profile.
 *
 * <p>Handles:
 * <ul>
 *   <li>Matrix builtin replacement (gl_ModelViewMatrix, gl_ProjectionMatrix, etc.)</li>
 *   <li>Vertex attribute replacement (gl_Vertex, gl_Color, gl_MultiTexCoord0/1, gl_Normal)</li>
 *   <li>gl_TexCoord[N] varying array → per-index in/out declarations</li>
 *   <li>Texture function renames (texture2D → texture, etc.)</li>
 *   <li>Fragment output handling (gl_FragColor → layout-qualified out declarations)</li>
 *   <li>Fog builtins (gl_Fog, gl_FogFragCoord)</li>
 *   <li>gl_FrontColor → local variable in vertex shaders</li>
 *   <li>shadow2D/shadow2DLod → texture/textureLod with vec4 wrapping</li>
 *   <li>Version upgrade to 330 core minimum</li>
 *   <li>Reserved word pre-parse renaming (texture-as-variable, sample, etc.)</li>
 * </ul>
 */
public class CompatShaderTransformer {

    private static final Logger LOGGER = LogManager.getLogger(CompatShaderTransformer.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile("#version[ \\t]+(\\d+)(?:[ \\t]+(\\w+))?");

    private static final Pattern GLES_IFDEF_PATTERN = Pattern.compile(
        "^[ \\t]*#[ \\t]*ifdef[ \\t]+GL_ES[ \\t]*(?://.*)?$"
    );
    private static final Pattern ENDIF_PATTERN = Pattern.compile("^[ \\t]*#[ \\t]*endif[ \\t]*(?://.*)?$");
    private static final Pattern PRECISION_DECLARATION_PATTERN = Pattern.compile(
        "^[ \\t]*precision[ \\t]+(?:lowp|mediump|highp)[ \\t]+(?:float|int)[ \\t]*;[ \\t]*(?://.*)?$"
    );
    private static final Pattern MAIN_VOID_PARAMETERS_PATTERN = Pattern.compile(
        "\\bmain[ \\t\\r\\n]*\\([ \\t\\r\\n]*void[ \\t\\r\\n]*\\)"
    );
    private static final Pattern DIRECTIVE_NAME_PATTERN = Pattern.compile("[ \\t]*([A-Za-z]+)");
    private static final Pattern MACRO_NAME_PATTERN = Pattern.compile("[ \\t]*(?:define|undef)[ \\t]+([A-Za-z_][A-Za-z0-9_]*)");

    /** Compat builtins that trigger AST transformation. */
    private static final Set<String> COMPAT_BUILTINS = Set.of(
        "gl_ModelView", "gl_Projection", "gl_NormalMatrix", "gl_TextureMatrix",
        "gl_FragColor", "gl_Fog", "gl_FrontColor", "gl_Color",
        "gl_Vertex", "gl_MultiTexCoord", "gl_TexCoord", "gl_Normal", "ftransform",
        "texture2D", "texture3D", "texelFetch2D", "texelFetch3D", "textureSize2D",
        "shadow2D", "gl_FrontLightModelProduct",
        "gl_LightSource", "gl_FrontMaterial"
    );

    private static final Pattern NEEDS_TRANSFORM_PATTERN = Pattern.compile(
        String.join("|", COMPAT_BUILTINS) + "|\\b(?:attribute|varying)\\b"
    );

    private static final Map<String, String> MATRIX_RENAMES = Map.of(
        "gl_ModelViewMatrix", "angelica_ModelViewMatrix",
        "gl_ModelViewMatrixInverse", "angelica_ModelViewMatrixInverse",
        "gl_ProjectionMatrix", "angelica_ProjectionMatrix",
        "gl_ProjectionMatrixInverse", "angelica_ProjectionMatrixInverse",
        "gl_NormalMatrix", "angelica_NormalMatrix"
    );

    private static final Path DUMP_DIR;
    private static final AtomicInteger dumpCounter = new AtomicInteger(0);

    static {
        DUMP_DIR = Boolean.parseBoolean(System.getProperty("angelica.dumpShaders", "false")) ? Paths.get("compat_shaders") : null;
    }

    private static final int CACHE_SIZE = 32;
    private record CacheKey(String source, boolean isFragment) {}
    private static final Map<CacheKey, String> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(32, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<CacheKey, String> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    public static void clearCache() {
        cache.clear();
    }

    /**
     * Transform a mod shader source for core profile compatibility.
     */
    public static String transform(String source, boolean isFragment) {
        final boolean needsTransform = needsTransformation(source);
        String result;
        if (!needsTransform) {
            result = fixupVersion(source);
        } else {
            final CacheKey key = new CacheKey(source, isFragment);
            final String cached = cache.get(key);
            if (cached != null) {
                dumpShader(source, cached, isFragment, needsTransform);
                return cached;
            }

            try {
                result = transformInternal(source, isFragment);
                cache.put(key, result);
            } catch (Exception e) {
                LOGGER.warn("CompatShaderTransformer: AST transformation failed, falling back to version fixup only", e);
                result = fixupVersion(source);
            }
        }

        dumpShader(source, result, isFragment, needsTransform);
        return result;
    }

    private static boolean needsTransformation(String source) {
        // Shaders already at 330+ core don't need compat transformation
        final Matcher vm = VERSION_PATTERN.matcher(source);
        if (vm.find()) {
            final int version = Integer.parseInt(vm.group(1));
            if (version >= 330 && "core".equals(vm.group(2))) return false;
        }

        return NEEDS_TRANSFORM_PATTERN.matcher(source).find();
    }

    private static String transformInternal(String source, boolean isFragment) {
        final Matcher versionMatcher = VERSION_PATTERN.matcher(source);
        int declaredVersion = 110;
        if (versionMatcher.find()) {
            declaredVersion = Integer.parseInt(versionMatcher.group(1));
        }

        final int targetVersion = Math.max(declaredVersion, RENDER_BACKEND != null ? RENDER_BACKEND.getMinGLSLVersion() : 330);

        source = stripGlesPrecisionGuards(source);
        final SeparatedSource separatedSource = separatePreprocessorPreamble(source);
        source = MAIN_VOID_PARAMETERS_PATTERN.matcher(separatedSource.shader()).replaceAll("main()");

        // Pre-parse reserved word renaming — prevents ANTLR parse failures
        source = GlslTransformUtils.replaceTexture(source);
        source = GlslTransformUtils.renameReservedWords(source, targetVersion);

        final ShaderParser.ParsedShader parsedShader = ShaderParser.parseShader(source);
        final Transformer transformer = new Transformer(parsedShader.full());

        injectMatrixUniforms(transformer);

        transformFog(transformer, isFragment, source);

        // gl_FrontLightModelProduct.sceneColor → angelica_SceneColor uniform
        if (source.contains("gl_FrontLightModelProduct")) {
            transformer.injectVariable("uniform vec4 angelica_SceneColor;");
            transformer.replaceExpression("gl_FrontLightModelProduct.sceneColor", "angelica_SceneColor");
        }

        // gl_LightSource[i] → struct + uniform array (both via injectFunction to keep struct before uniform)
        if (source.contains("gl_LightSource")) {
            transformer.injectFunction("struct angelica_LightSourceParameters {"
                    + "vec4 ambient;vec4 diffuse;vec4 specular;vec4 position;vec4 halfVector;"
                    + "vec3 spotDirection;float spotExponent;float spotCutoff;float spotCosCutoff;"
                    + "float constantAttenuation;float linearAttenuation;float quadraticAttenuation;"
                    + "};");
            transformer.injectFunction("uniform angelica_LightSourceParameters angelica_LightSource[2];");
            transformer.rename("gl_LightSource", "angelica_LightSource");
        }

        // gl_FrontMaterial → struct + uniform (both via injectFunction to keep struct before uniform)
        if (source.contains("gl_FrontMaterial")) {
            transformer.injectFunction("struct angelica_MaterialParameters {"
                    + "vec4 emission;vec4 ambient;vec4 diffuse;vec4 specular;float shininess;"
                    + "};");
            transformer.injectFunction("uniform angelica_MaterialParameters angelica_FrontMaterial;");
            transformer.rename("gl_FrontMaterial", "angelica_FrontMaterial");
        }

        // gl_FrontColor (vertex) → gl_Color (fragment) varying chain
        // Vertex side is unconditional: fragment may read gl_Color without vertex writing gl_FrontColor.
        // Uses angelica_ prefix to avoid colliding with user-declared varyings (e.g. "v_Color").
        if (!isFragment) {
            transformer.injectVariable("out vec4 angelica_FrontColor;");
            transformer.rename("gl_FrontColor", "angelica_FrontColor");
            transformer.prependMain("angelica_FrontColor = vec4(1.0);");

            // Vertex attributes — replaces removed FFP vertex inputs with explicit in declarations
            transformVertexAttributes(transformer, source);
        } else {
            if (source.contains("gl_Color")) {
                transformer.injectVariable("in vec4 angelica_FrontColor;");
                transformer.rename("gl_Color", "angelica_FrontColor");
            }
        }

        // gl_TexCoord[N] varying array → per-index in/out declarations
        final Set<Integer> texCoordIndices = new HashSet<>();
        transformer.renameArray("gl_TexCoord", "angelica_TexCoord", texCoordIndices);
        for (Integer i : texCoordIndices) {
            final String qualifier = isFragment ? "in" : "out";
            transformer.injectVariable(qualifier + " vec4 angelica_TexCoord" + i + ";");
        }

        // Fragment output handling + alpha test discard
        if (isFragment) {
            transformFragmentOutputs(transformer);
        }

        // texture-as-variable collision handling
        if (transformer.containsCall("texture") && transformer.hasVariable("texture")) {
            transformer.rename("texture", "gtexture");
        }
        if (transformer.hasVariable("angelica_renamed_texture")) {
            transformer.rename("angelica_renamed_texture", "gtexture");
        }

        transformer.renameFunctionCall(GlslTransformUtils.TEXTURE_RENAMES);

        transformer.renameAndWrapShadow("shadow2D", "texture");
        transformer.renameAndWrapShadow("shadow2DLod", "textureLod");

        final String versionDirective = "#version " + targetVersion + " core\n";
        final String preprocessor = separatedSource.preprocessor().trim();
        final String header = versionDirective + (preprocessor.isEmpty() ? "" : "\n" + preprocessor + "\n");
        final StringBuilder result = new StringBuilder();
        transformer.mutateTree(tree -> result.append(GlslTransformUtils.getFormattedShader(tree, header)));

        // Restore pre-parse renames
        String output = GlslTransformUtils.restoreReservedWords(result.toString());

        // Core profile: attribute → in, varying → out (vertex) / in (fragment)
        output = fixupQualifiers(output, isFragment);

        return output;
    }

    /**
     * Remove OpenGL ES default precision blocks that are invalid and unnecessary in desktop core shaders.
     * Other {@code GL_ES} conditionals are preserved because they can contain declarations with desktop alternatives.
     */
    private static String stripGlesPrecisionGuards(String source) {
        final String[] lines = source.split("\\R", -1);
        final StringBuilder result = new StringBuilder(source.length());

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            if (GLES_IFDEF_PATTERN.matcher(lines[lineIndex]).matches()) {
                int endIndex = lineIndex + 1;
                boolean foundPrecision = false;
                boolean onlyPrecision = true;

                while (endIndex < lines.length && !ENDIF_PATTERN.matcher(lines[endIndex]).matches()) {
                    final String line = lines[endIndex];
                    if (PRECISION_DECLARATION_PATTERN.matcher(line).matches()) {
                        foundPrecision = true;
                    } else if (!line.isBlank() && !line.stripLeading().startsWith("//")) {
                        onlyPrecision = false;
                    }
                    endIndex++;
                }

                if (foundPrecision && onlyPrecision && endIndex < lines.length) {
                    lineIndex = endIndex;
                    continue;
                }
            }

            result.append(lines[lineIndex]);
            if (lineIndex < lines.length - 1) {
                result.append('\n');
            }
        }

        return result.toString();
    }

    /**
     * Keep a leading directive-only preamble out of the GLSL AST parser. Moving directives after shader tokens, or
     * moving shader tokens out of a conditional block, would change preprocessing semantics and is therefore rejected.
     */
    private static SeparatedSource separatePreprocessorPreamble(String source) {
        final String[] lines = source.split("\\R", -1);
        final StringBuilder shader = new StringBuilder(source.length());
        final StringBuilder preprocessor = new StringBuilder();
        boolean continuation = false;
        boolean inBlockComment = false;
        boolean shaderStarted = false;
        boolean macroDirectiveSeen = false;
        int conditionalDepth = 0;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            final String line = lines[lineIndex];
            final LineClassification classification = classifyLine(line, inBlockComment);
            inBlockComment = classification.inBlockComment();

            if (continuation) {
                preprocessor.append(line).append('\n');
                continuation = classification.trailingBackslash();
            } else if (classification.directive()) {
                final String directiveName = directiveName(line, classification.directiveOffset());
                if (shaderStarted && !canRelocateLateMacro(
                    line,
                    classification.directiveOffset(),
                    directiveName,
                    shader,
                    macroDirectiveSeen
                )) {
                    throw unsafePreprocessorLayout(lineIndex, "directive appears after GLSL tokens");
                }
                if ("line".equals(directiveName)) {
                    throw unsafePreprocessorLayout(lineIndex, "#line cannot be relocated without changing line semantics");
                }
                if ("version".equals(directiveName)) {
                    if (conditionalDepth != 0) {
                        throw unsafePreprocessorLayout(lineIndex, "#version appears inside a conditional block");
                    }
                } else {
                    preprocessor.append(line).append('\n');
                }

                conditionalDepth = updateConditionalDepth(directiveName, conditionalDepth, lineIndex);
                macroDirectiveSeen |= isMacroDirective(directiveName);
                continuation = classification.trailingBackslash();
            } else {
                if (classification.shaderToken()) {
                    if (conditionalDepth != 0) {
                        throw unsafePreprocessorLayout(lineIndex, "conditional directive controls GLSL tokens");
                    }
                    shaderStarted = true;
                }
                shader.append(line);
            }

            if (lineIndex < lines.length - 1) {
                shader.append('\n');
            }
        }

        if (continuation) {
            throw unsafePreprocessorLayout(lines.length - 1, "unterminated directive continuation");
        }
        if (conditionalDepth != 0) {
            throw unsafePreprocessorLayout(lines.length - 1, "unterminated conditional directive");
        }

        return new SeparatedSource(shader.toString(), preprocessor.toString());
    }

    private static int updateConditionalDepth(String directiveName, int depth, int lineIndex) {
        if ("if".equals(directiveName) || "ifdef".equals(directiveName) || "ifndef".equals(directiveName)) {
            return depth + 1;
        }
        if ("else".equals(directiveName) || "elif".equals(directiveName)) {
            if (depth == 0) {
                throw unsafePreprocessorLayout(lineIndex, "conditional branch has no opening directive");
            }
            return depth;
        }
        if ("endif".equals(directiveName)) {
            if (depth == 0) {
                throw unsafePreprocessorLayout(lineIndex, "#endif has no opening directive");
            }
            return depth - 1;
        }
        return depth;
    }

    private static String directiveName(String line, int directiveOffset) {
        final Matcher matcher = DIRECTIVE_NAME_PATTERN.matcher(line.substring(directiveOffset + 1));
        return matcher.lookingAt() ? matcher.group(1).toLowerCase(Locale.ROOT) : "";
    }

    private static boolean canRelocateLateMacro(
        String line,
        int directiveOffset,
        String directiveName,
        CharSequence precedingShader,
        boolean macroDirectiveSeen
    ) {
        if (macroDirectiveSeen || !isMacroDirective(directiveName)) {
            return false;
        }

        final Matcher matcher = MACRO_NAME_PATTERN.matcher(line.substring(directiveOffset + 1));
        if (!matcher.lookingAt()) {
            return false;
        }

        final Pattern previousUse = Pattern.compile("\\b" + Pattern.quote(matcher.group(1)) + "\\b");
        return !previousUse.matcher(precedingShader).find();
    }

    private static boolean isMacroDirective(String directiveName) {
        return "define".equals(directiveName) || "undef".equals(directiveName);
    }

    private static IllegalArgumentException unsafePreprocessorLayout(int lineIndex, String reason) {
        return new IllegalArgumentException("Unsafe shader preprocessor layout at line " + (lineIndex + 1) + ": " + reason);
    }

    private static LineClassification classifyLine(String line, boolean startsInBlockComment) {
        boolean inBlockComment = startsInBlockComment;
        boolean inString = false;
        char quote = 0;
        boolean escaped = false;
        int directiveOffset = -1;
        int lastOutsideComment = -1;
        boolean shaderToken = false;

        for (int index = 0; index < line.length(); index++) {
            final char current = line.charAt(index);
            final char next = index + 1 < line.length() ? line.charAt(index + 1) : 0;

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    inString = false;
                }
                continue;
            }
            if (current == '/' && next == '/') {
                break;
            }
            if (current == '/' && next == '*') {
                inBlockComment = true;
                index++;
                continue;
            }
            if (current == '\"' || current == '\'') {
                inString = true;
                quote = current;
                continue;
            }
            if (!Character.isWhitespace(current)) {
                lastOutsideComment = index;
                if (directiveOffset < 0 && !shaderToken) {
                    if (current == '#') {
                        directiveOffset = index;
                    } else {
                        shaderToken = true;
                    }
                }
            }
        }

        final int lastNonWhitespace = lastNonWhitespace(line);
        final boolean trailingBackslash = lastNonWhitespace >= 0
            && lastNonWhitespace == lastOutsideComment
            && line.charAt(lastNonWhitespace) == '\\';
        return new LineClassification(directiveOffset >= 0, shaderToken, inBlockComment, trailingBackslash, directiveOffset);
    }

    private static int lastNonWhitespace(String line) {
        for (int index = line.length() - 1; index >= 0; index--) {
            if (!Character.isWhitespace(line.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private record SeparatedSource(String shader, String preprocessor) {}

    private record LineClassification(
        boolean directive,
        boolean shaderToken,
        boolean inBlockComment,
        boolean trailingBackslash,
        int directiveOffset
    ) {}

    /**
     * Inject matrix uniforms and rename compat builtins.
     */
    private static void injectMatrixUniforms(Transformer transformer) {
        transformer.injectVariable("uniform mat4 angelica_ModelViewMatrix;");
        transformer.injectVariable("uniform mat4 angelica_ModelViewMatrixInverse;");
        transformer.injectVariable("uniform mat4 angelica_ProjectionMatrix;");
        transformer.injectVariable("uniform mat4 angelica_ProjectionMatrixInverse;");
        transformer.injectVariable("uniform mat3 angelica_NormalMatrix;");
        transformer.injectVariable("uniform mat4 angelica_LightmapTextureMatrix;");

        transformer.rename(MATRIX_RENAMES);

        // Expression replacements
        transformer.replaceExpression("gl_ModelViewProjectionMatrix", "(angelica_ProjectionMatrix * angelica_ModelViewMatrix)");
        transformer.replaceExpression("gl_TextureMatrix[0]", "mat4(1.0)");
        transformer.replaceExpression("gl_TextureMatrix[1]", "angelica_LightmapTextureMatrix");
        transformer.replaceExpression(
                "gl_TextureMatrix",
                "mat4[8](mat4(1.0), angelica_LightmapTextureMatrix, mat4(1.0), mat4(1.0), mat4(1.0), mat4(1.0), mat4(1.0), mat4(1.0))");
    }

    /**
     * Transform fragment outputs for core profile.
     */
    private static void transformFragmentOutputs(Transformer transformer) {
        if (transformer.containsCall("gl_FragColor")) {
            transformer.replaceExpression("gl_FragColor", "gl_FragData[0]");
        }

        final Set<Integer> found = new HashSet<>();
        transformer.renameArray("gl_FragData", "angelica_FragData", found);

        for (Integer i : found) {
            transformer.injectVariable("layout (location = " + i + ") out vec4 angelica_FragData" + i + ";");
        }

        // Core profile: GL_ALPHA_TEST is removed - inject runtime discard using GLSM-tracked alpha reference (uploaded by CompatUniformManager).
        if (found.contains(0)) {
            transformer.injectVariable("uniform float angelica_currentAlphaTest;");
            transformer.appendMain("if (angelica_FragData0.a <= angelica_currentAlphaTest) discard;");
        }
    }

    private static void transformFog(Transformer transformer, boolean isFragment, String source) {
        // Vertex side is unconditional: fragment may read gl_FogFragCoord without vertex writing it
        transformer.rename("gl_FogFragCoord", "angelica_FogFragCoord");
        if (!isFragment) {
            transformer.injectVariable("out float angelica_FogFragCoord;");
            transformer.prependMain("angelica_FogFragCoord = 0.0;");
        } else {
            if (source.contains("gl_FogFragCoord")) {
                transformer.injectVariable("in float angelica_FogFragCoord;");
            }
        }

        transformer.rename("gl_Fog", "angelica_Fog");
        transformer.injectVariable("uniform float angelica_FogDensity;");
        transformer.injectVariable("uniform float angelica_FogStart;");
        transformer.injectVariable("uniform float angelica_FogEnd;");
        transformer.injectVariable("uniform vec4 angelica_FogColor;");
        transformer.injectFunction("struct angelica_FogParameters {vec4 color;float density;float start;float end;float scale;};");
        transformer.injectFunction("angelica_FogParameters angelica_Fog = angelica_FogParameters("
                + "angelica_FogColor, angelica_FogDensity, angelica_FogStart, angelica_FogEnd, "
                + "1.0 / (angelica_FogEnd - angelica_FogStart));");
    }

    /**
     * Replace removed FFP vertex attributes with explicit {@code in} declarations at core profile attribute locations.
     */
    private static void transformVertexAttributes(Transformer transformer, String source) {
        if (source.contains("gl_Vertex") || source.contains("ftransform")) {
            transformer.injectVariable("layout(location = 0) in vec4 angelica_Vertex;");
            transformer.rename("gl_Vertex", "angelica_Vertex");
        }
        // gl_Color in vertex shaders is the per-vertex color attribute, distinct from the fragment gl_Color (interpolated gl_FrontColor) handled above
        if (source.contains("gl_Color")) {
            transformer.injectVariable("layout(location = 1) in vec4 angelica_Color;");
            transformer.rename("gl_Color", "angelica_Color");
        }
        if (source.contains("gl_MultiTexCoord0")) {
            transformer.injectVariable("layout(location = 2) in vec4 angelica_MultiTexCoord0;");
            transformer.rename("gl_MultiTexCoord0", "angelica_MultiTexCoord0");
        }
        if (source.contains("gl_MultiTexCoord1")) {
            transformer.injectVariable("layout(location = 3) in vec4 angelica_MultiTexCoord1;");
            transformer.rename("gl_MultiTexCoord1", "angelica_MultiTexCoord1");
        }
        if (source.contains("gl_Normal")) {
            transformer.injectVariable("layout(location = 4) in vec3 angelica_Normal;");
            transformer.rename("gl_Normal", "angelica_Normal");
        }
        if (source.contains("ftransform")) {
            transformer.replaceExpression("ftransform()", "(angelica_ProjectionMatrix * angelica_ModelViewMatrix * angelica_Vertex)");
        }
    }

    private static void dumpShader(String original, String transformed, boolean isFragment, boolean wasTransformed) {
        if (DUMP_DIR == null) return;
        final int id = dumpCounter.getAndIncrement();
        final String suffix = isFragment ? ".frag.glsl" : ".vert.glsl";
        try {
            Files.createDirectories(DUMP_DIR);
            // Capture caller info for identification
            final String caller = identifyCaller();
            final String header = "// Compat shader dump #" + id + " (" + (isFragment ? "fragment" : "vertex") + ")"
                + "\n// Transformed: " + wasTransformed
                + "\n// Caller: " + caller + "\n\n";
            Files.writeString(DUMP_DIR.resolve(id + "_original" + suffix), header + original, StandardCharsets.UTF_8);
            Files.writeString(DUMP_DIR.resolve(id + "_transformed" + suffix), header + transformed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to dump compat shader: {}", e.getMessage());
        }
    }

    private static String identifyCaller() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            final String cls = frame.getClassName();
            if (!cls.startsWith("com.gtnewhorizons.angelica.glsm.")
                && !cls.startsWith("java.")
                && !cls.equals("org.lwjgl.opengl.GL20")) {
                return cls + "." + frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }
        return "unknown";
    }

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\battribute\\b");
    private static final Pattern VARYING_PATTERN = Pattern.compile("\\bvarying\\b");

    /** Replace legacy storage qualifiers removed in core profile. Safe on AST-serialized output (no comments). */
    public static String fixupQualifiers(String source, boolean isFragment) {
        source = ATTRIBUTE_PATTERN.matcher(source).replaceAll("in");
        source = VARYING_PATTERN.matcher(source).replaceAll(isFragment ? "in" : "out");
        return source;
    }

    // Patterns for parsing fragment shader in declarations to generate passthrough vertex shaders
    private static final Pattern FRAG_IN_COLOR = Pattern.compile("\\bin\\s+vec4\\s+angelica_FrontColor\\b");
    private static final Pattern FRAG_IN_TEXCOORD = Pattern.compile("\\bin\\s+vec4\\s+angelica_TexCoord(\\d+)\\b");
    private static final Pattern FRAG_IN_FOGCOORD = Pattern.compile("\\bin\\s+float\\s+angelica_FogFragCoord\\b");

    /**
     * Generate a passthrough vertex shader for a fragment-only program.
     *
     * @param fragmentSource the transformed fragment shader source
     * @return a complete vertex shader source string
     */
    public static String generatePassthroughVertexShader(String fragmentSource) {
        final StringBuilder sb = new StringBuilder(512);
        sb.append("#version 330 core\n\n");

        // Always have position
        sb.append("layout(location = 0) in vec4 a_Position;\n");

        // Matrix uniforms for position transform
        sb.append("uniform mat4 angelica_ModelViewMatrix;\n");
        sb.append("uniform mat4 angelica_ProjectionMatrix;\n");

        final StringBuilder varyings = new StringBuilder();
        final StringBuilder assignments = new StringBuilder();
        if (FRAG_IN_COLOR.matcher(fragmentSource).find()) {
            sb.append("layout(location = 1) in vec4 a_Color;\n");
            varyings.append("out vec4 angelica_FrontColor;\n");
            assignments.append("  angelica_FrontColor = a_Color;\n");
        }

        final Matcher texCoordMatcher = FRAG_IN_TEXCOORD.matcher(fragmentSource);
        final Set<Integer> texCoordIndices = new HashSet<>();
        while (texCoordMatcher.find()) {
            texCoordIndices.add(Integer.parseInt(texCoordMatcher.group(1)));
        }
        for (int i : texCoordIndices) {
            // PRIMARY_UV=2, SECONDARY_UV=3. Only indices 0 and 1 are supported; higher indices collide at location 3.
            final int location = i == 0 ? 2 : 3;
            sb.append("layout(location = ").append(location).append(") in vec4 a_TexCoord").append(i).append(";\n");
            varyings.append("out vec4 angelica_TexCoord").append(i).append(";\n");
            assignments.append("  angelica_TexCoord").append(i).append(" = a_TexCoord").append(i).append(";\n");
        }

        if (FRAG_IN_FOGCOORD.matcher(fragmentSource).find()) {
            varyings.append("out float angelica_FogFragCoord;\n");
            assignments.append("  angelica_FogFragCoord = abs((angelica_ModelViewMatrix * a_Position).z);\n");
        }

        sb.append(varyings);
        sb.append("\nvoid main() {\n");
        sb.append("  gl_Position = angelica_ProjectionMatrix * angelica_ModelViewMatrix * a_Position;\n");
        sb.append(assignments);
        sb.append("}\n");

        return sb.toString();
    }

    /** Ensure #version is at least 330 core, strip 'compatibility' profile. */
    private static String fixupVersion(String source) {
        final Matcher m = VERSION_PATTERN.matcher(source);
        if (!m.find()) {
            return "#version 330 core\n" + source;
        }

        final int version = Integer.parseInt(m.group(1));
        final String profile = m.group(2);

        if (version >= 330 && !"compatibility".equals(profile)) {
            if ("core".equals(profile)) return source;
            return m.replaceFirst("#version " + version + " core");
        }

        final int targetVersion = Math.max(version, 330);
        return m.replaceFirst("#version " + targetVersion + " core");
    }
}
