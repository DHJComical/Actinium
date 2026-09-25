package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style screen direction adapted to the 1.12.2 client.
 */
public enum ScreenDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN;

    /** Returns the opposite direction. */
    public ScreenDirection getOpposite() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }
}
