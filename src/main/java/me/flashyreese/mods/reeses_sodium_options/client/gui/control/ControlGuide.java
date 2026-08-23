package me.flashyreese.mods.reeses_sodium_options.client.gui.control;

import com.dhj.actinium.gui.rso.compat.Component;

public record ControlGuide(Input input, Component label) {
    public static ControlGuide press(Component label) {
        return new ControlGuide(Input.PRESS, label);
    }

    public static ControlGuide navigationLeftRight(Component label) {
        return new ControlGuide(Input.NAVIGATION_LEFT_RIGHT, label);
    }

    public static ControlGuide previousTab(Component label) {
        return new ControlGuide(Input.PREVIOUS_TAB, label);
    }

    public static ControlGuide nextTab(Component label) {
        return new ControlGuide(Input.NEXT_TAB, label);
    }

    public enum Input {
        PRESS,
        NAVIGATION_LEFT_RIGHT,
        PREVIOUS_TAB,
        NEXT_TAB
    }
}
