package uk.gov.hmcts.darts.task.service;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
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
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.CloseUnfinishedTranscriptionsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ExternalDataStoreDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.InboundAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.InboundToUnstructuredAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.OutboundAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessDailyListAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.UnstructuredAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.UnstructuredToArmAutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.testutils.IntegrationPerClassBase;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.testutils.AwaitabilityUtil.waitForMax10SecondsWithOneSecondPoll;

@Slf4j
@TestMethodOrder(OrderAnnotation.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AutomatedTaskServiceTest extends IntegrationPerClassBase {

    @Autowired
    private AutomatedTaskService automatedTaskService;
    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;
    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @Autowired
    private LockProvider lockProvider;
    @Autowired
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Autowired
    private TranscriptionsApi transcriptionsApi;

    @Autowired
    private OutboundAudioDeleterProcessor outboundAudioDeleterProcessor;

    @Autowired
    private ExternalInboundDataStoreDeleter externalInboundDataStoreDeleter;
    @Autowired
    private ExternalUnstructuredDataStoreDeleter externalUnstructuredDataStoreDeleter;
    @Autowired
    private ExternalOutboundDataStoreDeleter externalOutboundDataStoreDeleter;

    @Autowired
    private InboundAudioDeleterProcessor inboundAudioDeleterProcessor;

    @Autowired
    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    @Autowired
    private UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;

    @Autowired
    private UnstructuredToArmProcessor unstructuredToArmProcessor;

    private static void displayTasks(Set<ScheduledTask> scheduledTasks) {
        log.info("Number of scheduled tasks " + scheduledTasks.size());
        scheduledTasks.forEach(
            scheduledTask -> {
                Task task = scheduledTask.getTask();
                if (task instanceof CronTask cronTask) {
                    log.info("CronTask expression: {} Runnable: {}",
                             cronTask.getExpression(), cronTask.getRunnable()
                    );
                } else if (task instanceof TriggerTask triggerTask) {
                    log.info("TriggerTask trigger: {} Runnable: {}",
                             triggerTask.getTrigger(), triggerTask.getRunnable()
                    );
                } else if (task instanceof FixedRateTask fixedRateTask) {
                    log.info("FixedRateTask initial delay duration: {} Interval duration: {} ",
                             fixedRateTask.getInitialDelayDuration(), fixedRateTask.getIntervalDuration()
                    );
                } else if (task instanceof FixedDelayTask fixedDelayTask) {
                    log.info("FixedDelayTask initial delay duration: {} Interval duration: {}",
                             fixedDelayTask.getInitialDelayDuration(), fixedDelayTask.getIntervalDuration()
                    );
                } else {
                    log.info("Unknown task type: " + task);
                }
            }
        );
    }

    @Test
    void givenAutomatedTaskVerifyStatusBeforeAndAfterRunning() throws InterruptedException {
        ProcessDailyListAutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider,
                                                                                        automatedTaskConfigurationProperties
        );

        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        // this may have transitioned to complete. Lets check the history to ensure that
        // we started with not started state
        assertTrue(automatedTask.hasTransitionState(AutomatedTaskStatus.NOT_STARTED));

        boolean result1 = automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(
            automatedTask.getTaskName(), true, "*/7 * * * * *");
        assertTrue(result1);

        waitForMax10SecondsWithOneSecondPoll(() -> {
            AutomatedTaskStatus newAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
            return AutomatedTaskStatus.COMPLETED.equals(newAutomatedTaskStatus);
        });

        boolean result2 = automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(
            originalAutomatedTaskEntity.get().getTaskName(),
            true,
            originalAutomatedTaskEntity.get().getCronExpression()
        );
        assertTrue(result2);
    }

    @Test
    void givenConfiguredTaskCancelProcessDailyList() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider,
                                                                        automatedTaskConfigurationProperties
        );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(automatedTask.getTaskName(), mayInterruptIfRunning);
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    void givenConfiguredTasksUpdateCronExpressionAndResetCronExpression() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider,
                                                                        automatedTaskConfigurationProperties
        );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void cancelAutomatedTaskAndUpdateCronExpression() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider,
                                                                        automatedTaskConfigurationProperties
        );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

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

    void givenNonExistingAutomatedTaskNameUpdateAutomatedTaskCronExpressionThrowsDartsApiException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> automatedTaskService.updateAutomatedTaskCronExpression(
                "Test", "*/10 * * * * *")
        );

        assertEquals(AutomatedTaskSetupError.FAILED_TO_FIND_AUTOMATED_TASK, exception.getError());
    }

    @Test
    @SuppressWarnings("PMD.LawOfDemeter")
    void givenExistingAutomatedTaskNameAndInvalidCronExpressionThrowsDartsApiException() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider,
                                                                        automatedTaskConfigurationProperties
        );

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
    void updateCronExpressionWithoutRescheduleForcingTaskToSkipRunning() throws InterruptedException {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(automatedTaskRepository, lockProvider,
                                                                        automatedTaskConfigurationProperties
        );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(automatedTask.getTaskName(), false, "*/5 * * * * *");

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity2 =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        AutomatedTaskEntity automatedTaskEntity2 = updatedAutomatedTaskEntity2.get();
        automatedTaskEntity2.setCronExpression("*/4 * * * * *");
        automatedTaskRepository.saveAndFlush(automatedTaskEntity2);

        log.info("TEST - Updated task {} cron expression {}  via database directly", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity2.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity2.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity2.get().getCronExpression());

        waitForMax10SecondsWithOneSecondPoll(() -> {
            AutomatedTaskStatus newAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
            return AutomatedTaskStatus.COMPLETED.equals(newAutomatedTaskStatus);
        });

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void givenConfiguredTasksUpdateCronAndResetCronForCloseUnfinishedTranscriptionsAutomatedTask() {
        AutomatedTask automatedTask =
            new CloseUnfinishedTranscriptionsAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                transcriptionsApi
            );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void givenConfiguredTaskCancelCloseUnfinishedTranscriptionsAutomatedTask() {
        AutomatedTask automatedTask =
            new CloseUnfinishedTranscriptionsAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                transcriptionsApi
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(automatedTask.getTaskName(), mayInterruptIfRunning);
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    void givenConfiguredTasksUpdateCronAndResetCronForOutboundAudioDeleterAutomatedTask() {
        AutomatedTask automatedTask =
            new OutboundAudioDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                outboundAudioDeleterProcessor
            );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void givenConfiguredTaskCancelOutboundAudioDeleterAutomatedTask() {
        AutomatedTask automatedTask =
            new OutboundAudioDeleterAutomatedTask(automatedTaskRepository,
                                                  lockProvider,
                                                  automatedTaskConfigurationProperties, outboundAudioDeleterProcessor
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(automatedTask.getTaskName(), mayInterruptIfRunning);
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    void givenConfiguredTaskCancelInboundAudioDeleterAutomatedTask() {
        AutomatedTask automatedTask =
            new InboundAudioDeleterAutomatedTask(automatedTaskRepository,
                                                 lockProvider,
                                                 automatedTaskConfigurationProperties, inboundAudioDeleterProcessor
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(
            automatedTask.getTaskName(),
            mayInterruptIfRunning
        );
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    void givenConfiguredTasksUpdateCronAndResetCronForExternalDataDeleterAutomatedTask() {
        AutomatedTask automatedTask =
            new ExternalDataStoreDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                externalInboundDataStoreDeleter, externalUnstructuredDataStoreDeleter, externalOutboundDataStoreDeleter
            );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void givenConfiguredTaskCancelExternalDataDeleterAutomatedTask() {
        AutomatedTask automatedTask =
            new ExternalDataStoreDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                externalInboundDataStoreDeleter,
                externalUnstructuredDataStoreDeleter,
                externalOutboundDataStoreDeleter
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(automatedTask.getTaskName(), mayInterruptIfRunning);
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    void givenConfiguredTaskCancelInboundToUnstructuredAutomatedTask() {
        AutomatedTask automatedTask =
            new InboundToUnstructuredAutomatedTask(automatedTaskRepository,
                                                   lockProvider,
                                                   automatedTaskConfigurationProperties, inboundToUnstructuredProcessor
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(
            automatedTask.getTaskName(),
            mayInterruptIfRunning
        );
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    void givenConfiguredTaskCancelUnstructuredAudioDeleterAutomatedTask() {
        AutomatedTask automatedTask =
            new UnstructuredAudioDeleterAutomatedTask(automatedTaskRepository,
                                                      lockProvider,
                                                      automatedTaskConfigurationProperties,
                                                      unstructuredAudioDeleterProcessor
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(
            automatedTask.getTaskName(),
            mayInterruptIfRunning
        );
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }

    @Test
    @Order(16)
    void givenConfiguredTasksUpdateCronAndResetCronForUnstructuredToArmAutomatedTask() {
        AutomatedTask automatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                unstructuredToArmProcessor
            );

        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(),
                 originalAutomatedTaskEntity.get().getCronExpression()
        );

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        Optional<AutomatedTaskEntity> updatedAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Updated task {} cron expression {}", automatedTask.getTaskName(),
                 updatedAutomatedTaskEntity.get().getCronExpression()
        );
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    @Order(17)
    void givenConfiguredTaskCancelUnstructuredToArmAutomatedTask() {
        AutomatedTask automatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                unstructuredToArmProcessor
            );

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(
            automatedTask.getTaskName(),
            mayInterruptIfRunning
        );
        assertTrue(taskCancelled);

        log.info("About to reload task {}", automatedTask.getTaskName());
        automatedTaskService.reloadTaskByName(automatedTask.getTaskName());

    }
}
