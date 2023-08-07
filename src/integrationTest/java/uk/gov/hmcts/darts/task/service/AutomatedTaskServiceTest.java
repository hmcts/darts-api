package uk.gov.hmcts.darts.task.service;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessDailyListAutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.testutils.IntegrationPerClassBase;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(OrderAnnotation.class)
class AutomatedTaskServiceTest extends IntegrationPerClassBase {
    @Autowired
    private AutomatedTaskService automatedTaskService;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;

    @Autowired
    private LockProvider lockProvider;

    @Test
    @Order(1)
    void givenAutomatedTaskVerifyStatusBeforeAndAfterRunning() throws InterruptedException {
        AbstractLockableAutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider);

        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression());

        AutomatedTaskStatus originalAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
        assertEquals(AutomatedTaskStatus.NOT_STARTED, originalAutomatedTaskStatus);

        boolean result1 = automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(
            automatedTask.getTaskName(), true, "*/7 * * * * *");
        assertTrue(result1);

        Thread.sleep(10_000);

        AutomatedTaskStatus newAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
        assertEquals(AutomatedTaskStatus.COMPLETED, newAutomatedTaskStatus);

        boolean result2 = automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(
            originalAutomatedTaskEntity.get().getTaskName(),
            true,
            originalAutomatedTaskEntity.get().getCronExpression());
        assertTrue(result2);
    }

    @Test
    @Order(2)
    void givenConfiguredTaskCancelProcessDailyList() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider);

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(automatedTask.getTaskName(), mayInterruptIfRunning);
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    @Order(3)
    void givenConfiguredTasksUpdateCronExpressionAndResetCronExpression() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider);
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression());
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    @Order(4)
    void cancelAutomatedTaskAndUpdateCronExpression() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider);
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression());

        boolean mayInterruptIfRunning = false;
        automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(
            automatedTask.getTaskName(), mayInterruptIfRunning, "*/10 * * * * *");

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    @Order(5)
    void givenNonExistingAutomatedTaskNameUpdateAutomatedTaskCronExpressionThrowsDartsApiException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> automatedTaskService.updateAutomatedTaskCronExpression(
                "Test", "*/10 * * * * *")
        );

        assertEquals(AutomatedTaskSetupError.FAILED_TO_FIND_AUTOMATED_TASK, exception.getError());
    }

    @Test
    @Order(6)
    @SuppressWarnings("PMD.LawOfDemeter")
    void givenExistingAutomatedTaskNameAndInvalidCronExpressionThrowsDartsApiException() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider);

        var exception = assertThrows(
            DartsApiException.class,
            () -> automatedTaskService.updateAutomatedTaskCronExpression(
                automatedTask.getTaskName(), "*/10 * * * *")
        );

        assertEquals(AutomatedTaskSetupError.INVALID_CRON_EXPRESSION, exception.getError());
        assertEquals(AutomatedTaskSetupError.INVALID_CRON_EXPRESSION.getErrorTypeNumeric(), exception.getError().getErrorTypeNumeric());
        assertEquals(AutomatedTaskSetupError.INVALID_CRON_EXPRESSION.getErrorTypePrefix(), exception.getError().getErrorTypePrefix());
    }

    @Test
    @Order(7)
    void updateCronExpressionWithoutRescheduleForcingTaskToSkipRunning() throws InterruptedException {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider);
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(automatedTask.getTaskName(), false,"*/6 * * * * *");

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression());
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        Thread.sleep(8000);

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity2 =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        AutomatedTaskEntity automatedTaskEntity2 = updatedAutomatedTaskEntity2.get();
        automatedTaskEntity2.setCronExpression("*/30 * * * * *");
        automatedTaskRepository.saveAndFlush(automatedTaskEntity2);

        log.info("TEST - Updated task {} cron expression {}  via database directly", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity2.get().getCronExpression());
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity2.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity2.get().getCronExpression());
        Thread.sleep(6000);

        AutomatedTaskStatus newAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
        assertEquals(AutomatedTaskStatus.SKIPPED, newAutomatedTaskStatus);

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    private static void displayTasks(Set<ScheduledTask> scheduledTasks) {
        log.info("Number of scheduled tasks " + scheduledTasks.size());
        scheduledTasks.forEach(
            scheduledTask -> {
                Task task = scheduledTask.getTask();
                if (task instanceof CronTask cronTask) {
                    log.info("CronTask expression: {} Runnable: {}",
                             cronTask.getExpression(), cronTask.getRunnable());
                } else if (task instanceof TriggerTask triggerTask) {
                    log.info("TriggerTask trigger: {} Runnable: {}",
                             triggerTask.getTrigger(), triggerTask.getRunnable());
                } else if (task instanceof FixedRateTask fixedRateTask) {
                    log.info("FixedRateTask initial delay duration: {} Interval duration: {} ",
                             fixedRateTask.getInitialDelayDuration(), fixedRateTask.getIntervalDuration());
                } else if (task instanceof FixedDelayTask fixedDelayTask) {
                    log.info("FixedDelayTask initial delay duration: {} Interval duration: {}",
                             fixedDelayTask.getInitialDelayDuration(), fixedDelayTask.getIntervalDuration());
                } else {
                    log.info("Unknown task type: " + task);
                }
            }
        );
    }
}
