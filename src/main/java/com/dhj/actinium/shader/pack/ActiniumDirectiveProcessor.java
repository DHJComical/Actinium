package com.dhj.actinium.shader.pack;

import com.dhj.actinium.celeritas.ActiniumShaders;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

final class ActiniumDirectiveProcessor {
    private static final String[] COMPARISON_OPERATORS = {"==", "!=", ">=", "<=", ">", "<"};
    private static final Map<String, String> STATIC_DEFINES = createStaticDefines();

    private ActiniumDirectiveProcessor() {
    }

    public static Properties loadPropertiesFile(Path path, String logicalName, Iterable<String> directiveSources) {
        return loadPropertiesFile(path, logicalName, directiveSources, Map.of());
    }

    public static Properties loadPropertiesFile(Path path, String logicalName, Iterable<String> directiveSources, Map<String, String> optionOverrides) {
        Properties properties = new Properties();

        if (!Files.isRegularFile(path)) {
            return properties;
        }

        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String processed = preprocess(source, collectDefines(directiveSources, optionOverrides));
            properties.load(new StringReader(processed));
        } catch (IOException e) {
            ActiniumShaders.logger().warn("Failed to read shader pack file {} ({})", logicalName, path, e);
        }

        return properties;
    }

    private static String preprocess(String source, Map<String, String> baseDefines) {
        Map<String, String> defines = new HashMap<>(baseDefines);
        ArrayList<ConditionalFrame> stack = new ArrayList<>();
        StringBuilder output = new StringBuilder(source.length());
        boolean active = true;

        for (String rawLine : source.split("\\R", -1)) {
            String trimmed = stripInlineComment(rawLine).trim();

            if (trimmed.startsWith("#ifdef ")) {
                String name = trimmed.substring("#ifdef ".length()).trim();
                boolean condition = defines.containsKey(name);
                stack.add(new ConditionalFrame(active, condition, active && condition));
                active = active && condition;
                continue;
            }

            if (trimmed.startsWith("#ifndef ")) {
                String name = trimmed.substring("#ifndef ".length()).trim();
                boolean condition = !defines.containsKey(name);
                stack.add(new ConditionalFrame(active, condition, active && condition));
                active = active && condition;
                continue;
            }

            if (trimmed.startsWith("#if ")) {
                boolean condition = evaluateExpression(trimmed.substring("#if ".length()).trim(), defines);
                stack.add(new ConditionalFrame(active, condition, active && condition));
                active = active && condition;
                continue;
            }

            if (trimmed.startsWith("#elif ")) {
                if (!stack.isEmpty()) {
                    ConditionalFrame frame = stack.get(stack.size() - 1);
                    boolean condition = !frame.branchTaken && evaluateExpression(trimmed.substring("#elif ".length()).trim(), defines);
                    frame.branchTaken |= condition;
                    frame.active = frame.parentActive && condition;
                    active = frame.active;
                }
                continue;
            }

            if (trimmed.startsWith("#else")) {
                if (!stack.isEmpty()) {
                    ConditionalFrame frame = stack.get(stack.size() - 1);
                    boolean condition = !frame.branchTaken;
                    frame.branchTaken = true;
                    frame.active = frame.parentActive && condition;
                    active = frame.active;
                }
                continue;
            }

            if (trimmed.startsWith("#endif")) {
                if (!stack.isEmpty()) {
                    ConditionalFrame frame = stack.remove(stack.size() - 1);
                    active = frame.parentActive;
                }
                continue;
            }

            if (!active) {
                continue;
            }

            if (trimmed.startsWith("#define ")) {
                applyDefine(trimmed, defines);
                continue;
            }

            if (trimmed.startsWith("#undef ")) {
                defines.remove(trimmed.substring("#undef ".length()).trim());
                continue;
            }

            output.append(rawLine).append('\n');
        }

        return output.toString();
    }

    private static Map<String, String> collectDefines(Iterable<String> directiveSources, Map<String, String> optionOverrides) {
        Map<String, String> defines = new LinkedHashMap<>(STATIC_DEFINES);

        for (String shaderSource : directiveSources) {
            if (shaderSource == null || shaderSource.isBlank()) {
                continue;
            }

            parseDefines(shaderSource, defines);
        }

        optionOverrides.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }

            String trimmedKey = key.trim();
            String trimmedValue = value.trim();

            if (trimmedKey.isEmpty() || trimmedKey.contains("::")) {
                return;
            }

            if ("true".equalsIgnoreCase(trimmedValue)) {
                defines.put(trimmedKey, "1");
            } else if ("false".equalsIgnoreCase(trimmedValue)) {
                defines.remove(trimmedKey);
                defines.put(trimmedKey, "0");
            } else if (!trimmedValue.isEmpty()) {
                defines.put(trimmedKey, trimmedValue);
            }
        });

        return defines;
    }

    private static void parseDefines(String source, Map<String, String> defines) {
        ArrayList<ConditionalFrame> stack = new ArrayList<>();
        boolean active = true;

        for (String rawLine : source.split("\\R")) {
            String trimmed = stripInlineComment(rawLine).trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("#ifdef ")) {
                String name = trimmed.substring("#ifdef ".length()).trim();
                boolean condition = defines.containsKey(name);
                stack.add(new ConditionalFrame(active, condition, active && condition));
                active = active && condition;
                continue;
            }

            if (trimmed.startsWith("#ifndef ")) {
                String name = trimmed.substring("#ifndef ".length()).trim();
                boolean condition = !defines.containsKey(name);
                stack.add(new ConditionalFrame(active, condition, active && condition));
                active = active && condition;
                continue;
            }

            if (trimmed.startsWith("#if ")) {
                boolean condition = evaluateExpression(trimmed.substring("#if ".length()).trim(), defines);
                stack.add(new ConditionalFrame(active, condition, active && condition));
                active = active && condition;
                continue;
            }

            if (trimmed.startsWith("#elif ")) {
                if (!stack.isEmpty()) {
                    ConditionalFrame frame = stack.get(stack.size() - 1);
                    boolean condition = !frame.branchTaken && evaluateExpression(trimmed.substring("#elif ".length()).trim(), defines);
                    frame.branchTaken |= condition;
                    frame.active = frame.parentActive && condition;
                    active = frame.active;
                }
                continue;
            }

            if (trimmed.startsWith("#else")) {
                if (!stack.isEmpty()) {
                    ConditionalFrame frame = stack.get(stack.size() - 1);
                    boolean condition = !frame.branchTaken;
                    frame.branchTaken = true;
                    frame.active = frame.parentActive && condition;
                    active = frame.active;
                }
                continue;
            }

            if (trimmed.startsWith("#endif")) {
                if (!stack.isEmpty()) {
                    ConditionalFrame frame = stack.remove(stack.size() - 1);
                    active = frame.parentActive;
                }
                continue;
            }

            if (!active) {
                continue;
            }

            if (trimmed.startsWith("#define ")) {
                applyDefine(trimmed, defines);
            } else if (trimmed.startsWith("#undef ")) {
                defines.remove(trimmed.substring("#undef ".length()).trim());
            }
        }
    }

    private static void applyDefine(String trimmed, Map<String, String> defines) {
        int valueStart = trimmed.indexOf(' ', "#define ".length());
        String name;
        String value;

        if (valueStart >= 0) {
            name = trimmed.substring("#define ".length(), valueStart).trim();
            value = trimmed.substring(valueStart + 1).trim();
        } else {
            name = trimmed.substring("#define ".length()).trim();
            value = "1";
        }

        if (!name.isEmpty() && !name.contains("(")) {
            defines.put(name, value.isEmpty() ? "1" : value);
        }
    }

    private static Map<String, String> createStaticDefines() {
        Map<String, String> defines = new LinkedHashMap<>();
        defines.put("MC_VERSION", "11202");
        defines.put("MC_GLSL_VERSION", "120");
        defines.put("IS_IRIS", "1");
        return defines;
    }

    private static boolean evaluateExpression(String expression, Map<String, String> defines) {
        String trimmed = trimOuterParentheses(expression.trim());

        int operatorIndex = findTopLevelOperator(trimmed, "||");
        if (operatorIndex >= 0) {
            return evaluateExpression(trimmed.substring(0, operatorIndex), defines)
                    || evaluateExpression(trimmed.substring(operatorIndex + 2), defines);
        }

        operatorIndex = findTopLevelOperator(trimmed, "&&");
        if (operatorIndex >= 0) {
            return evaluateExpression(trimmed.substring(0, operatorIndex), defines)
                    && evaluateExpression(trimmed.substring(operatorIndex + 2), defines);
        }

        if (trimmed.startsWith("!")) {
            return !evaluateExpression(trimmed.substring(1), defines);
        }

        if (trimmed.startsWith("defined(") && trimmed.endsWith(")")) {
            return defines.containsKey(trimmed.substring("defined(".length(), trimmed.length() - 1).trim());
        }

        for (String operator : COMPARISON_OPERATORS) {
            operatorIndex = findTopLevelOperator(trimmed, operator);
            if (operatorIndex >= 0) {
                double left = resolveNumeric(trimmed.substring(0, operatorIndex), defines);
                double right = resolveNumeric(trimmed.substring(operatorIndex + operator.length()), defines);
                return switch (operator) {
                    case "==" -> left == right;
                    case "!=" -> left != right;
                    case ">=" -> left >= right;
                    case "<=" -> left <= right;
                    case ">" -> left > right;
                    case "<" -> left < right;
                    default -> false;
                };
            }
        }

        return resolveTruthy(trimmed, defines);
    }

    private static int findTopLevelOperator(String expression, String operator) {
        int depth = 0;

        for (int i = 0; i <= expression.length() - operator.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }

            if (depth == 0 && expression.startsWith(operator, i)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean resolveTruthy(String token, Map<String, String> defines) {
        String resolved = resolveToken(token.trim(), defines);

        if (resolved.isEmpty()) {
            return false;
        }

        if ("true".equalsIgnoreCase(resolved)) {
            return true;
        }

        if ("false".equalsIgnoreCase(resolved)) {
            return false;
        }

        try {
            return Float.parseFloat(resolved) != 0.0f;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static double resolveNumeric(String token, Map<String, String> defines) {
        String resolved = resolveToken(token.trim(), defines);

        if ("true".equalsIgnoreCase(resolved)) {
            return 1.0;
        }

        if ("false".equalsIgnoreCase(resolved)) {
            return 0.0;
        }

        try {
            return Double.parseDouble(resolved);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static String resolveToken(String token, Map<String, String> defines) {
        String trimmed = trimOuterParentheses(token.trim());
        String resolved = defines.get(trimmed);
        return resolved != null ? resolved.trim() : trimmed;
    }

    private static String stripInlineComment(String line) {
        int commentStart = line.indexOf("//");
        return commentStart >= 0 ? line.substring(0, commentStart) : line;
    }

    private static String trimOuterParentheses(String value) {
        String trimmed = value.trim();

        while (trimmed.startsWith("(") && trimmed.endsWith(")") && hasBalancedOuterParentheses(trimmed)) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        return trimmed;
    }

    private static boolean hasBalancedOuterParentheses(String value) {
        int depth = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0 && i < value.length() - 1) {
                    return false;
                }
            }
        }

        return depth == 0;
    }

    private static final class ConditionalFrame {
        private final boolean parentActive;
        private boolean branchTaken;
        private boolean active;

        private ConditionalFrame(boolean parentActive, boolean branchTaken, boolean active) {
            this.parentActive = parentActive;
            this.branchTaken = branchTaken;
            this.active = active;
        }
    }
}
