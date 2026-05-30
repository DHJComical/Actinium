package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

public enum MultiDrawMode implements TextProvider {
    DIRECT("direct", "sodium.options.multidraw_mode.direct"),
    INDIRECT("indirect", "sodium.options.multidraw_mode.indirect"),
    INDIVIDUAL("individual", "sodium.options.multidraw_mode.individual");

    private final String id;
    private final TextComponent name;

    MultiDrawMode(String id, String translationKey) {
        this.id = id;
        this.name = TextComponent.translatable(translationKey);
    }

    public String id() {
        return this.id;
    }

    @Override
    public TextComponent getLocalizedName() {
        return this.name;
    }

    public static MultiDrawMode fromProperty(String value) {
        for (MultiDrawMode mode : values()) {
            if (mode.id.equalsIgnoreCase(value)) {
                return mode;
            }
        }

        return DIRECT;
    }
}
