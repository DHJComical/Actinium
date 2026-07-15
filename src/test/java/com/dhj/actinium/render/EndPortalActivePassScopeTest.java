package com.dhj.actinium.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndPortalActivePassScopeTest {
    @Test
    void doesNotRestoreWhenTheCompositorProgramWasNotUsed() {
        List<String> events = new ArrayList<>();

        String result = EndPortalActivePassScope.run(
            markProgramUsed -> {
                events.add("raw-operation");
                return "cached";
            },
            () -> events.add("restore-pass")
        );

        assertEquals("cached", result);
        assertEquals(List.of("raw-operation"), events);
    }

    @Test
    void restoresOnceAfterAllRawProgramOperationsComplete() {
        List<String> events = new ArrayList<>();

        EndPortalActivePassScope.run(
            markProgramUsed -> {
                markProgramUsed.run();
                events.add("initialize-raw-restore");
                markProgramUsed.run();
                events.add("update-raw-restore");
                return null;
            },
            () -> events.add("restore-pass")
        );

        assertEquals(
            List.of("initialize-raw-restore", "update-raw-restore", "restore-pass"),
            events
        );
    }

    @Test
    void restoresOnceWhenAProgramOperationFails() {
        List<String> events = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> EndPortalActivePassScope.run(
            markProgramUsed -> {
                markProgramUsed.run();
                events.add("update-raw-restore");
                throw new IllegalStateException("update failed");
            },
            () -> events.add("restore-pass")
        ));

        assertEquals(List.of("update-raw-restore", "restore-pass"), events);
    }
}
