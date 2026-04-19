package com.dhj.actinium.shader.options;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActiniumShaderProfileSet {
    private final LinkedHashMap<String, ActiniumShaderProfile> orderedProfiles;
    private final List<ActiniumShaderProfile> sortedProfiles;

    public ActiniumShaderProfileSet(LinkedHashMap<String, ActiniumShaderProfile> orderedProfiles) {
        this.orderedProfiles = new LinkedHashMap<>(orderedProfiles);
        this.sortedProfiles = new ArrayList<>(orderedProfiles.values());
        this.sortedProfiles.sort((left, right) -> Integer.compare(right.optionValues().size(), left.optionValues().size()));
    }

    public int size() {
        return this.orderedProfiles.size();
    }

    public ProfileResult scan(ActiniumShaderOptionMenu menu, Map<String, String> currentValues) {
        if (this.sortedProfiles.isEmpty()) {
            return new ProfileResult(null, null, null);
        }

        for (int index = 0; index < this.sortedProfiles.size(); index++) {
            ActiniumShaderProfile profile = this.sortedProfiles.get(index);

            if (matches(menu, profile, currentValues)) {
                return new ProfileResult(
                        profile,
                        this.sortedProfiles.get(Math.floorMod(index + 1, this.sortedProfiles.size())),
                        this.sortedProfiles.get(Math.floorMod(index - 1, this.sortedProfiles.size()))
                );
            }
        }

        return new ProfileResult(
                null,
                this.sortedProfiles.get(0),
                this.sortedProfiles.get(this.sortedProfiles.size() - 1)
        );
    }

    private static boolean matches(ActiniumShaderOptionMenu menu, ActiniumShaderProfile profile, Map<String, String> currentValues) {
        for (Map.Entry<String, String> entry : profile.optionValues().entrySet()) {
            ActiniumShaderOption option = menu.getOption(entry.getKey());

            if (option == null) {
                continue;
            }

            String currentValue = currentValues.getOrDefault(option.name(), option.getDefaultSerializedValue());

            if (!entry.getValue().equals(currentValue)) {
                return false;
            }
        }

        return true;
    }

    public static ActiniumShaderProfileSet fromTree(Map<String, List<String>> tree, ActiniumShaderOptionMenu menu) {
        LinkedHashMap<String, ActiniumShaderProfile> profiles = new LinkedHashMap<>();

        for (String name : tree.keySet()) {
            profiles.put(name, parse(name, new ArrayList<>(), tree, menu));
        }

        return new ActiniumShaderProfileSet(profiles);
    }

    private static ActiniumShaderProfile parse(String name, List<String> parents, Map<String, List<String>> tree, ActiniumShaderOptionMenu menu) {
        Map<String, String> optionValues = new LinkedHashMap<>();
        List<String> options = tree.get(name);

        if (options == null) {
            throw new IllegalArgumentException("Profile '" + name + "' does not exist");
        }

        for (String option : options) {
            if (option.startsWith("profile.")) {
                String dependency = option.substring("profile.".length());

                if (parents.contains(dependency)) {
                    throw new IllegalArgumentException("Profile '" + name + "' is recursively included");
                }

                parents.add(dependency);
                optionValues.putAll(parse(dependency, parents, tree, menu).optionValues());
                parents.remove(parents.size() - 1);
                continue;
            }

            if (option.startsWith("!program.")) {
                continue;
            }

            if (option.startsWith("!")) {
                String optionName = option.substring(1);

                if (menu.getOption(optionName) != null) {
                    optionValues.put(optionName, "false");
                }
                continue;
            }

            int separator = option.indexOf('=');

            if (separator < 0) {
                separator = option.indexOf(':');
            }

            if (separator >= 0) {
                String optionName = option.substring(0, separator);
                String value = option.substring(separator + 1);

                if (menu.getOption(optionName) != null) {
                    optionValues.put(optionName, value);
                }
                continue;
            }

            ActiniumShaderOption resolved = menu.getOption(option);

            if (resolved != null && resolved.isBooleanOption()) {
                optionValues.put(option, "true");
            }
        }

        return new ActiniumShaderProfile(name, optionValues);
    }

    public record ProfileResult(ActiniumShaderProfile current, ActiniumShaderProfile next, ActiniumShaderProfile previous) {
    }
}
