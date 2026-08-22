package com.dhj.actinium.compat.blockrender;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the async chunk-building crash reported in issue #36.
 *
 * <p>Extra Utilities 2's {@code XUBlockStatic$3.getQuads} lazily fills a plain
 * {@link HashMap} with {@code computeIfAbsent} per block instance. Actinium builds chunk
 * meshes on multiple worker threads, so two workers can enter {@code computeIfAbsent} on the
 * same map concurrently and throw {@code ConcurrentModificationException}. This test verifies
 * that {@link ModdedBlockRenderCompat#runWithBlockLock} serializes the cache-fill critical
 * section: the second caller blocks until the first mapping completes, so no two mappings can
 * ever run at once (exactly the property the real XU2 getQuads needs from the compat lock).</p>
 */
class ModdedBlockRenderCompatTest {
    @Test
    void serializesConcurrentCacheAccess() throws Exception {
        Object block = new Object();
        Map<Integer, Integer> cache = new HashMap<>();
        AtomicInteger activeMappings = new AtomicInteger();
        AtomicBoolean concurrentMappings = new AtomicBoolean();
        CountDownLatch firstMappingStarted = new CountDownLatch(1);
        CountDownLatch secondRenderStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstMapping = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> renderCachedValue(
                    block, cache, 1, activeMappings, concurrentMappings,
                    firstMappingStarted, null, releaseFirstMapping));

            assertTrue(firstMappingStarted.await(5, TimeUnit.SECONDS));

            var second = executor.submit(() -> renderCachedValue(
                    block, cache, 2, activeMappings, concurrentMappings,
                    null, secondRenderStarted, releaseFirstMapping));

            // The second caller must block on the lock until the first mapping completes.
            assertFalse(secondRenderStarted.await(150, TimeUnit.MILLISECONDS));

            releaseFirstMapping.countDown();

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            // If the lock were missing, the two computeIfAbsent calls would run concurrently
            // and set this flag (and very likely throw CME).
            assertFalse(concurrentMappings.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void differentLocksAllowConcurrentCacheAccess() throws Exception {
        // Two distinct blocks hold independent locks, so their critical sections do not block
        // each other (the lock is per block instance, not global).
        Object blockA = new Object();
        Object blockB = new Object();
        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch bStarted = new CountDownLatch(1);
        AtomicBoolean bothEntered = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> ModdedBlockRenderCompat.runWithBlockLock(blockA, () -> {
                aStarted.countDown();
                await(bStarted);
            }));
            var b = executor.submit(() -> ModdedBlockRenderCompat.runWithBlockLock(blockB, () -> {
                bStarted.countDown();
                await(aStarted);
            }));

            assertTrue(aStarted.await(1, TimeUnit.SECONDS));
            assertTrue(bStarted.await(1, TimeUnit.SECONDS));
            bothEntered.set(true);

            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
            assertTrue(bothEntered.get());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Simulates XU2's getQuads: lazily fills a plain HashMap and flags concurrent mappings.
     */
    private static void renderCachedValue(
            Object block,
            Map<Integer, Integer> cache,
            int key,
            AtomicInteger activeMappings,
            AtomicBoolean concurrentMappings,
            CountDownLatch firstMappingStarted,
            CountDownLatch secondRenderStarted,
            CountDownLatch releaseFirstMapping
    ) {
        ModdedBlockRenderCompat.runWithBlockLock(block, () -> {
            if (secondRenderStarted != null) {
                secondRenderStarted.countDown();
            }

            cache.computeIfAbsent(key, ignored -> {
                if (firstMappingStarted != null) {
                    firstMappingStarted.countDown();
                }

                int active = activeMappings.incrementAndGet();
                if (active > 1) {
                    concurrentMappings.set(true);
                }

                try {
                    if (releaseFirstMapping != null && firstMappingStarted != null) {
                        await(releaseFirstMapping);
                    }
                    return key;
                } finally {
                    activeMappings.decrementAndGet();
                }
            });
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for the latch");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the latch", ex);
        }
    }
}
