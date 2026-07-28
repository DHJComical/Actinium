package com.dhj.actinium.render.terrain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileEntityBatchDrawGuardTest {
    @Test
    void drawsBuildingBatch() {
        AtomicInteger draws = new AtomicInteger();

        boolean drawn = TileEntityBatchDrawGuard.drawIfBuilding(true, draws::incrementAndGet);

        assertTrue(drawn);
        assertEquals(1, draws.get());
    }

    @Test
    void skipsBatchAlreadyFinishedByNestedRenderer() {
        AtomicInteger draws = new AtomicInteger();

        boolean drawn = TileEntityBatchDrawGuard.drawIfBuilding(false, draws::incrementAndGet);

        assertFalse(drawn);
        assertEquals(0, draws.get());
    }

    @Test
    void preservesDrawFailure() {
        IllegalStateException failure = new IllegalStateException("draw failed");

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> TileEntityBatchDrawGuard.drawIfBuilding(true, () -> {
                throw failure;
            })
        );

        assertEquals(failure, thrown);
    }
}
