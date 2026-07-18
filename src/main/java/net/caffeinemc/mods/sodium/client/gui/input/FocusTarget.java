package net.caffeinemc.mods.sodium.client.gui.input;

/** Keyboard focus endpoint managed by the screen-wide navigation chain. */
public interface FocusTarget {
    boolean isFocused();

    void setFocused(boolean focused);

    default boolean isFocusable() {
        return true;
    }
}
