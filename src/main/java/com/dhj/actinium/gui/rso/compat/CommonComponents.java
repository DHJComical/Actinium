package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style common component helpers adapted to the 1.12.2 client.
 */
public final class CommonComponents {

    private CommonComponents() {
    }

    /** Combines an option name and value into a single label. */
    public static Component optionNameValue(Component name, Component value) {
        return new MutableComponent(name.unwrap().createCopy())
                .append(literal(": "))
                .append(value);
    }

    /** Returns the localized on/off status for a boolean option. */
    public static Component optionStatus(boolean enabled) {
        return translatable(enabled ? "options.on" : "options.off");
    }

    /** Creates a translatable component. */
    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** Creates a literal component. */
    public static Component literal(String text) {
        return Component.literal(text);
    }
}
