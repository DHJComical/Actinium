package com.dhj.actinium.shader.transform;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActiniumGlslTransformUtils {
    public static final Map<String, String> TEXTURE_RENAMES = Map.ofEntries(
            Map.entry("texture2D", "texture"),
            Map.entry("texture2DLod", "textureLod")
    );

    private static final String RENAMED_PREFIX = "actinium_renamed_";
    private static final Pattern TEXTURE_PATTERN = Pattern.compile("\\btexture\\s*\\(|(\\btexture\\b)");

    private ActiniumGlslTransformUtils() {
    }

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

    public static String restoreReservedWords(String source) {
        return source.replace(RENAMED_PREFIX + "texture", "texture");
    }

    public static String getFormattedShader(ParseTree tree, String header) {
        StringBuilder sb = new StringBuilder(header);
        if (!header.isEmpty() && !header.endsWith("\n")) {
            sb.append('\n');
        }
        String[] indent = {""};
        format(tree, sb, indent);
        return sb.toString();
    }

    private static void format(ParseTree tree, StringBuilder sb, String[] indent) {
        if (tree instanceof TerminalNode) {
            String text = tree.getText();
            if ("<EOF>".equals(text)) {
                return;
            }
            if ("#".equals(text)) {
                sb.append("\n#");
                return;
            }
            sb.append(text);
            if ("{".equals(text)) {
                sb.append(" \n\t");
                indent[0] = "\t";
            } else if ("}".equals(text)) {
                if (sb.length() >= 2) {
                    sb.deleteCharAt(sb.length() - 2);
                }
                indent[0] = "";
                sb.append(" \n");
            } else {
                sb.append(";".equals(text) ? " \n" + indent[0] : " ");
            }
            return;
        }

        for (int i = 0; i < tree.getChildCount(); i++) {
            format(tree.getChild(i), sb, indent);
        }
    }
}
