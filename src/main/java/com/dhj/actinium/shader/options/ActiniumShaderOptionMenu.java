package com.dhj.actinium.shader.options;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ActiniumShaderOptionMenu {
    private final String packName;
    private final Map<String, ActiniumShaderOption> options;
    private final Map<String, String> translations;
    private final @Nullable List<String> mainScreenTokens;
    private final Map<String, List<String>> subScreenTokens;
    private final @Nullable Integer mainScreenColumns;
    private final Map<String, Integer> subScreenColumns;
    private final ActiniumShaderProfileSet profiles;
    private final ActiniumShaderProfileSet profiles2;

    public ActiniumShaderOptionMenu(String packName, List<ActiniumShaderOption> options, Map<String, String> translations,
                                    @Nullable List<String> mainScreenTokens, Map<String, List<String>> subScreenTokens,
                                    @Nullable Integer mainScreenColumns, Map<String, Integer> subScreenColumns,
                                    ActiniumShaderProfileSet profiles, ActiniumShaderProfileSet profiles2) {
        this.packName = packName;

        LinkedHashMap<String, ActiniumShaderOption> optionMap = new LinkedHashMap<>();
        options.forEach(option -> optionMap.put(option.name(), option));
        this.options = Collections.unmodifiableMap(optionMap);
        this.translations = Collections.unmodifiableMap(new LinkedHashMap<>(translations));
        this.mainScreenTokens = mainScreenTokens != null ? Collections.unmodifiableList(new ArrayList<>(mainScreenTokens)) : null;
        this.subScreenTokens = immutableListMap(subScreenTokens);
        this.mainScreenColumns = mainScreenColumns;
        this.subScreenColumns = Collections.unmodifiableMap(new LinkedHashMap<>(subScreenColumns));
        this.profiles = profiles;
        this.profiles2 = profiles2;
    }

    public String packName() {
        return this.packName;
    }

    public List<ActiniumShaderOption> getOptions() {
        return new ArrayList<>(this.options.values());
    }

    public @Nullable ActiniumShaderOption getOption(String name) {
        return this.options.get(name);
    }

    public ActiniumShaderProfileSet getProfiles() {
        return this.profiles;
    }

    public ActiniumShaderProfileSet getProfiles2() {
        return this.profiles2;
    }

    public List<String> getScreenTokens(@Nullable String screenId) {
        if (screenId == null) {
            return this.mainScreenTokens != null ? this.mainScreenTokens : List.of("*");
        }

        return this.subScreenTokens.getOrDefault(screenId, List.of());
    }

    public int getColumnCount(@Nullable String screenId) {
        if (screenId == null) {
            return this.mainScreenColumns != null ? Math.max(1, this.mainScreenColumns) : 2;
        }

        return Math.max(1, this.subScreenColumns.getOrDefault(screenId, 2));
    }

    public String getScreenLabel(@Nullable String screenId) {
        if (screenId == null) {
            return this.packName;
        }

        return this.translations.getOrDefault("screen." + screenId, screenId);
    }

    public @Nullable String getScreenComment(@Nullable String screenId) {
        if (screenId == null) {
            return null;
        }

        return this.translations.get("screen." + screenId + ".comment");
    }

    public String getOptionLabel(ActiniumShaderOption option) {
        return this.translations.getOrDefault("option." + option.name(), option.name());
    }

    public @Nullable String getOptionComment(ActiniumShaderOption option) {
        String translated = this.translations.get("option." + option.name() + ".comment");
        return translated != null ? translated : option.getSourceComment();
    }

    public @Nullable String getOptionComment(ActiniumShaderOption option, @Nullable String screenId) {
        String translated = this.translations.get("option." + option.name() + ".comment");

        if (translated != null && !translated.isBlank()) {
            return translated;
        }

        String screenComment = this.getScreenComment(screenId);

        if (screenComment != null && !screenComment.isBlank()) {
            return screenComment;
        }

        String ownerScreenId = this.findOwningScreenId(option.name());
        String ownerScreenComment = this.getScreenComment(ownerScreenId);

        if (ownerScreenComment != null && !ownerScreenComment.isBlank()) {
            return ownerScreenComment;
        }

        String sourceComment = option.getSourceComment();
        return sourceComment != null && !sourceComment.isBlank() ? sourceComment : null;
    }

    public String getValueLabel(ActiniumShaderOption option, String value) {
        return this.translations.getOrDefault("value." + option.name() + "." + value, value);
    }

    public String getProfileLabel(String profileName, boolean secondProfileSet) {
        String key = (secondProfileSet ? "profile2." : "profile.") + profileName;
        return this.translations.getOrDefault(key, profileName);
    }

    public @Nullable String getProfileComment(boolean secondProfileSet) {
        return this.translations.get(secondProfileSet ? "profile2.comment" : "profile.comment");
    }

    public List<String> getUnusedOptionIds() {
        Set<String> referenced = new LinkedHashSet<>();
        collectReferencedOptions(this.getScreenTokens(null), referenced);
        this.subScreenTokens.values().forEach(tokens -> collectReferencedOptions(tokens, referenced));

        List<String> unused = new ArrayList<>();

        for (String optionId : this.options.keySet()) {
            if (!referenced.contains(optionId)) {
                unused.add(optionId);
            }
        }

        unused.sort(Comparator.comparing(this::getComparableLabel));
        return unused;
    }

    public Map<String, String> normalizeOverrides(Map<String, String> storedOverrides) {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();

        for (ActiniumShaderOption option : this.options.values()) {
            String matchedValue = null;

            for (Map.Entry<String, String> entry : storedOverrides.entrySet()) {
                if (option.matchesStoredKey(entry.getKey())) {
                    matchedValue = entry.getValue();
                    break;
                }
            }

            if (matchedValue != null && !option.isDefaultValue(matchedValue)) {
                normalized.put(option.name(), matchedValue);
            }
        }

        return normalized;
    }

    private String getComparableLabel(String optionId) {
        ActiniumShaderOption option = this.options.get(optionId);
        return option != null ? this.getOptionLabel(option).toLowerCase() : optionId.toLowerCase();
    }

    private void collectReferencedOptions(List<String> tokens, Set<String> referenced) {
        for (String token : tokens) {
            if (token.isBlank() || "*".equals(token) || "<empty>".equals(token) || "<profile>".equals(token) || "<profile2>".equals(token)) {
                continue;
            }

            if (token.startsWith("[") && token.endsWith("]")) {
                continue;
            }

            referenced.add(token);
        }
    }

    private @Nullable String findOwningScreenId(String optionId) {
        for (Map.Entry<String, List<String>> entry : this.subScreenTokens.entrySet()) {
            if (entry.getValue().contains(optionId)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static Map<String, List<String>> immutableListMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, Collections.unmodifiableList(new ArrayList<>(value))));
        return Collections.unmodifiableMap(copy);
    }
}
