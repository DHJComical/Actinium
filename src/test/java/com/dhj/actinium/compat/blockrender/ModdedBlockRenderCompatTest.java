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

class ModdedBlockRenderCompatTest {
    @Test
    void recognizesKnownUnsafeRendererClasses() {
        assertTrue(ModdedBlockRenderCompat.isUnsafeBlockClassName(
                "com.rwtema.extrautils2.backend.XUBlockStatic$3"));
        assertTrue(ModdedBlockRenderCompat.isUnsafeBlockClassName(
                "com.infinityraider.agricraft.blocks.BlockCrop"));
    }

    @Test
    void rejectsUnrelatedRendererClasses() {
        assertFalse(ModdedBlockRenderCompat.isUnsafeBlockClassName("net.minecraft.block.Block"));
        assertFalse(ModdedBlockRenderCompat.isUnsafeBlockClassName("com.rwtema.extrautils1.backend.XUBlock"));
        assertFalse(ModdedBlockRenderCompat.isUnsafeBlockClassName(null));
    }

    @Test
    void serializesSharedRendererCacheAccess() throws Exception {
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
                    block,
                    cache,
                    1,
                    activeMappings,
                    concurrentMappings,
                    firstMappingStarted,
                    null,
                    releaseFirstMapping
            ));

            assertTrue(firstMappingStarted.await(5, TimeUnit.SECONDS));

            var second = executor.submit(() -> renderCachedValue(
                    block,
                    cache,
                    2,
                    activeMappings,
                    concurrentMappings,
                    null,
                    secondRenderStarted,
                    releaseFirstMapping
            ));

            assertFalse(secondRenderStarted.await(100, TimeUnit.MILLISECONDS));
            releaseFirstMapping.countDown();

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertFalse(concurrentMappings.get());
        } finally {
            executor.shutdownNow();
        }
    }

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
                throw new AssertionError("Timed out waiting for the first cache mapping");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the first cache mapping", ex);
        }
    }
}
