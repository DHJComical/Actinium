package com.gtnewhorizons.angelica.glsm;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlslTransformUtils {
    private static final String RENAMED_PREFIX = "actinium_renamed_";

    public static final Map<String, String> TEXTURE_RENAMES = Map.ofEntries(
        Map.entry("texture2D", "texture"),
        Map.entry("texture3D", "texture"),
        Map.entry("texture2DLod", "textureLod"),
        Map.entry("texture3DLod", "textureLod"),
        Map.entry("texture1DLod", "textureLod"),
        Map.entry("textureCubeLod", "textureLod"),
        Map.entry("texture1DArrayLod", "textureLod"),
        Map.entry("texture2DArrayLod", "textureLod"),
        Map.entry("texture2DProj", "textureProj"),
        Map.entry("texture3DProj", "textureProj"),
        Map.entry("texture1DProj", "textureProj"),
        Map.entry("texture2DRectProj", "textureProj"),
        Map.entry("texture2DGrad", "textureGrad"),
        Map.entry("texture2DGradARB", "textureGrad"),
        Map.entry("texture3DGrad", "textureGrad"),
        Map.entry("texelFetch2D", "texelFetch"),
        Map.entry("texelFetch3D", "texelFetch"),
        Map.entry("textureSize2D", "textureSize")
    );

    /**
     * Legacy texture builtins that the grammar lexes as keyword tokens but never accepts in
     * function-call position, so ANTLR silently recovers and the transformer would emit broken
     * GLSL. All of them map to the core {@code texture} builtin and are renamed pre-parse; their
     * Proj/Lod variants parse fine and are handled by {@link #TEXTURE_RENAMES} instead.
     */
    private static final List<ReservedWordRename> PRE_PARSE_TEXTURE_RENAMES = List.of(
        new ReservedWordRename(Pattern.compile("\\btexture1D\\b"), "texture"),
        new ReservedWordRename(Pattern.compile("\\btextureCube\\b"), "texture"),
        new ReservedWordRename(Pattern.compile("\\btexture2DRect\\b"), "texture"),
        new ReservedWordRename(Pattern.compile("\\btexture1DArray\\b"), "texture"),
        new ReservedWordRename(Pattern.compile("\\btexture2DArray\\b"), "texture"),
        new ReservedWordRename(Pattern.compile("\\btextureCubeArray\\b"), "texture")
    );

    private static final Pattern TEXTURE_PATTERN = Pattern.compile("\\btexture\\s*\\(|(\\btexture\\b)");

    private record ReservedWordRename(Pattern pattern, String replacement) {}

    private static final Map<Integer, List<ReservedWordRename>> VERSIONED_RESERVED_WORDS = Map.of(
        0, List.of(
            new ReservedWordRename(Pattern.compile("\\bsample\\b"), RENAMED_PREFIX + "sample"),
            new ReservedWordRename(Pattern.compile("\\bnew\\b"), RENAMED_PREFIX + "new")
        ),
        400, List.of(new ReservedWordRename(Pattern.compile("\\bsampler\\b(?!\\d)"), RENAMED_PREFIX + "sampler"))
    );

    public static String replaceTexture(String input) {
        Matcher matcher = TEXTURE_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                matcher.appendReplacement(builder, RENAMED_PREFIX + "texture");
            } else {
                matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * Rename legacy texture builtins the parser rejects in call position to the core
     * {@code texture} builtin. Must run after {@link #replaceTexture} so the {@code texture}
     * calls introduced here are not mistaken for texture-as-variable usage.
     */
    public static String renameParseBreakingTextureFunctions(String source) {
        for (var rename : PRE_PARSE_TEXTURE_RENAMES) {
            source = rename.pattern().matcher(source).replaceAll(rename.replacement());
        }
        return source;
    }

    public static String renameReservedWords(String source, int targetVersion) {
        for (var entry : VERSIONED_RESERVED_WORDS.entrySet()) {
            if (targetVersion >= entry.getKey()) {
                for (var rename : entry.getValue()) {
                    source = rename.pattern().matcher(source).replaceAll(rename.replacement());
                }
            }
        }
        return source;
    }

    public static String restoreReservedWords(String source) {
        source = source.replace(RENAMED_PREFIX + "texture", "texture");
        source = source.replace(RENAMED_PREFIX + "sampler", "sampler");
        source = source.replace(RENAMED_PREFIX + "sample", "sample");
        source = source.replace(RENAMED_PREFIX + "new", "new");
        return source;
    }

    public static String getFormattedShader(ParseTree tree, String header) {
        StringBuilder sb = new StringBuilder(header + "\n");
        String[] tabHolder = {""};
        getFormattedShader(tree, sb, tabHolder);
        return sb.toString();
    }

    private static void getFormattedShader(ParseTree tree, StringBuilder builder, String[] tabHolder) {
        if (tree instanceof TerminalNode) {
            String text = tree.getText();
            if ("<EOF>".equals(text)) {
                return;
            }
            if ("#".equals(text)) {
                builder.append("\n#");
                return;
            }
            builder.append(text);
            if ("{".equals(text)) {
                builder.append(" \n\t");
                tabHolder[0] = "\t";
            }
            if ("}".equals(text)) {
                if (builder.length() >= 2) {
                    builder.deleteCharAt(builder.length() - 2);
                }
                tabHolder[0] = "";
                builder.append(" \n");
            } else {
                builder.append(";".equals(text) ? " \n" + tabHolder[0] : " ");
            }
            return;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            getFormattedShader(tree.getChild(i), builder, tabHolder);
        }
    }
}
