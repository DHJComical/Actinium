package net.caffeinemc.mods.sodium.client.gui.widgets;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollStateTest {
    @Test
    void clampsScrollAndReportsOnlyRealChanges() {
        List<Integer> changes = new ArrayList<>();
        ScrollState state = new ScrollState(changes::add);
        state.setContext(100, 260);

        state.scroll(35);
        state.scrollTo(999);
        state.scroll(1);
        state.scrollTo(-10);

        assertEquals(List.of(35, 160, 0), changes);
        assertEquals(0, state.amount());
        assertTrue(state.canScroll());
        assertEquals(38, state.thumbLength(100));
    }

    @Test
    void contextShrinkClampsExistingOffset() {
        ScrollState state = new ScrollState(null);
        state.setContext(100, 300);
        state.scrollTo(180);
        state.setContext(100, 140);
        assertEquals(40, state.amount());
        state.setContext(150, 140);
        assertEquals(0, state.amount());
        assertFalse(state.canScroll());
    }

    @Test
    void rejectsNegativeDimensions() {
        ScrollState state = new ScrollState(null);
        assertThrows(IllegalArgumentException.class, () -> state.setContext(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> state.setContext(10, -1));
    }
}
