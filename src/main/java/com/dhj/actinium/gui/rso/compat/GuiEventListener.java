package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style GUI event listener adapted to the 1.12.2 client.
 * Mirrors the upstream {@code GuiEventListener} contract so Reese's Sodium
 * Options widgets can keep their event handling structure unchanged.
 */
public interface GuiEventListener {

    /** Returns whether this listener is currently able to receive interaction. */
    default boolean isActive() {
        return true;
    }

    /**
     * Returns whether the given mouse position is over the widget.
     *
     * @param mouseX the GUI-space mouse X
     * @param mouseY the GUI-space mouse Y
     */
    default boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    /** Sets whether this listener has keyboard focus. */
    default void setFocused(boolean focused) {
    }

    /** Returns whether this listener currently has keyboard focus. */
    default boolean isFocused() {
        return false;
    }

    /**
     * Handles a mouse click.
     *
     * @param event      the mouse button event
     * @param doubleClick whether the click is a double click
     * @return true if the event was consumed
     */
    default boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    /**
     * Handles a mouse release.
     *
     * @param event the mouse button event
     * @return true if the event was consumed
     */
    default boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }

    /**
     * Handles a mouse drag.
     *
     * @param event  the mouse button event
     * @param deltaX the horizontal drag delta
     * @param deltaY the vertical drag delta
     * @return true if the event was consumed
     */
    default boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Handles a mouse scroll.
     *
     * @param mouseX          the GUI-space mouse X
     * @param mouseY          the GUI-space mouse Y
     * @param horizontalAmount the horizontal scroll amount
     * @param verticalAmount  the vertical scroll amount
     * @return true if the event was consumed
     */
    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    /**
     * Handles a key press.
     *
     * @param event the key event
     * @return true if the event was consumed
     */
    default boolean keyPressed(KeyEvent event) {
        return false;
    }

    /**
     * Handles a typed character.
     *
     * @param event the character event
     * @return true if the event was consumed
     */
    default boolean charTyped(CharacterEvent event) {
        return false;
    }

    /**
     * Computes the next focus path for the given navigation event, or null if
     * this listener cannot take focus.
     *
     * @param navigation the focus navigation event
     */
    default ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return null;
    }

    /**
     * Returns the rectangle occupied by this listener in GUI coordinates.
     */
    default ScreenRectangle getRectangle() {
        return ScreenRectangle.empty();
    }

    /**
     * Returns the current focus path of this listener, or null when it does
     * not currently hold focus.
     */
    default ComponentPath getCurrentFocusPath() {
        return null;
    }
}
