package net.coderbot.iris.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the pass-binding policy without requiring an OpenGL context.
 */
class DeferredWorldRenderingPipelinePassBindingTest {
    @Test
    void checksTheCurrentProgramForShadowRendering() {
        assertTrue(DeferredWorldRenderingPipeline.shouldCheckCurrentProgram(WorldRenderingPhase.TERRAIN_SOLID, true));
        assertTrue(DeferredWorldRenderingPipeline.shouldCheckCurrentProgram(WorldRenderingPhase.NONE, true));
    }

    @Test
    void checksTheCurrentProgramForEntityPhases() {
        assertTrue(DeferredWorldRenderingPipeline.shouldCheckCurrentProgram(WorldRenderingPhase.ENTITIES, false));
        assertTrue(DeferredWorldRenderingPipeline.shouldCheckCurrentProgram(WorldRenderingPhase.BLOCK_ENTITIES, false));
    }

    @Test
    void skipsTheCurrentProgramCheckForOrdinaryPhases() {
        assertFalse(DeferredWorldRenderingPipeline.shouldCheckCurrentProgram(WorldRenderingPhase.TERRAIN_SOLID, false));
        assertFalse(DeferredWorldRenderingPipeline.shouldCheckCurrentProgram(WorldRenderingPhase.SKY, false));
    }

    @Test
    void rebindsOnlyWhenTheActualProgramDiffers() {
        assertTrue(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.TERRAIN_SOLID, true, 17, 23));
        assertTrue(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.ENTITIES, false, 17, 23));
        assertTrue(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.ENTITIES, false, 17, 0));
        assertTrue(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.TERRAIN_SOLID, true, 17, 0));
        assertFalse(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.BLOCK_ENTITIES, false, 23, 23));
        assertFalse(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.ENTITIES, false, 0, 0));
        assertFalse(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.TERRAIN_SOLID, true, 0, 0));
        assertFalse(DeferredWorldRenderingPipeline.shouldRebindPass(
            WorldRenderingPhase.TERRAIN_SOLID, false, 17, 23));
    }

    @Test
    void treatsAProgramlessDefaultPassAsProgramZero() {
        assertEquals(0, DeferredWorldRenderingPipeline.getPassProgramId(null));
    }

    @Test
    void preservesTheShadowFailureWhenCleanupAlsoFails() {
        RuntimeException shadowFailure = new RuntimeException("shadow failure");
        RuntimeException cleanupFailure = new RuntimeException("cleanup failure");

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            DeferredWorldRenderingPipeline.runShadowPassWithCleanup(
                () -> {
                    throw shadowFailure;
                },
                () -> {
                    throw cleanupFailure;
                }));

        assertSame(shadowFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void propagatesCleanupFailureWhenShadowRenderingSucceeds() {
        RuntimeException cleanupFailure = new RuntimeException("cleanup failure");

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            DeferredWorldRenderingPipeline.runShadowPassWithCleanup(
                () -> {
                },
                () -> {
                    throw cleanupFailure;
                }));

        assertSame(cleanupFailure, thrown);
    }
}
