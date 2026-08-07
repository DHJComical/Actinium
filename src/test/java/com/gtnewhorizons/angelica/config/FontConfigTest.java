package com.gtnewhorizons.angelica.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FontConfigTest {
    @Test
    void fontAntiAliasingIsDisabledByDefault() {
        assertEquals(0, FontConfig.fontAAMode);
    }
}
