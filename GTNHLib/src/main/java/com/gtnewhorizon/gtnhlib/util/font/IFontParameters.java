package com.gtnewhorizon.gtnhlib.util.font;

public interface IFontParameters {
    default float actinium$getGlyphScaleX() { return 1.0f; }
    default float actinium$getGlyphScaleY() { return 1.0f; }
    default float actinium$getGlyphSpacing() { return 0.0f; }
    default float actinium$getWhitespaceScale() { return 1.0f; }
    default float actinium$getShadowOffset() { return 1.0f; }
    default float actinium$getCharWidthFine(char chr) { return 0.0f; }
}
