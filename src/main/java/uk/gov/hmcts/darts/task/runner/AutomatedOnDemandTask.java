package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.util.concurrent.CompletableFuture;

/**
 * An interface that describes an on demand system task that can be fired off synchronously or asynchronously using a shedlock
 * lock registering the outcome appropriately.
 */
public interface AutomatedOnDemandTask {
    String getTaskName();

    AutomatedTaskStatus getAutomatedTaskStatus();

    /**
     * Runs the task as an async job.
     */
    default void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(() -> run(runnable));
    }

    /**
     * Runs the task as a synchronous job.
     * @param runnable The runnable to be executed
     */
    void run(Runnable runnable);
}