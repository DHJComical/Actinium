package com.gtnewhorizons.angelica.glsm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuadConverterTest {
    @Test
    void skipsSharedEboBindWhenTheCurrentVaoAlreadyOwnsIt() {
        assertFalse(QuadConverter.needsSharedEboBind(17, 17));
    }

    @Test
    void bindsSharedEboForAnAppOwnedOrDifferentElementBuffer() {
        assertTrue(QuadConverter.needsSharedEboBind(0, 17));
        assertTrue(QuadConverter.needsSharedEboBind(23, 17));
    }
}
