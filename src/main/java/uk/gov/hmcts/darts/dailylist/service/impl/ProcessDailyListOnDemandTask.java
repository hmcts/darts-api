package uk.gov.hmcts.darts.dailylist.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.task.api.AutomatedTasksApi;
import uk.gov.hmcts.darts.task.runner.AutomatedOnDemandTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.time.Instant;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;

/**
 * An on demand task that allows us to lock around the daily list shed lock and process a sync/async runnable task within that lock, registering
 * the outcome as appropriate.
 */
@RequiredArgsConstructor
@Component
@Getter
public class ProcessDailyListOnDemandTask implements AutomatedOnDemandTask {
    private AutomatedTaskStatus lastOutcome = AutomatedTaskStatus.NOT_STARTED;
    private final AutomatedTasksApi automatedTasksApi;

    @SuppressWarnings("PMD.UnusedAssignment")//Required to keep accurate track of process
    private void processDailyList(Runnable runnable) {
        LockingTaskExecutor executor = automatedTasksApi.getLockingTaskExecutor();
        executor.executeWithLock((Runnable)() -> {
            lastOutcome = AutomatedTaskStatus.IN_PROGRESS;

            try {
                LockAssert.assertLocked();
                runnable.run();
            } catch (IllegalStateException exception) {
                lastOutcome = AutomatedTaskStatus.LOCK_FAILED;
                throw exception;
            } catch (Exception e) {
                lastOutcome = AutomatedTaskStatus.FAILED;
                throw e;
            }
            lastOutcome = AutomatedTaskStatus.COMPLETED;
       }, new LockConfiguration(
           Instant.now(),
           PROCESS_DAILY_LIST_TASK_NAME.getTaskName(),
           automatedTasksApi.getLockAtMostFor(),
           automatedTasksApi.getLockAtLeastFor()));
        }

    @Override
    public void run(Runnable runnable) {
        processDailyList(runnable);
    }

    @Override
    public String getTaskName() {
        return PROCESS_DAILY_LIST_TASK_NAME.getTaskName();
    }

    @Override
    public AutomatedTaskStatus getAutomatedTaskStatus() {
        return lastOutcome;
    }
}