package com.dhj.actinium.shader.options;

import com.dhj.actinium.shader.pack.ActiniumShaderPackResources;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ActiniumShaderOptionParser {
    private ActiniumShaderOptionParser() {
    }

    public static List<ActiniumShaderOption> parseOptions(ActiniumShaderPackResources resources, Set<String> sliderOptions) {
        Map<String, ParsedAggregate> merged = new LinkedHashMap<>();

        for (String relativePath : resources.findShaderOptionFiles()) {
            byte[] bytes = resources.readResourceBytes(relativePath);

            if (bytes == null || bytes.length == 0) {
                continue;
            }

            String source = new String(bytes, StandardCharsets.UTF_8);

            for (String line : source.split("\\R", -1)) {
                ParsedOption parsed = tryParse(relativePath, line);

                if (parsed == null) {
                    continue;
                }

                ParsedAggregate aggregate = merged.get(parsed.name);

                if (aggregate == null) {
                    merged.put(parsed.name, new ParsedAggregate(parsed, sliderOptions.contains(parsed.name)));
                    continue;
                }

                if (aggregate.isCompatible(parsed)) {
                    aggregate.merge(parsed);
                }
            }
        }

        List<ActiniumShaderOption> options = new ArrayList<>(merged.size());
        merged.values().forEach(aggregate -> options.add(aggregate.build()));
        options.sort(Comparator.comparing(ActiniumShaderOption::name));
        return options;
    }

    public static String applyOverride(String relativePath, String line, Map<String, String> overrides) {
        ParsedOption parsed = tryParse(relativePath, line);

        if (parsed == null) {
            return line;
        }

        String overrideValue = lookupOverride(relativePath, parsed.name, overrides);

        if (overrideValue == null) {
            return line;
        }

        return switch (parsed.syntax) {
            case BOOLEAN_DEFINE -> renderBooleanDefine(parsed, overrideValue);
            case VALUE_DEFINE -> renderValueDefine(parsed, overrideValue);
            case BOOLEAN_CONST -> renderBooleanConst(parsed, overrideValue);
            case VALUE_CONST -> renderValueConst(parsed, overrideValue);
        };
    }

    private static @Nullable ParsedOption tryParse(String relativePath, String line) {
        ParsedOption parsed = parseDefine(relativePath, line);

        if (parsed != null) {
            return parsed;
        }

        return parseConst(relativePath, line);
    }

    private static @Nullable ParsedOption parseDefine(String relativePath, String line) {
        String indent = leadingWhitespace(line);
        String trimmed = line.trim();
        boolean enabledByDefault = true;

        if (trimmed.startsWith("//")) {
            String uncommented = trimmed.substring(2).trim();

            if (!uncommented.startsWith("#define ")) {
                return null;
            }

            trimmed = uncommented;
            enabledByDefault = false;
        }

        if (!trimmed.startsWith("#define ")) {
            return null;
        }

        String remainder = trimmed.substring("#define ".length()).trim();

        if (remainder.isEmpty()) {
            return null;
        }

        int commentIndex = remainder.indexOf("//");
        String definition = commentIndex >= 0 ? remainder.substring(0, commentIndex).trim() : remainder.trim();
        String commentBody = commentIndex >= 0 ? remainder.substring(commentIndex + 2).trim() : null;
        String commentSuffix = commentIndex >= 0 ? " //" + remainder.substring(commentIndex + 2).trim() : "";

        if (definition.isEmpty()) {
            return null;
        }

        int valueStart = findWhitespace(definition);
        String name = valueStart >= 0 ? definition.substring(0, valueStart).trim() : definition;
        String value = valueStart >= 0 ? definition.substring(valueStart).trim() : null;

        if (name.isEmpty() || name.contains("(")) {
            return null;
        }

        if (value == null || value.isEmpty()) {
            return new ParsedOption(
                    relativePath,
                    name,
                    Syntax.BOOLEAN_DEFINE,
                    indent,
                    enabledByDefault,
                    null,
                    List.of(),
                    commentBody,
                    commentSuffix,
                    null
            );
        }

        List<String> allowedValues = parseAllowedValues(commentBody);

        if (allowedValues.isEmpty()) {
            return null;
        }

        return new ParsedOption(
                relativePath,
                name,
                Syntax.VALUE_DEFINE,
                indent,
                enabledByDefault,
                value,
                allowedValues,
                stripAllowedValues(commentBody),
                commentSuffix,
                null
        );
    }

    private static @Nullable ParsedOption parseConst(String relativePath, String line) {
        String indent = leadingWhitespace(line);
        String trimmed = line.trim();

        if (!trimmed.startsWith("const ")) {
            return null;
        }

        int equalsIndex = trimmed.indexOf('=');
        int semicolonIndex = trimmed.indexOf(';', equalsIndex + 1);

        if (equalsIndex < 0 || semicolonIndex < 0) {
            return null;
        }

        String declaration = trimmed.substring(0, equalsIndex).trim();
        String[] declarationParts = declaration.split("\\s+");

        if (declarationParts.length != 3) {
            return null;
        }

        String type = declarationParts[1];
        String name = declarationParts[2];

        if (name.isEmpty()) {
            return null;
        }

        String value = trimmed.substring(equalsIndex + 1, semicolonIndex).trim();
        String remainder = trimmed.substring(semicolonIndex + 1).trim();
        String commentBody = null;
        String commentSuffix = "";

        if (remainder.startsWith("//")) {
            commentBody = remainder.substring(2).trim();
            commentSuffix = " " + remainder;
        } else if (!remainder.isEmpty()) {
            return null;
        }

        if ("bool".equals(type)) {
            if (!"true".equals(value) && !"false".equals(value)) {
                return null;
            }

            return new ParsedOption(
                    relativePath,
                    name,
                    Syntax.BOOLEAN_CONST,
                    indent,
                    Boolean.parseBoolean(value),
                    null,
                    List.of(),
                    commentBody,
                    commentSuffix,
                    type
            );
        }

        if (!"int".equals(type) && !"float".equals(type)) {
            return null;
        }

        List<String> allowedValues = parseAllowedValues(commentBody);

        if (allowedValues.isEmpty()) {
            return null;
        }

        return new ParsedOption(
                relativePath,
                name,
                Syntax.VALUE_CONST,
                indent,
                true,
                value,
                allowedValues,
                stripAllowedValues(commentBody),
                commentSuffix,
                type
        );
    }

    private static String renderBooleanDefine(ParsedOption parsed, String overrideValue) {
        StringBuilder builder = new StringBuilder(parsed.indent);

        if (!Boolean.parseBoolean(overrideValue)) {
            builder.append("// ");
        }

        builder.append("#define ").append(parsed.name);

        if (!parsed.commentSuffix.isEmpty()) {
            builder.append(parsed.commentSuffix);
        }

        return builder.toString();
    }

    private static String renderValueDefine(ParsedOption parsed, String overrideValue) {
        StringBuilder builder = new StringBuilder(parsed.indent);
        builder.append("#define ").append(parsed.name).append(' ').append(overrideValue);

        if (!parsed.commentSuffix.isEmpty()) {
            builder.append(parsed.commentSuffix);
        }

        return builder.toString();
    }

    private static String renderBooleanConst(ParsedOption parsed, String overrideValue) {
        StringBuilder builder = new StringBuilder(parsed.indent);
        builder.append("const ").append(parsed.valueType).append(' ')
                .append(parsed.name).append(" = ").append(Boolean.parseBoolean(overrideValue)).append(';');

        if (!parsed.commentSuffix.isEmpty()) {
            builder.append(parsed.commentSuffix);
        }

        return builder.toString();
    }

    private static String renderValueConst(ParsedOption parsed, String overrideValue) {
        StringBuilder builder = new StringBuilder(parsed.indent);
        builder.append("const ").append(parsed.valueType).append(' ')
                .append(parsed.name).append(" = ").append(overrideValue).append(';');

        if (!parsed.commentSuffix.isEmpty()) {
            builder.append(parsed.commentSuffix);
        }

        return builder.toString();
    }

    private static @Nullable String lookupOverride(String relativePath, String name, Map<String, String> overrides) {
        String direct = overrides.get(name);

        if (direct != null) {
            return direct;
        }

        return overrides.get(relativePath.replace('\\', '/') + "::" + name);
    }

    private static List<String> parseAllowedValues(@Nullable String comment) {
        if (comment == null) {
            return List.of();
        }

        int start = comment.indexOf('[');
        int end = comment.indexOf(']');

        if (start < 0 || end <= start + 1) {
            return List.of();
        }

        String body = comment.substring(start + 1, end).trim();

        if (body.isEmpty()) {
            return List.of();
        }

        String[] split = body.split("\\s+");
        List<String> values = new ArrayList<>(split.length);

        for (String value : split) {
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }

        return values;
    }

    private static @Nullable String stripAllowedValues(@Nullable String comment) {
        if (comment == null) {
            return null;
        }

        int start = comment.indexOf('[');
        int end = comment.indexOf(']');

        if (start < 0 || end <= start) {
            return comment.isBlank() ? null : comment;
        }

        String stripped = (comment.substring(0, start) + comment.substring(end + 1)).trim();
        return stripped.isBlank() ? null : stripped;
    }

    private static String leadingWhitespace(String line) {
        int index = 0;

        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }

        return line.substring(0, index);
    }

    private static int findWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }

        return -1;
    }

    private enum Syntax {
        BOOLEAN_DEFINE,
        VALUE_DEFINE,
        BOOLEAN_CONST,
        VALUE_CONST
    }

    private static final class ParsedOption {
        private final String sourcePath;
        private final String name;
        private final Syntax syntax;
        private final String indent;
        private final boolean defaultBooleanValue;
        private final @Nullable String defaultValue;
        private final List<String> allowedValues;
        private final @Nullable String comment;
        private final String commentSuffix;
        private final @Nullable String valueType;

        private ParsedOption(String sourcePath, String name, Syntax syntax, String indent, boolean defaultBooleanValue,
                             @Nullable String defaultValue, List<String> allowedValues, @Nullable String comment,
                             String commentSuffix, @Nullable String valueType) {
            this.sourcePath = sourcePath;
            this.name = name;
            this.syntax = syntax;
            this.indent = indent;
            this.defaultBooleanValue = defaultBooleanValue;
            this.defaultValue = defaultValue;
            this.allowedValues = new ArrayList<>(allowedValues);
            this.comment = comment;
            this.commentSuffix = commentSuffix;
            this.valueType = valueType;
        }
    }

    private static final class ParsedAggregate {
        private final String name;
        private final boolean booleanOption;
        private final boolean defaultBooleanValue;
        private final @Nullable String defaultValue;
        private final List<String> allowedValues;
        private final Set<String> sourcePaths = new LinkedHashSet<>();
        private @Nullable String sourceComment;
        private boolean slider;

        private ParsedAggregate(ParsedOption parsed, boolean slider) {
            this.name = parsed.name;
            this.booleanOption = parsed.syntax == Syntax.BOOLEAN_DEFINE || parsed.syntax == Syntax.BOOLEAN_CONST;
            this.defaultBooleanValue = parsed.defaultBooleanValue;
            this.defaultValue = parsed.defaultValue;
            this.allowedValues = new ArrayList<>(parsed.allowedValues);
            this.sourcePaths.add(parsed.sourcePath);
            this.sourceComment = parsed.comment;
            this.slider = slider;
        }

        private boolean isCompatible(ParsedOption parsed) {
            boolean parsedBoolean = parsed.syntax == Syntax.BOOLEAN_DEFINE || parsed.syntax == Syntax.BOOLEAN_CONST;

            if (this.booleanOption != parsedBoolean) {
                return false;
            }

            if (this.booleanOption) {
                return this.defaultBooleanValue == parsed.defaultBooleanValue;
            }

            return Objects.equals(this.defaultValue, parsed.defaultValue)
                    && this.allowedValues.equals(parsed.allowedValues);
        }

        private void merge(ParsedOption parsed) {
            this.sourcePaths.add(parsed.sourcePath);

            if ((this.sourceComment == null || this.sourceComment.isBlank()) && parsed.comment != null && !parsed.comment.isBlank()) {
                this.sourceComment = parsed.comment;
            }
        }

        private ActiniumShaderOption build() {
            return new ActiniumShaderOption(
                    this.name,
                    this.booleanOption,
                    this.defaultBooleanValue,
                    this.defaultValue,
                    this.allowedValues,
                    new ArrayList<>(this.sourcePaths),
                    this.sourceComment,
                    this.slider
            );
        }
    }
}
