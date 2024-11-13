package uk.gov.hmcts.darts.task.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronExpression;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.datamanagement.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ApplyRetentionCaseAssociatedObjectsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.ArmRetentionEventDateCalculatorAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.config.CloseOldCasesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.CloseUnfinishedTranscriptionsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.DailyListAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.ExternalDataStoreDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.GenerateCaseDocumentAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.GenerateCaseDocumentForRetentionDateAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.InboundAudioDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.InboundToUnstructuredAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.OutboundAudioDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.ProcessArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.ProcessDailyListAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.RemoveDuplicatedEventsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.UnstructuredAudioDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.UnstructuredToArmAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ApplyRetentionCaseAssociatedObjectsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ArmRetentionEventDateCalculatorAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.CloseOldCasesAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.CloseUnfinishedTranscriptionsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.DailyListAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ExternalDataStoreDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.GenerateCaseDocumentAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.GenerateCaseDocumentForRetentionDateAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.InboundAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.InboundToUnstructuredAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.OutboundAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessArmResponseFilesAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessDailyListAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.RemoveDuplicatedEventsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.UnstructuredAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.UnstructuredToArmAutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsProcessor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.COMPLETED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.FAILED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.NOT_STARTED;
import static uk.gov.hmcts.darts.test.common.AwaitabilityUtil.waitForMax10SecondsWithOneSecondPoll;

@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
class AutomatedTaskServiceTest extends IntegrationBase {

    @Autowired
    private AutomatedTaskService automatedTaskService;
    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;
    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @Autowired
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Autowired
    private TranscriptionsProcessor transcriptionsProcessor;

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
    private AutomatedTaskProcessorFactory taskProcessorFactory;

    @Autowired
    private CleanupArmResponseFilesService cleanupArmResponseFilesService;

    @Autowired
    private CloseOldCasesProcessor closeOldCasesProcessor;

    @Autowired
    private DailyListService dailyListService;

    @SpyBean
    private CaseRepository caseRepository;

    @Autowired
    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;

    @Autowired
    private ApplyRetentionCaseAssociatedObjectsProcessor applyRetentionCaseAssociatedObjectsProcessor;

    @Autowired
    private InboundAnnotationTranscriptionDeleterProcessor armTranscriptionAndAnnotationDeleterProcessor;

    @Autowired
    private RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor;

    @Autowired
    private LogApi logApi;

    @Autowired
    private LockService lockService;

    @Autowired
    UnstructuredToArmBatchProcessorImpl unstructuredToArmBatchProcessor;


    @MockBean
    private UserIdentity userIdentity;

    private UserAccountEntity testUser;

    @BeforeEach
    void setupData() {
        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(testUser);
    }

    @AfterEach
    public void resetTables() {
        if (nonNull(testUser)) {
            try (EntityManager em = dartsDatabase.getEntityManagerFactory().createEntityManager()) {
                em.getTransaction().begin();

                final Query query1 = em.createNativeQuery("truncate table darts.automated_task_aud");
                query1.executeUpdate();
                final Query query2 = em.createNativeQuery("update darts.automated_task set last_modified_by = 0 where last_modified_by = " + testUser.getId());
                query2.executeUpdate();
                em.getTransaction().commit();
            }
        }
    }

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

                    if (triggerTask.getRunnable() instanceof AbstractLockableAutomatedTask automatedTask) {
                        log.info("TriggerTask name: {}, cron expression: {}",
                                 automatedTask.getTaskName(), automatedTask.getLastCronExpression());
                    } else {
                        log.info("TriggerTask trigger: {} Runnable: {}",
                                 triggerTask.getTrigger(), triggerTask.getRunnable());
                    }
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
    void givenSuccessfullyStartedTaskFailsDuringExecutionThenStatusIsSetToFailed() {
        GenerateCaseDocumentAutomatedTask automatedTask
            = new GenerateCaseDocumentAutomatedTask(
            automatedTaskRepository, mock(GenerateCaseDocumentAutomatedTaskConfig.class), taskProcessorFactory, logApi, lockService
        );
        doThrow(ArithmeticException.class).when(caseRepository).findCasesNeedingCaseDocumentGenerated(any(), any());

        automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(automatedTask.getTaskName(), true, "*/7 * * * * *");

        waitForMax10SecondsWithOneSecondPoll(() -> {
            AutomatedTaskStatus newAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
            return newAutomatedTaskStatus == FAILED;
        });
    }

