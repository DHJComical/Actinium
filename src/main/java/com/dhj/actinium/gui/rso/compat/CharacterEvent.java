package com.dhj.actinium.gui.rso.compat;

/**
 * Newer-Minecraft style character event adapted to the 1.12.2 client.
 * Carries a single Unicode code point produced by keyboard input.
 */
public record CharacterEvent(int codepoint) {

    /** Returns the code point as a String. */
    public String codepointAsString() {
        return new String(Character.toChars(this.codepoint));
    }

    /** Returns whether this code point may be typed into a text field. */
    public boolean isAllowedChatCharacter() {
        return this.codepoint != 167 && this.codepoint >= 32 && this.codepoint != 127;
    }
}
