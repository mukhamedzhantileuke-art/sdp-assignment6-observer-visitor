package kz.aitu.sdp.assignment6.concurrency;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Concurrency coordinator managing the producer thread pool on an {@link ExecutorService}.
 * Adheres to Clean Code chapter 13: thread pool size is configurable, and shutdown is graceful.
 */
public class GridSimulationEngine {

    private final ExecutorService executorService;
    private final int threadCount;

    public GridSimulationEngine(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be greater than 0");
        }
        this.threadCount = threadCount;
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    public void submitProducer(Runnable producerTask) {
        Objects.requireNonNull(producerTask, "producerTask cannot be null");
        executorService.submit(producerTask);
    }

    /**
     * Gracefully shuts down the executor service, allowing running producers to complete
     * without dropping events or leaking threads.
     *
     * @param timeoutMs maximum milliseconds to await orderly termination
     * @return true if all tasks terminated cleanly, false otherwise
     */
    public boolean shutdownGracefully(long timeoutMs) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                return executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
            }
            return true;
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int getThreadCount() {
        return threadCount;
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public boolean isTerminated() {
        return executorService.isTerminated();
    }
}
