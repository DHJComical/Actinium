package com.gtnewhorizons.angelica.glsm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompatShaderTransformer {
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\battribute\\b");
    private static final Pattern VARYING_PATTERN = Pattern.compile("\\bvarying\\b");

    private CompatShaderTransformer() {
    }

    public static void clearCache() {
    }

    public static String transform(String source, boolean isFragment) {
        return fixupQualifiers(source, isFragment);
    }

    public static String fixupQualifiers(String source, boolean isFragment) {
        String result = ATTRIBUTE_PATTERN.matcher(source).replaceAll("in");
        Matcher varying = VARYING_PATTERN.matcher(result);
        return varying.replaceAll(isFragment ? "in" : "out");
    }
}
