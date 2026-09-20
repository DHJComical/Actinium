package dhj.embeddedt.embeddium.impl.render.chunk.compile.executor;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

class ChunkJobQueue {
    /**
     * Priority given to important jobs, which always run before any deferred job.
     */
    static final long IMPORTANT_PRIORITY = Long.MIN_VALUE;

    @SuppressWarnings("ComparatorCombinators")
    private static final Comparator<ChunkJobTyped<?, ?>> ORDER = (a, b) -> {
        int result = Long.compare(a.priority, b.priority);
        return result != 0 ? result : Long.compare(a.sequence, b.sequence);
    };

    private final PriorityQueue<ChunkJobTyped<?, ?>> jobs = new PriorityQueue<>(ORDER);
    private long nextSequence;

    private final Semaphore semaphore = new Semaphore(0);

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void add(ChunkJobTyped<?, ?> job, long priority) {
        if (!this.isRunning()) {
            throw new IllegalStateException("Queue is no longer running");
        }

        synchronized (this.jobs) {
            job.priority = priority;
            job.sequence = this.nextSequence++;
            this.jobs.add(job);
        }

        this.semaphore.release(1);
    }

    @Nullable
    public ChunkJob pollJob() {
        if (this.isRunning() && this.semaphore.tryAcquire()) {
            return this.getNextTask();
        } else {
            return null;
        }
    }

    @Nullable
    public ChunkJob waitForNextJob() throws InterruptedException {
        if (!this.isRunning()) {
            return null;
        }

        this.semaphore.acquire();

        return this.getNextTask();
    }

    public boolean stealJob(ChunkJob job) {
        if (!this.semaphore.tryAcquire()) {
            return false;
        }

        boolean success;

        synchronized (this.jobs) {
            success = this.jobs.remove(job);
        }

        if (!success) {
            // If we didn't manage to actually steal the task, then we need to release the permit which we did steal
            this.semaphore.release(1);
        }

        return success;
    }

    @Nullable
    private ChunkJob getNextTask() {
        synchronized (this.jobs) {
            return this.jobs.poll();
        }
    }

    public Collection<ChunkJob> shutdown() {
        var list = new ArrayDeque<ChunkJob>();

        this.isRunning.set(false);

        while (this.semaphore.tryAcquire()) {
            var task = this.getNextTask();

            if (task != null) {
                list.add(task);
            }
        }

        // force the worker threads to wake up and exit
        this.semaphore.release(Runtime.getRuntime().availableProcessors());

        return list;
    }

    public int size() {
        return this.semaphore.availablePermits();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }
}
