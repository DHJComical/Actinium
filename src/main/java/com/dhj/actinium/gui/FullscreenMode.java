package com.dhj.actinium.gui;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

public enum FullscreenMode implements TextProvider {
    OFF("celeritas.options.fullscreen_mode.off"),
    EXCLUSIVE("celeritas.options.fullscreen_mode.exclusive"),
    BORDERLESS("celeritas.options.fullscreen_mode.borderless");

    private final TextComponent name;

    FullscreenMode(String translationKey) {
        this.name = TextComponent.translatable(translationKey);
    }

    @Override
    public TextComponent getLocalizedName() {
        return this.name;
    }

}
