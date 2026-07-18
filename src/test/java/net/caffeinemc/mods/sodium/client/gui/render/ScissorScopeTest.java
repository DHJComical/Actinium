package net.caffeinemc.mods.sodium.client.gui.render;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScissorScopeTest {
    @Test
    void restoresEnabledStateAndBoxAfterNestedRendering() {
        Backend backend = new Backend(true, new int[]{1, 2, 3, 4});

        try (ScissorScope ignored = ScissorScope.open(backend, new int[]{10, 20, 30, 40})) {
            assertTrue(backend.enabled);
            assertArrayEquals(new int[]{10, 20, 30, 40}, backend.box);
        }

        assertTrue(backend.enabled);
        assertArrayEquals(new int[]{1, 2, 3, 4}, backend.box);
    }

    @Test
    void restoresDisabledStateAndBoxWhenRenderingThrows() {
        Backend backend = new Backend(false, new int[]{5, 6, 7, 8});
        try {
            try (ScissorScope ignored = ScissorScope.open(backend, new int[]{50, 60, 70, 80})) {
                throw new IllegalStateException("render failed");
            }
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("render failed"));
        }

        assertFalse(backend.enabled);
        assertArrayEquals(new int[]{5, 6, 7, 8}, backend.box);
    }

    private static final class Backend implements ScissorScope.Backend {
        private boolean enabled;
        private int[] box;

        private Backend(boolean enabled, int[] box) {
            this.enabled = enabled;
            this.box = Arrays.copyOf(box, box.length);
        }

        @Override
        public boolean isEnabled() {
            return this.enabled;
        }

        @Override
        public int[] getBox() {
            return Arrays.copyOf(this.box, this.box.length);
        }

        @Override
        public void setBox(int[] box) {
            this.box = Arrays.copyOf(box, box.length);
        }

        @Override
        public void enable() {
            this.enabled = true;
        }

        @Override
        public void disable() {
            this.enabled = false;
        }
    }
}