    @Test
    void givenAutomatedTaskVerifyStatusBeforeAndAfterRunning() {
        ProcessDailyListAutomatedTask automatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository, mock(ProcessDailyListAutomatedTaskConfig.class), null, logApi, lockService
        );

        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());

        // this may have transitioned to complete. Let's check the history to ensure that
        // we started with NOT_STARTED state
        assertTrue(automatedTask.hasTransitionState(NOT_STARTED));

        boolean result1 = automatedTaskService.cancelAutomatedTaskAndUpdateCronExpression(
            automatedTask.getTaskName(), true, "*/7 * * * * *");
        assertTrue(result1);

        waitForMax10SecondsWithOneSecondPoll(() -> {
            AutomatedTaskStatus newAutomatedTaskStatus = automatedTaskService.getAutomatedTaskStatus(automatedTask.getTaskName());
            return COMPLETED.equals(newAutomatedTaskStatus);
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
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository, mock(ProcessDailyListAutomatedTaskConfig.class), null, logApi, lockService
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
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository, mock(ProcessDailyListAutomatedTaskConfig.class), null, logApi, lockService
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
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository, mock(ProcessDailyListAutomatedTaskConfig.class), null, logApi, lockService
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
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository, mock(ProcessDailyListAutomatedTaskConfig.class), null, logApi, lockService
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
    void updateCronExpressionWithoutRescheduleForcingTaskToSkipRunning() {
        AutomatedTask automatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository, mock(ProcessDailyListAutomatedTaskConfig.class), null, logApi, lockService
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
            return COMPLETED.equals(newAutomatedTaskStatus);
        });

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void givenConfiguredTasksUpdateCronAndResetCronForCloseUnfinishedTranscriptionsAutomatedTask() {
        AutomatedTask automatedTask =
            new CloseUnfinishedTranscriptionsAutomatedTask(
                automatedTaskRepository,
                mock(CloseUnfinishedTranscriptionsAutomatedTaskConfig.class),
                transcriptionsProcessor,
                logApi,
                lockService
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
                mock(CloseUnfinishedTranscriptionsAutomatedTaskConfig.class),
                transcriptionsProcessor,
                logApi,
                lockService
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
                mock(OutboundAudioDeleterAutomatedTaskConfig.class),
                outboundAudioDeleterProcessor,
                logApi,
                lockService
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
                                                  mock(OutboundAudioDeleterAutomatedTaskConfig.class),
                                                  outboundAudioDeleterProcessor, logApi, lockService
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
                                                 mock(InboundAudioDeleterAutomatedTaskConfig.class),
                                                 inboundAudioDeleterProcessor, logApi, lockService
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
                mock(ExternalDataStoreDeleterAutomatedTaskConfig.class),
                externalInboundDataStoreDeleter, externalUnstructuredDataStoreDeleter, externalOutboundDataStoreDeleter, logApi,
                lockService
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
                mock(ExternalDataStoreDeleterAutomatedTaskConfig.class),
                externalInboundDataStoreDeleter,
                externalUnstructuredDataStoreDeleter,
                externalOutboundDataStoreDeleter,
                logApi,
                lockService
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
                                                   mock(InboundToUnstructuredAutomatedTaskConfig.class),
                                                   inboundToUnstructuredProcessor, logApi, lockService
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
                                                      mock(UnstructuredAudioDeleterAutomatedTaskConfig.class),
                                                      unstructuredAudioDeleterProcessor, logApi,
                                                      lockService
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
    void givenConfiguredTasksUpdateCronAndResetCronForUnstructuredToArmAutomatedTask() {
        AutomatedTask automatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                mock(UnstructuredToArmAutomatedTaskConfig.class),
                unstructuredToArmBatchProcessor,
                logApi,
                lockService
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
    void givenConfiguredTaskCancelUnstructuredToArmAutomatedTask() {
        AutomatedTask automatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                mock(UnstructuredToArmAutomatedTaskConfig.class),
                unstructuredToArmBatchProcessor,
                logApi,
                lockService
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
    void givenConfiguredTasksUpdateCronAndResetCronForProcessArmResponseFilesAutomatedTask() {
        AutomatedTask automatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                mock(ProcessArmResponseFilesAutomatedTaskConfig.class),
                taskProcessorFactory,
                logApi,
                lockService
            );

        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());

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
    void givenConfiguredTaskCancelProcessArmResponseFilesAutomatedTask() {
        AutomatedTask automatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                mock(ProcessArmResponseFilesAutomatedTaskConfig.class),
                taskProcessorFactory,
                logApi,
                lockService
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
    void givenConfiguredTaskCloseOldCasesAutomatedTask() {
        AutomatedTask automatedTask =
            new CloseOldCasesAutomatedTask(
                automatedTaskRepository,
                mock(CloseOldCasesAutomatedTaskConfig.class),
                logApi,
                lockService,
                closeOldCasesProcessor
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
    void givenConfiguredTaskCancelCloseOldCasesAutomatedTask() {
        AutomatedTask automatedTask =
            new CloseOldCasesAutomatedTask(
                automatedTaskRepository,
                mock(CloseOldCasesAutomatedTaskConfig.class),
                logApi,
                lockService,
                closeOldCasesProcessor
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
    void givenConfiguredTaskGenerateCaseDocumentAutomatedTask() {
        AutomatedTask automatedTask =
            new GenerateCaseDocumentAutomatedTask(
                automatedTaskRepository,
                mock(GenerateCaseDocumentAutomatedTaskConfig.class),
                taskProcessorFactory,
                logApi,
                lockService
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
    void givenConfiguredTaskCancelGenerateCaseDocumentAutomatedTask() {
        AutomatedTask automatedTask =
            new GenerateCaseDocumentAutomatedTask(
                automatedTaskRepository,
                mock(GenerateCaseDocumentAutomatedTaskConfig.class),
                taskProcessorFactory,
                logApi,
                lockService
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
    void givenConfiguredTaskGenerateRemoveDuplicateEventAutomatedTask() {
        AutomatedTask automatedTask =
            new RemoveDuplicatedEventsAutomatedTask(
                automatedTaskRepository,
                mock(RemoveDuplicatedEventsAutomatedTaskConfig.class),
                removeDuplicateEventsProcessor,
                logApi,
                lockService
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
    void givenConfiguredTaskCancelRemoveDuplicateEventAutomatedTask() {
        AutomatedTask automatedTask =
            new RemoveDuplicatedEventsAutomatedTask(
                automatedTaskRepository,
                mock(RemoveDuplicatedEventsAutomatedTaskConfig.class),
                removeDuplicateEventsProcessor,
                logApi,
                lockService
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
    void canUpdatedCronForDailyListHouseKeepingTask() {
        var automatedTask = new DailyListAutomatedTask(
            automatedTaskRepository,
            mock(DailyListAutomatedTaskConfig.class),
            dailyListService,
            logApi,
            lockService
        );
        var originalAutomatedTaskEntity = automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());

        automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/9 * * * * *");

        var updatedAutomatedTaskEntity = automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        assertEquals(originalAutomatedTaskEntity.get().getTaskName(), updatedAutomatedTaskEntity.get().getTaskName());
        assertNotEquals(originalAutomatedTaskEntity.get().getCronExpression(), updatedAutomatedTaskEntity.get().getCronExpression());

        automatedTaskService.updateAutomatedTaskCronExpression(
            automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());
    }

    @Test
    void canCancelDailyListAutomatedTask() {
        AutomatedTask automatedTask =
            new DailyListAutomatedTask(
                automatedTaskRepository,
                mock(DailyListAutomatedTaskConfig.class),
                dailyListService,
                logApi,
                lockService
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
    void givenConfiguredTaskApplyRetentionCaseAssociatedObjectsAutomatedTask() {
        AutomatedTask automatedTask =
            new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
                automatedTaskRepository,
                mock(ApplyRetentionCaseAssociatedObjectsAutomatedTaskConfig.class),
                applyRetentionCaseAssociatedObjectsProcessor,
                logApi,
                lockService
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
    void givenConfiguredTaskCancelApplyRetentionCaseAssociatedObjectsAutomatedTask() {
        AutomatedTask automatedTask =
            new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
                automatedTaskRepository,
                mock(ApplyRetentionCaseAssociatedObjectsAutomatedTaskConfig.class),
                applyRetentionCaseAssociatedObjectsProcessor,
                logApi,
                lockService
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
    void givenConfiguredTasksUpdateCronAndResetCronForArmRetentionEventDateCalculatorAutomatedTask() {
        AutomatedTask automatedTask =
            new ArmRetentionEventDateCalculatorAutomatedTask(
                automatedTaskRepository,
                mock(ArmRetentionEventDateCalculatorAutomatedTaskConfig.class),
                armRetentionEventDateProcessor,
                logApi,
                lockService
            );
        Optional<AutomatedTaskEntity> originalAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(automatedTask.getTaskName());
        log.info("TEST - Original task {} cron expression {}", automatedTask.getTaskName(), originalAutomatedTaskEntity.get().getCronExpression());

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
    void givenConfiguredTaskCancelArmRetentionEventDateCalculatorAutomatedTask() {
        AutomatedTask automatedTask =
            new ArmRetentionEventDateCalculatorAutomatedTask(
                automatedTaskRepository,
                mock(ArmRetentionEventDateCalculatorAutomatedTaskConfig.class),
                armRetentionEventDateProcessor,
                logApi,
                lockService
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
    void givenConfiguredTaskInboundTranscriptionAndAnnotationDeleterAutomatedTask() throws Exception {
        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(
            AutomatedTaskName.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName(),
            mayInterruptIfRunning
        );
        assertTrue(taskCancelled);

        automatedTaskService.reloadTaskByName(AutomatedTaskName.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName());
    }

    @Test
    void givenConfiguredTaskUnstructuredTranscriptionAndAnnotationDeleterAutomatedTask() throws Exception {
        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        boolean mayInterruptIfRunning = false;
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(
            AutomatedTaskName.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName(),
            mayInterruptIfRunning
        );
        assertTrue(taskCancelled);

        automatedTaskService.reloadTaskByName(AutomatedTaskName.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName());
    }

    @Test
    void givenConfiguredTaskGenerateCaseDocumentForRetentionDateAutomatedTask() {
        AutomatedTask automatedTask =
            new GenerateCaseDocumentForRetentionDateAutomatedTask(
                automatedTaskRepository,
                mock(GenerateCaseDocumentForRetentionDateAutomatedTaskConfig.class),
                taskProcessorFactory,
                logApi,
                lockService
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
    void givenConfiguredTaskCancelGenerateCaseDocumentForRetentionDateAutomatedTask() {
        AutomatedTask automatedTask =
            new GenerateCaseDocumentForRetentionDateAutomatedTask(
                automatedTaskRepository,
                mock(GenerateCaseDocumentForRetentionDateAutomatedTaskConfig.class),
                taskProcessorFactory,
                logApi,
                lockService
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
    void verifyCronExpressionRunsBetween8pmAnd8amAt50MinutesPastTheHour() {
        CronExpression cronTrigger = CronExpression.parse("0 50 20-23,0-7 * * *");

        LocalDateTime next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 19, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 20, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 20, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 20, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 21, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 21, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 22, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 22, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 23, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 23, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 0, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 0, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 1, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 1, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 2, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 2, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 3, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 3, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 4, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 4, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 5, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 5, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 6, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 6, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 7, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 7, 50), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 2, 8, 49));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 20, 50), next);

    }

    @Test
    void verifyCronExpressionRunsEveryHourAt15MinutesPastTheHour() {
        CronExpression cronTrigger = CronExpression.parse("0 15 * * * *");

        LocalDateTime next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 10, 14));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 10, 15), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 10, 16));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 11, 15), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 23, 59));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 0, 15), next);

    }

    @Test
    void verifyCronExpressionRunsEvery5MinutesAt1MinutePast() {
        CronExpression cronTrigger = CronExpression.parse("0 1/5 * * * *");

        LocalDateTime next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 10, 15));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 10, 16), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 10, 16));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 1, 10, 21), next);

        next = cronTrigger.next(LocalDateTime.of(2024, 9, 1, 23, 59));
        log.info("Next Execution Time: " + next);
        assertEquals(LocalDateTime.of(2024, 9, 2, 0, 1), next);

        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);
    }
}