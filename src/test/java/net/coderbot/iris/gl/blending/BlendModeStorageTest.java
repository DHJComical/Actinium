package net.coderbot.iris.gl.blending;

import com.gtnewhorizons.angelica.glsm.states.BlendState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlendModeStorageTest {
    private FakeBlendStateAccess stateAccess;

    @BeforeEach
    void setUp() {
        stateAccess = new FakeBlendStateAccess();
        stateAccess.enabled = true;
        stateAccess.blend.setAll(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        BlendModeStorage.setBlendStateAccessForTesting(stateAccess);
    }

    @AfterEach
    void tearDown() {
        BlendModeStorage.restoreDefaultBlendStateAccessForTesting();
    }

    @Test
    void deferredVanillaBlendDoesNotEscapeGlobalOverride() {
        BlendModeOverride.OFF.apply();
        assertFalse(stateAccess.enabled);

        BlendModeStorage.deferBlendModeToggle(true);
        BlendModeStorage.deferBlendFunc(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ZERO);
        BlendModeStorage.flushDeferredBlend();

        assertFalse(stateAccess.enabled);
        assertEquals(GL11.GL_SRC_ALPHA, stateAccess.blend.getSrcRgb());
        assertTrue(BlendModeStorage.isBlendLocked());
        assertTrue(BlendModeStorage.isHasDeferredChanges());

        BlendModeOverride.restore();

        assertTrue(stateAccess.enabled);
        assertEquals(GL11.GL_ONE, stateAccess.blend.getSrcRgb());
        assertEquals(GL11.GL_ZERO, stateAccess.blend.getDstRgb());
        assertFalse(BlendModeStorage.isBlendLocked());
        assertFalse(BlendModeStorage.isHasDeferredChanges());
    }

    @Test
    void deferredBlendFlushesNormallyWithoutOverride() {
        stateAccess.enabled = false;
        BlendModeStorage.setBlendStateAccessForTesting(stateAccess);

        BlendModeStorage.deferBlendModeToggle(true);
        BlendModeStorage.deferBlendFunc(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BlendModeStorage.flushDeferredBlend();

        assertTrue(stateAccess.enabled);
        assertEquals(GL11.GL_ONE, stateAccess.blend.getSrcRgb());
        assertEquals(GL11.GL_ONE, stateAccess.blend.getDstRgb());
        assertEquals(GL11.GL_SRC_ALPHA, stateAccess.blend.getSrcAlpha());
        assertEquals(GL11.GL_ONE_MINUS_SRC_ALPHA, stateAccess.blend.getDstAlpha());
        assertFalse(BlendModeStorage.isHasDeferredChanges());
    }

    @Test
    void consecutiveGlobalOverridePreservesDeferredVanillaState() {
        BlendModeOverride.OFF.apply();
        BlendModeStorage.deferBlendModeToggle(true);
        BlendModeStorage.deferBlendFunc(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ZERO);

        BlendState secondOverride = new BlendState(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        new BlendModeOverride(secondOverride).apply();
        BlendModeStorage.flushDeferredBlend();

        assertTrue(stateAccess.enabled);
        assertEquals(GL11.GL_SRC_ALPHA, stateAccess.blend.getSrcRgb());
        assertEquals(GL11.GL_ONE_MINUS_SRC_ALPHA, stateAccess.blend.getDstRgb());
        assertTrue(BlendModeStorage.isBlendLocked());
        assertTrue(BlendModeStorage.isHasDeferredChanges());

        BlendModeOverride.restore();

        assertTrue(stateAccess.enabled);
        assertEquals(GL11.GL_ONE, stateAccess.blend.getSrcRgb());
        assertEquals(GL11.GL_ZERO, stateAccess.blend.getDstRgb());
        assertFalse(BlendModeStorage.isBlendLocked());
        assertFalse(BlendModeStorage.isHasDeferredChanges());
    }

    @Test
    void indexedOverrideSurvivesLockedFlushUntilGlobalRestore() {
        int index = 3;
        BlendState indexedOverride = new BlendState(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        BlendModeStorage.overrideBufferBlend(index, indexedOverride);
        BlendModeStorage.deferBlendModeToggle(false);
        BlendModeStorage.deferBlendFunc(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

        BlendModeStorage.flushDeferredBlend();

        assertTrue(stateAccess.enabled);
        assertEquals(GL11.GL_SRC_ALPHA, stateAccess.blend.getSrcRgb());
        assertTrue(stateAccess.bufferEnabled.get(index));
        assertEquals(GL11.GL_SRC_ALPHA, stateAccess.bufferBlend.get(index).getSrcRgb());
        assertEquals(GL11.GL_ONE, stateAccess.bufferBlend.get(index).getDstRgb());

        BlendModeOverride.restore();

        assertFalse(stateAccess.enabled);
        assertEquals(GL11.GL_ONE, stateAccess.blend.getSrcRgb());
        assertEquals(GL11.GL_ONE, stateAccess.blend.getDstRgb());
        assertTrue(stateAccess.bufferEnabled.get(index));
        assertEquals(GL11.GL_SRC_ALPHA, stateAccess.bufferBlend.get(index).getSrcRgb());
        assertFalse(BlendModeStorage.isBlendLocked());
        assertFalse(BlendModeStorage.isHasDeferredChanges());
    }

    private static final class FakeBlendStateAccess implements BlendModeStorage.BlendStateAccess {
        private boolean enabled;
        private final BlendState blend = new BlendState();
        private final Map<Integer, Boolean> bufferEnabled = new HashMap<>();
        private final Map<Integer, BlendState> bufferBlend = new HashMap<>();

        @Override
        public boolean isBlendEnabled() {
            return enabled;
        }

        @Override
        public void copyBlendState(BlendState destination) {
            destination.set(blend);
        }

        @Override
        public void setBlendEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void setBlendFunction(BlendState state) {
            blend.set(state);
        }

        @Override
        public void setBufferBlendEnabled(int index, boolean enabled) {
            bufferEnabled.put(index, enabled);
        }

        @Override
        public void setBufferBlendFunction(int index, BlendState state) {
            BlendState copy = new BlendState();
            copy.set(state);
            bufferBlend.put(index, copy);
        }
    }
}
