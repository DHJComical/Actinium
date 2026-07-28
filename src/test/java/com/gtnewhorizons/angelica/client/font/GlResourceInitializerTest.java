package com.gtnewhorizons.angelica.client.font;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlResourceInitializerTest {

    @Test
    void constructionDoesNotCreateResource() {
        AtomicInteger factoryCalls = new AtomicInteger();

        new GlResourceInitializer<>(() -> true, () -> {
            factoryCalls.incrementAndGet();
            return new Object();
        });

        assertEquals(0, factoryCalls.get());
    }

    @Test
    void missingContextFailsBeforeResourceCreation() {
        AtomicInteger factoryCalls = new AtomicInteger();
        GlResourceInitializer<Object> initializer = new GlResourceInitializer<>(() -> false, () -> {
            factoryCalls.incrementAndGet();
            return new Object();
        });

        assertThrows(IllegalStateException.class, initializer::get);
        assertEquals(0, factoryCalls.get());
    }

    @Test
    void initializedResourceStillRequiresContextOnCallingThread() {
        AtomicBoolean contextCurrent = new AtomicBoolean(true);
        GlResourceInitializer<Object> initializer = new GlResourceInitializer<>(contextCurrent::get, Object::new);

        initializer.get();
        contextCurrent.set(false);

        assertThrows(IllegalStateException.class, initializer::get);
    }

    @Test
    void concurrentFirstUseCreatesAndPublishesOneResource() throws Exception {
        AtomicInteger factoryCalls = new AtomicInteger();
        Object expected = new Object();
        GlResourceInitializer<Object> initializer = new GlResourceInitializer<>(() -> true, () -> {
            factoryCalls.incrementAndGet();
            return expected;
        });
        List<Callable<Object>> calls = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            calls.add(initializer::get);
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<Object>> results = executor.invokeAll(calls);
            for (Future<Object> result : results) {
                assertSame(expected, result.get());
            }
        }
        assertEquals(1, factoryCalls.get());
    }
}
