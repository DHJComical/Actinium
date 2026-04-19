package com.dhj.actinium.shader.options;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ActiniumShaderProfile {
    private final String name;
    private final Map<String, String> optionValues;

    public ActiniumShaderProfile(String name, Map<String, String> optionValues) {
        this.name = name;
        this.optionValues = Map.copyOf(new LinkedHashMap<>(optionValues));
    }

    public String name() {
        return this.name;
    }

    public Map<String, String> optionValues() {
        return this.optionValues;
    }
}
