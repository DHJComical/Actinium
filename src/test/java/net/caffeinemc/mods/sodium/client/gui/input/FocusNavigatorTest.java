package net.caffeinemc.mods.sodium.client.gui.input;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FocusNavigatorTest {
    @Test
    void movesAcrossFocusableTargetsAndCancelsPreviousFocus() {
        Target search = new Target(true);
        Target disabled = new Target(false);
        Target option = new Target(true);
        Target apply = new Target(true);
        FocusNavigator navigator = new FocusNavigator();
        navigator.replaceTargets(List.of(search, disabled, option, apply));

        assertSame(search, navigator.move(1));
        assertTrue(search.focused);
        assertSame(option, navigator.move(1));
        assertFalse(search.focused);
        assertTrue(option.focused);
        assertSame(search, navigator.move(-1));
        assertFalse(option.focused);
        assertTrue(search.focused);
    }

    @Test
    void rebuildingChainClearsRemovedOrDisabledFocus() {
        Target option = new Target(true);
        Target done = new Target(true);
        FocusNavigator navigator = new FocusNavigator();
        navigator.replaceTargets(List.of(option, done));
        navigator.focus(option);

        navigator.replaceTargets(List.of(done));
        assertFalse(option.focused);
        assertNull(navigator.focused());

        navigator.focus(done);
        done.focusable = false;
        navigator.replaceTargets(List.of(done));
        assertFalse(done.focused);
        assertNull(navigator.focused());
    }

    private static final class Target implements FocusTarget {
        private boolean focusable;
        private boolean focused;

        private Target(boolean focusable) {
            this.focusable = focusable;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocusable() {
            return this.focusable;
        }
    }
}
