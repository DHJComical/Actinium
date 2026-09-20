package dhj.embeddedt.embeddium.impl.render.chunk.compile.executor;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import dhj.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkJobQueueTest {
    @Test
    void blockedWorkerWakesAndReceivesAddedJob() throws Exception {
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

            // Semaphore.acquire parks the worker; wait until it is actually blocked before adding work.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            boolean workerWaiting = false;
            while (System.nanoTime() < deadline) {
                if (worker.getState() == Thread.State.WAITING) {
                    workerWaiting = true;
                    break;
                }
                Thread.yield();
            }

            assertTrue(workerWaiting, "The worker did not block on the empty queue before the deadline");

            var job = newJob();
            queue.add(job, 0L);

            worker.join(1_000);
            assertFalse(worker.isAlive());
            assertSame(job, returnedJob.get());
            assertTrue(queue.isEmpty());
        } finally {
            queue.shutdown();
            worker.join(1_000);
        }
    }

    @Test
    void pollsJobsInPriorityOrder() {
        ChunkJobQueue queue = new ChunkJobQueue();

        try {
            var far = newJob();
            var near = newJob();
            var important = newJob();

            queue.add(far, 100L);
            queue.add(near, 5L);
            queue.add(important, ChunkJobQueue.IMPORTANT_PRIORITY);

            assertSame(important, queue.pollJob());
            assertSame(near, queue.pollJob());
            assertSame(far, queue.pollJob());
            assertTrue(queue.isEmpty());
        } finally {
            queue.shutdown();
        }
    }

    private static ChunkJobTyped<ChunkBuilderTask<Object>, Object> newJob() {
        return new ChunkJobTyped<>(new StubTask(), result -> {
        });
    }

    private static final class StubTask extends ChunkBuilderTask<Object> {
        @Override
        public Object execute(ChunkBuildContext context, CancellationToken cancellationToken) {
            return null;
        }
    }
}
