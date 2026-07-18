package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VisibleHitMapTest {
    @Test
    void forgetsOldControlBoundsWhenViewportFrameChanges() {
        VisibleHitMap<String> map = new VisibleHitMap<>();
        map.add("old", new GuiRect(10, 10, 80, 20));
        assertEquals("old", map.find(20, 15));

        map.beginFrame();
        map.add("visible", new GuiRect(10, 40, 80, 10));

        assertNull(map.find(20, 15));
        assertEquals("visible", map.find(20, 45));
    }

    @Test
    void clippedBoundsDoNotHitHiddenPartOfPartiallyVisibleRow() {
        VisibleHitMap<String> map = new VisibleHitMap<>();
        GuiRect row = new GuiRect(10, 5, 80, 20);
        GuiRect viewport = new GuiRect(10, 10, 80, 30);
        map.add("row", row.intersection(viewport));

        assertNull(map.find(20, 7));
        assertEquals("row", map.find(20, 10));
    }
}
