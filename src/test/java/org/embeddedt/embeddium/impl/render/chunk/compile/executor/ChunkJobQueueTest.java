package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkJobQueueTest {
    @Test
    void reportsBlockedWorkerAndPreservesPermitSemantics() throws Exception {
        ChunkJobQueue queue = new ChunkJobQueue();
        CountDownLatch workerStarted = new CountDownLatch(1);
        AtomicReference<ChunkJob> returnedJob = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            workerStarted.countDown();
            try {
                returnedJob.set(queue.waitForNextJob());
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        });
        worker.setDaemon(true);

        try {
            worker.start();
            assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            boolean workerBlocked = false;
            while (System.nanoTime() < deadline) {
                if (queue.checkAndClearWorkerBlocked()) {
                    workerBlocked = true;
                    break;
                }
                Thread.yield();
            }

            assertTrue(workerBlocked);
            assertFalse(queue.checkAndClearWorkerBlocked());

            TestJob job = new TestJob();
            queue.add(job, false);

            worker.join(1_000);
            assertFalse(worker.isAlive());
            assertSame(job, returnedJob.get());
            assertTrue(queue.isEmpty());
        } finally {
            queue.shutdown();
            worker.join(1_000);
        }
    }

    private static final class TestJob implements ChunkJob {
        private volatile boolean cancelled;
        private volatile boolean started;

        @Override
        public void execute(ChunkBuildContext context) {
            this.started = true;
        }

        @Override
        public boolean isStarted() {
            return this.started;
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public void setCancelled() {
            this.cancelled = true;
        }
    }
}
