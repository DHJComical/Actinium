package com.gtnewhorizons.angelica.glsm.states;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolygonStateTest {

    @Test
    void glDefaults() {
        PolygonState state = new PolygonState();
        assertEquals(GL11.GL_FILL, state.getFrontMode());
        assertEquals(GL11.GL_FILL, state.getBackMode());
        assertEquals(0.0f, state.getOffsetFactor());
        assertEquals(0.0f, state.getOffsetUnits());
        assertEquals(0.0f, state.getOffsetClamp());
        assertEquals(GL11.GL_BACK, state.getCullFaceMode());
        assertEquals(GL11.GL_CCW, state.getFrontFace());
    }

    @Test
    void setSameAsCopyRoundTrip() {
        PolygonState a = new PolygonState();
        a.setFrontMode(GL11.GL_LINE);
        a.setBackMode(GL11.GL_POINT);
        a.setOffsetFactor(1.5f);
        a.setOffsetUnits(-2.0f);
        a.setOffsetClamp(0.25f);
        a.setCullFaceMode(GL11.GL_FRONT);
        a.setFrontFace(GL11.GL_CW);

        PolygonState b = a.copy();
        assertNotSame(a, b);
        assertTrue(a.sameAs(b));

        b.setOffsetClamp(0.5f);
        assertFalse(a.sameAs(b), "offset clamp difference must be observed");
        b.setOffsetClamp(0.25f);
        b.setFrontMode(GL11.GL_FILL);
        assertFalse(a.sameAs(b), "polygon mode difference must be observed");

        a.set(b);
        assertTrue(a.sameAs(b), "set() must copy every tracked field");
    }
}
