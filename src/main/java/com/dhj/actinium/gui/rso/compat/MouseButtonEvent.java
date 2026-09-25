package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style mouse button event adapted to the 1.12.2 client.
 * Carries the button index and the GUI-space cursor position.
 */
public record MouseButtonEvent(int button, double x, double y) {

    /** Returns whether the left mouse button was involved. */
    public boolean isLeft() {
        return this.button == 0;
    }

    /** Returns whether the right mouse button was involved. */
    public boolean isRight() {
        return this.button == 1;
    }

    /** Returns whether the middle mouse button was involved. */
    public boolean isMiddle() {
        return this.button == 2;
    }
}
