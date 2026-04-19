package com.dhj.actinium.shader.options;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActiniumShaderOption {
    private final String name;
    private final boolean booleanOption;
    private final boolean defaultBooleanValue;
    private final @Nullable String defaultValue;
    private final List<String> allowedValues;
    private final List<String> sourcePaths;
    private final @Nullable String sourceComment;
    private final boolean slider;

    public ActiniumShaderOption(String name, boolean booleanOption, boolean defaultBooleanValue, @Nullable String defaultValue,
                                List<String> allowedValues, List<String> sourcePaths, @Nullable String sourceComment,
                                boolean slider) {
        this.name = name;
        this.booleanOption = booleanOption;
        this.defaultBooleanValue = defaultBooleanValue;
        this.defaultValue = defaultValue;
        this.allowedValues = Collections.unmodifiableList(new ArrayList<>(allowedValues));
        this.sourcePaths = Collections.unmodifiableList(new ArrayList<>(sourcePaths));
        this.sourceComment = sourceComment;
        this.slider = slider;
    }

    public String name() {
        return this.name;
    }

    public boolean isBooleanOption() {
        return this.booleanOption;
    }

    public boolean getDefaultBooleanValue() {
        return this.defaultBooleanValue;
    }

    public @Nullable String getDefaultValue() {
        return this.defaultValue;
    }

    public String getDefaultSerializedValue() {
        return this.booleanOption ? Boolean.toString(this.defaultBooleanValue) : (this.defaultValue != null ? this.defaultValue : "");
    }

    public List<String> getAllowedValues() {
        return this.allowedValues;
    }

    public List<String> getSourcePaths() {
        return this.sourcePaths;
    }

    public @Nullable String getSourceComment() {
        return this.sourceComment;
    }

    public boolean isSlider() {
        return this.slider;
    }

    public boolean isDefaultValue(String serializedValue) {
        return this.getDefaultSerializedValue().equals(serializedValue);
    }

    public boolean acceptsValue(String serializedValue) {
        if (this.booleanOption) {
            return "true".equals(serializedValue) || "false".equals(serializedValue);
        }

        return this.allowedValues.isEmpty() || this.allowedValues.contains(serializedValue);
    }

    public boolean matchesStoredKey(String key) {
        if (this.name.equals(key)) {
            return true;
        }

        for (String sourcePath : this.sourcePaths) {
            if ((sourcePath + "::" + this.name).equals(key)) {
                return true;
            }
        }

        return false;
    }
}
