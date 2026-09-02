package com.dhj.actinium.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderWorldRecursionGuardTest {
    @AfterEach
    void verifyGuardIsBalanced() {
        // A leaked enter() would let this exit() succeed; the guard must be back at zero depth.
        assertThrows(IllegalStateException.class, RenderWorldRecursionGuard::exit);
    }

    @Test
    void startsNotNested() {
        assertFalse(RenderWorldRecursionGuard.isNested());
    }

    @Test
    void outermostPassIsNotNested() {
        RenderWorldRecursionGuard.enter();
        try {
            assertFalse(RenderWorldRecursionGuard.isNested());
        } finally {
            RenderWorldRecursionGuard.exit();
        }
    }

    @Test
    void nestedPassIsDetectedUntilItExits() {
        RenderWorldRecursionGuard.enter();
        try {
            RenderWorldRecursionGuard.enter();
            try {
                assertTrue(RenderWorldRecursionGuard.isNested());
            } finally {
                RenderWorldRecursionGuard.exit();
            }
            assertFalse(RenderWorldRecursionGuard.isNested());
        } finally {
            RenderWorldRecursionGuard.exit();
        }
    }

    @Test
    void exitWithoutMatchingEnterFails() {
        assertThrows(IllegalStateException.class, RenderWorldRecursionGuard::exit);
    }

    @Test
    void exceptionInNestedPassLeavesNoLeakedDepth() {
        RuntimeException failure = new RuntimeException("nested pass failed");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            RenderWorldRecursionGuard.enter();
            try {
                RenderWorldRecursionGuard.enter();
                try {
                    throw failure;
                } finally {
                    RenderWorldRecursionGuard.exit();
                }
            } finally {
                RenderWorldRecursionGuard.exit();
            }
        });

        assertSame(failure, thrown);
        assertFalse(RenderWorldRecursionGuard.isNested());
        assertThrows(IllegalStateException.class, RenderWorldRecursionGuard::exit);
    }
}
