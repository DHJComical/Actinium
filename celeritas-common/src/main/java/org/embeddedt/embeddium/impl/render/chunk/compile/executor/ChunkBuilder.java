package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.render.chunk.compile.GlobalChunkBuildContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChunkBuilder {
    static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");
    /**
     * Megabytes of heap required per chunk builder thread. This is used to cap the number of worker
     * threads when the game is given a small heap.
     */
    private static final int MBS_PER_CHUNK_BUILDER = 64;

    /**
     * The number of tasks to allow in the queue per available worker thread. This value should be kept conservative
     * to avoid the threads becoming backlogged and failing to keep up with changes in chunk visibility (e.g.
     * camera movement). However, it also needs to be large enough that the thread is not spending part of the
     * frame doing nothing. 2 seems to be a decent value, and is what Sodium 0.2 used.
     * <p></p>
     * With adaptive scheduling, this is used as the floor for the in-flight target.
     */
    private static final int TASK_QUEUE_LIMIT_PER_WORKER = 2;

    /** Initial in-flight target per worker before measured throughput is available. */
    private static final int WARM_START_PER_WORKER = 32;

    /** Dampens target decay so temporary worker slowdown does not immediately discard useful queue capacity. */
    private static final int TARGET_DECAY_DAMPING = 2;

    /** Keeps the legacy fixed floor available as a local fallback during tuning or regression diagnosis. */
    private static final boolean ENABLE_ADAPTIVE_SCHEDULING = true;

    /** Enables target-change logging for local tuning. */
    private static final boolean DEBUG_ADAPTIVE_SCHEDULING = false;

    private final ChunkJobQueue queue = new ChunkJobQueue();

    private final List<WorkerThread> threads = new ArrayList<>();

    private final AtomicInteger busyThreadCount = new AtomicInteger();

    /** Current desired number of queued in-flight tasks, bounded below by the scheduling floor. */
    private int targetInFlight;

    /** Whether the previous dispatch stopped at its budget while more rebuild work remained. */
    private boolean lastDispatchBudgetLimited;

    private final ChunkBuildContext localContext;

    private final ManagedBlocker managedBlocker;

    public ChunkBuilder(ManagedBlocker managedBlocker, Supplier<ChunkBuildContext> contextSupplier, int requestedThreads) {
        GlobalChunkBuildContext.setMainThread();

        if (requestedThreads >= 0) {
            int count = getThreadCount(requestedThreads);

            for (int i = 0; i < count; i++) {
                ChunkBuildContext context = contextSupplier.get();
                WorkerRunnable worker = new WorkerRunnable(context);

                WorkerThread thread = new WorkerThread(worker, "Chunk Render Task Executor #" + i, context);
                thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
                thread.start();

                this.threads.add(thread);
            }
        }

        LOGGER.info("Started {} worker threads", this.threads.size());

        this.localContext = contextSupplier.get();

        this.managedBlocker = managedBlocker;

        if (ENABLE_ADAPTIVE_SCHEDULING && !this.threads.isEmpty()) {
            long warmStartTarget = (long) WARM_START_PER_WORKER * this.threads.size();
            this.targetInFlight = (int) Math.min(Integer.MAX_VALUE,
                    Math.max(this.getSchedulingFloor(), warmStartTarget));
        } else {
            this.targetInFlight = this.getSchedulingFloor();
        }
    }

    /**
     * Returns the minimum in-flight queue size needed to keep the workers supplied.
     */
    private int getSchedulingFloor() {
        return Math.max(1, this.threads.size()) * TASK_QUEUE_LIMIT_PER_WORKER;
    }

    /**
     * Advances the adaptive target once for the current frame.
     */
    public void tickSchedulingBudget() {
        if (!ENABLE_ADAPTIVE_SCHEDULING) {
            return;
        }

        int floor = this.getSchedulingFloor();
        int queued = this.queue.size();
        boolean workerStarved = this.queue.checkAndClearWorkerBlocked();
        int previousTarget = this.targetInFlight;

        if (workerStarved && this.lastDispatchBudgetLimited) {
            this.targetInFlight = (int) Math.min(Integer.MAX_VALUE, (long) this.targetInFlight * 2);
        } else if (queued > floor) {
            int excess = queued - floor;
            int decayStep = Math.max(1, excess / TARGET_DECAY_DAMPING);
            this.targetInFlight -= decayStep;
        }

        this.targetInFlight = Math.max(floor, this.targetInFlight);

        if (DEBUG_ADAPTIVE_SCHEDULING && this.targetInFlight != previousTarget) {
            LOGGER.info("Adaptive scheduling target {} {} -> {} (queued={}, starved={}, budgetLimited={})",
                    this.targetInFlight > previousTarget ? "grew" : "shrank",
                    previousTarget, this.targetInFlight, queued, workerStarved, this.lastDispatchBudgetLimited);
        }
    }

    /**
     * Returns the current desired number of in-flight tasks.
     */
    public int getTargetQueueSize() {
        return this.targetInFlight;
    }

    /**
     * Returns the remaining number of tasks allowed before reaching the current in-flight target.
     */
    public int getSchedulingBudget() {
        return Math.max(0, this.targetInFlight - this.queue.size());
    }

    /**
     * Records whether the previous dispatch was limited by this builder's budget.
     */
    public void setDispatchBudgetLimited(boolean budgetLimited) {
        this.lastDispatchBudgetLimited = budgetLimited;
    }

    /**
     * <p>Notifies all worker threads to stop and blocks until all workers terminate. After the workers have been shut
     * down, all tasks are cancelled and the pending queues are cleared. If the builder is already stopped, this
     * method does nothing and exits.</p>
     *
     * <p>After shutdown, all previously scheduled jobs will have been cancelled. Jobs that finished while
     * waiting for worker threads to shut down will still have their results processed for later cleanup.</p>
     */
    public void shutdown() {
        if (!this.queue.isRunning()) {
            throw new IllegalStateException("Worker threads are not running");
        }

        // Delete any queued tasks and resources attached to them
        var jobs = this.queue.shutdown();

        for (var job : jobs) {
            job.setCancelled();
        }

        this.shutdownThreads();
    }

    private void shutdownThreads() {
        LOGGER.info("Stopping worker threads");

        // Wait for every remaining thread to terminate
        for (WorkerThread thread : this.threads) {
            this.managedBlocker.managedBlock(() -> !thread.isAlive());
        }

        this.threads.clear();
    }

    public <TASK extends ChunkBuilderTask<OUTPUT>, OUTPUT> ChunkJobTyped<TASK, OUTPUT> scheduleTask(TASK task, boolean important,
                                                                                                    Consumer<@Nullable ChunkJobResult<OUTPUT>> consumer)
    {
        Objects.requireNonNull(task, "Task must be non-null");

        if (!this.queue.isRunning()) {
            throw new IllegalStateException("Executor is stopped");
        }

        var job = new ChunkJobTyped<>(task, consumer);

        this.queue.add(job, important);

        return job;
    }

    /**
     * Returns the "optimal" number of threads to be used for chunk build tasks. This will always return at least one
     * thread.
     */
    private static int getOptimalThreadCount() {
        int desiredThreads = Math.max(getMaxThreadCount() / 3, getMaxThreadCount() - 6);
        if (desiredThreads < 1) {
            return 1;
        } else if (desiredThreads > 10) {
            return 10;
        } else {
            return desiredThreads;
        }
    }

    private static int getThreadCount(int requested) {
        return requested == 0 ? getOptimalThreadCount() : Math.min(requested, getMaxThreadCount());
    }

    public static int getMaxThreadCount() {
        int totalCores = Runtime.getRuntime().availableProcessors();
        long memoryMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        // always allow at least one builder regardless of heap size
        int maxBuilders = Math.max(1, (int)(memoryMb / MBS_PER_CHUNK_BUILDER));
        // choose the total CPU cores or the number of builders the heap permits, whichever is smaller
        return Math.min(totalCores, maxBuilders);
    }

    public void tryStealTask(ChunkJob job) {
        if (!this.queue.stealJob(job)) {
            return;
        }

        executeJobWithLocalContext(job);
    }

    private void executeJobWithLocalContext(ChunkJob job) {
        var localContext = this.localContext;
        GlobalChunkBuildContext.bindMainThread(localContext);

        try {
            job.execute(localContext);
        } finally {
            GlobalChunkBuildContext.bindMainThread(null);
            localContext.cleanup();
        }
    }

    public void tick() {
        // Don't need to run jobs on the main thread if there are worker threads
        if (!this.threads.isEmpty()) {
            return;
        }

        while (!this.queue.isEmpty()) {
            var job = Objects.requireNonNull(this.queue.pollJob());
            executeJobWithLocalContext(job);
        }
    }

    public boolean isBuildQueueEmpty() {
        return this.queue.isEmpty();
    }

    public int getScheduledJobCount() {
        return this.queue.size();
    }

    public int getBusyThreadCount() {
        return this.busyThreadCount.get();
    }

    public int getTotalThreadCount() {
        return this.threads.size();
    }

    public void managedBlock(BooleanSupplier isDone) {
        this.managedBlocker.managedBlock(isDone);
    }

    public static final class WorkerThread extends Thread implements GlobalChunkBuildContext.Holder {
        private final ChunkBuildContext context;

        public WorkerThread(Runnable runnable, String name, ChunkBuildContext context) {
            super(runnable, name);
            this.context = context;
        }

        @Override
        public ChunkBuildContext embeddium$getGlobalContext() {
            return context;
        }
    }

    private class WorkerRunnable implements Runnable {
        // Making this thread-local provides a small boost to performance by avoiding the overhead in synchronizing
        // caches between different CPU cores
        private final ChunkBuildContext context;

        public WorkerRunnable(ChunkBuildContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            // Run until the chunk builder shuts down
            while (ChunkBuilder.this.queue.isRunning()) {
                ChunkJob job;

                try {
                    job = ChunkBuilder.this.queue.waitForNextJob();
                } catch (InterruptedException ignored) {
                    continue;
                }

                if (job == null) {
                    // might mean we are not running anymore... go around and check isRunning
                    continue;
                }

                ChunkBuilder.this.busyThreadCount.getAndIncrement();

                try {
                    job.execute(this.context);
                } finally {
                    this.context.cleanup();

                    ChunkBuilder.this.busyThreadCount.decrementAndGet();
                }
            }
        }
    }

    public interface ManagedBlocker {
        ManagedBlocker NONE = isDone -> {
            while (!isDone.getAsBoolean()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        };

        void managedBlock(BooleanSupplier isDone);
    }
}
