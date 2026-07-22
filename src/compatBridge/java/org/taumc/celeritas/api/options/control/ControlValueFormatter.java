package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

/**
 * Legacy formatter facade forwarding to the current text component implementation.
 */
@FunctionalInterface
public interface ControlValueFormatter {
    /** Formats a numeric control value for display. */
    TextComponent format(int value);

    /** Creates a percentage formatter. */
    static ControlValueFormatter percentage() {
        return v -> TextComponent.literal(v + "%");
    }

    /** Creates a plain integer formatter. */
    static ControlValueFormatter number() {
        return v -> TextComponent.literal(String.valueOf(v));
    }

    /** Creates a multiplier formatter. */
    static ControlValueFormatter multiplier() {
        return v -> TextComponent.literal(v + "x");
    }

    /** Creates a quantity formatter with a dedicated disabled label. */
    static ControlValueFormatter quantityOrDisabled(String name, String disabled) {
        return v -> TextComponent.literal(v == 0 ? disabled : v + " " + name);
    }

    /** Creates a formatter backed by a parameterized translation key. */
    static ControlValueFormatter translateVariable(String key) {
        return v -> TextComponent.translatable(key, v);
    }

    /** Creates a translated formatter with separate disabled and numeric keys. */
    static ControlValueFormatter translateDisabledOrVariable(String disabled, String variable) {
        return v -> v == 0 ? TextComponent.translatable(disabled) : TextComponent.translatable(variable, v);
    }

    /** Creates the vanilla GUI scale formatter. */
    static ControlValueFormatter guiScale() {
        return v -> v == 0 ? TextComponent.translatable("options.guiScale.auto") : TextComponent.literal(v + "x");
    }

    /** Creates the vanilla frame-rate limit formatter. */
    static ControlValueFormatter fpsLimit() {
        return v -> v == 260 ? TextComponent.translatable("options.framerateLimit.max") : TextComponent.translatable("options.framerate", v);
    }

    /** Creates the vanilla brightness formatter. */
    static ControlValueFormatter brightness() {
        return v -> v == 0 ? TextComponent.translatable("options.gamma.min") : v == 100 ? TextComponent.translatable("options.gamma.max") : TextComponent.literal(v + "%");
    }

    /** Creates the biome blend radius formatter. */
    static ControlValueFormatter biomeBlend() {
        return v -> v == 0 ? TextComponent.translatable("gui.none") : TextComponent.translatable("sodium.options.biome_blend.value", v);
    }
}
