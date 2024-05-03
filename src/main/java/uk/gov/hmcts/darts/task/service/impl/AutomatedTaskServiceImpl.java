package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.UnstructuredToArmProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.model.TriggerAndAutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutomatedTaskName;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ApplyRetentionAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ApplyRetentionCaseAssociatedObjectsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ArmRetentionEventDateCalculatorAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.CleanupArmResponseFilesAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.CloseOldCasesAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.CloseUnfinishedTranscriptionsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.DailyListAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ExternalDataStoreDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.InboundAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.InboundToUnstructuredAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.OutboundAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessArmResponseFilesAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessDailyListAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.UnstructuredAudioDeleterAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.UnstructuredToArmAutomatedTask;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError.FAILED_TO_FIND_AUTOMATED_TASK;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError.INVALID_CRON_EXPRESSION;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.APPLY_RETENTION_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLOSE_OLD_CASES_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.DAILY_LIST_HOUSEKEEPING_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.EXTERNAL_DATASTORE_DELETER_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.INBOUND_AUDIO_DELETER_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.INBOUND_TO_UNSTRUCTURED_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.OUTBOUND_AUDIO_DELETER_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_ARM_RESPONSE_FILES_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_AUDIO_DELETER_TASK_NAME;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_TO_ARM_TASK_NAME;


/**
 * Refer to <a href="https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-cron-expression">...</a>
 * for details of spring cron expressions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.TooFewBranchesForASwitchStatement", "PMD.SingularField"})
public class AutomatedTaskServiceImpl implements AutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;

    private final ScheduledTaskHolder taskHolder;

    private final LockProvider lockProvider;

    private final TaskScheduler taskScheduler;

    private final AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    private final Map<String, Trigger> taskTriggers = new ConcurrentHashMap<>();

    private final DailyListProcessor dailyListProcessor;

    private final OutboundAudioDeleterProcessor outboundAudioDeleterProcessor;

    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    private final UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;

    private final TranscriptionsProcessor transcriptionsProcessor;

    private final InboundAudioDeleterProcessor inboundAudioDeleterProcessor;

    private final ExternalInboundDataStoreDeleter inboundDataStoreDeleter;

    private final ExternalUnstructuredDataStoreDeleter unstructuredDataStoreDeleter;

    private final ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;

    private final UnstructuredToArmProcessorFactory unstructuredToArmProcessor;

    private final ArmResponseFilesProcessor armResponseFilesProcessor;

    private final ApplyRetentionProcessor applyRetentionProcessor;

    private final ApplyRetentionCaseAssociatedObjectsProcessor applyRetentionCaseAssociatedObjectsProcessor;

    private final CleanupArmResponseFilesService cleanupArmResponseFilesService;

    private final CloseOldCasesProcessor closeOldCasesProcessor;

    private final DailyListService dailyListService;

    private final ArmRetentionEventDateProcessor armRetentionEventDateProcessor;

    private final LogApi logApi;

    private final List<AbstractLockableAutomatedTask> automatedTasks = new ArrayList<>();

    @Override
    public void configureAndLoadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("Automated tasks are loading");
        addProcessDailyListToTaskRegistrar(taskRegistrar);
        addCloseNonCompletedTranscriptionsAutomatedTaskToTaskRegistrar(taskRegistrar);
        addOutboundAudioDeleterToTaskRegistrar(taskRegistrar);
        addInboundToUnstructuredTaskRegistrar(taskRegistrar);
        addInboundAudioDeleterToTaskRegistrar(taskRegistrar);
        addExternalDataStoreDeleterToTaskRegistrar(taskRegistrar);
        addUnstructuredAudioDeleterAutomatedTaskToTaskRegistrar(taskRegistrar);
        addUnstructuredToArmTaskRegistrar(taskRegistrar);
        addProcessArmResponseFilesTaskRegistrar(taskRegistrar);
        addApplyRetentionToTaskRegistrar(taskRegistrar);
        addCaseObjectApplyRetentionToTaskRegistrar(taskRegistrar);
        addCleanupArmResponseFilesTaskRegistrar(taskRegistrar);
        addCloseOldCasesTaskRegistrar(taskRegistrar);

        addDailyListHouseKeepingToTaskRegistrar(taskRegistrar);
        addArmRetentionEventDateCalculatorToTaskRegister(taskRegistrar);
    }

    @Override
    public Optional<AutomatedTaskEntity> getAutomatedTaskEntityByTaskName(String taskName) {
        return automatedTaskRepository.findByTaskName(taskName);
    }

    public String getAutomatedTaskCronExpression(AutomatedTask automatedTask) {
        String cronExpression = getAutomatedTaskCronExpression(automatedTask.getTaskName());
        if (cronExpression == null) {
            log.info("Unable to find cron expression for task: {}", automatedTask.getTaskName());
            throw new DartsApiException(INVALID_CRON_EXPRESSION);
        }
        return cronExpression;
    }

    public String getAutomatedTaskCronExpression(String taskName) {
        String cronExpression;
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskEntityByTaskName(taskName);
        if (automatedTaskEntity.isPresent()) {
            cronExpression = automatedTaskEntity.get().getCronExpression();
            log.debug("Task: {} cron expression: {}", taskName, cronExpression);
        } else {
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
        return cronExpression;
    }

    /**
     * Updates the automated task cron expression or throws DartsApiException.
     * Calling this without the automated task being cancelled first, and then reloaded means the cron expression will only get picked up
     * after the next execution once the automated task has run and then calculates the next execution time. Cancelling
     * the automated task first means it will use the given cronExpression if the cron expression is valid.
     *
     * @param taskName       name of automated task
     * @param cronExpression cron expression
     * @return true if the automated task is successfully updated
     */
    @Override
    public boolean updateAutomatedTaskCronExpression(String taskName, String cronExpression) {
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskEntityByTaskName(taskName);
        if (automatedTaskEntity.isPresent()) {
            AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
            if (CronExpression.isValidExpression(cronExpression)) {
                automatedTask.setCronExpression(cronExpression);
                automatedTaskRepository.saveAndFlush(automatedTask);
                log.debug("Updated the task {} with cron expression {}", taskName, cronExpression);
            } else {
                log.error("Unable to update the task {} with cron expression {}", taskName, cronExpression);
                throw new DartsApiException(INVALID_CRON_EXPRESSION);
            }
        } else {
            log.error("Failed to update the task {} with cron expression {}", taskName, cronExpression);
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
        return true;
    }

    @Override
    public void reloadTaskByName(String taskName) {
        if (isNull(AutomatedTaskName.valueOfTaskName(taskName))) {
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
        switch (AutomatedTaskName.valueOfTaskName(taskName)) {
            case PROCESS_DAILY_LIST_TASK_NAME -> rescheduleProcessDailyListAutomatedTask();
            case CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME -> rescheduleCloseNonCompletedTranscriptionsAutomatedTask();
            case OUTBOUND_AUDIO_DELETER_TASK_NAME -> rescheduleOutboundAudioDeleterAutomatedTask();
            case INBOUND_TO_UNSTRUCTURED_TASK_NAME -> rescheduleInboundToUnstructuredAutomatedTask();
            case EXTERNAL_DATASTORE_DELETER_TASK_NAME -> rescheduleExternalDataStoreDeleterAutomatedTask();
            case INBOUND_AUDIO_DELETER_TASK_NAME -> rescheduleInboundAudioDeleterAutomatedTask();
            case UNSTRUCTURED_AUDIO_DELETER_TASK_NAME -> rescheduleUnstructuredAudioDeleterAutomatedTask();
            case UNSTRUCTURED_TO_ARM_TASK_NAME -> rescheduleUnstructuredToArmAutomatedTask();
            case PROCESS_ARM_RESPONSE_FILES_TASK_NAME -> rescheduleProcessArmResponseFilesAutomatedTask();
            case APPLY_RETENTION_TASK_NAME -> rescheduleApplyRetentionAutomatedTask();
            case APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME -> rescheduleCaseObjectApplyRetentionAutomatedTask();
            case CLEANUP_ARM_RESPONSE_FILES_TASK_NAME -> rescheduleCleanupArmResponseFilesAutomatedTask();
            case CLOSE_OLD_CASES_TASK_NAME -> rescheduleCloseOldCasesAutomatedTask();

            case DAILY_LIST_HOUSEKEEPING_TASK_NAME -> rescheduleDailyListHousekeepingAutomatedTask();
            case ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME -> rescheduleArmRetentionEventDateCalculatorAutomatedTask();
            default -> throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
    }

    @Override
    public boolean cancelAutomatedTaskAndUpdateCronExpression(String taskName, boolean mayInterruptIfRunning, String cronExpression) {
        boolean cancelled = cancelAutomatedTask(taskName, mayInterruptIfRunning);
        boolean updatedCronExpresssion = false;
        if (cancelled) {
            updatedCronExpresssion = updateAutomatedTaskCronExpression(taskName, cronExpression);
            if (updatedCronExpresssion) {
                reloadTaskByName(taskName);
            } else {
                throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
            }
        }
        return cancelled && updatedCronExpresssion;
    }

    @Override
    public boolean cancelAutomatedTask(String taskName, boolean mayInterruptIfRunning) {
        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            Task task = scheduledTask.getTask();
            if (task instanceof TriggerTask triggerTask && cancelTriggerTask(
                taskName,
                scheduledTask,
                triggerTask,
                mayInterruptIfRunning
            )) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AutomatedTaskStatus getAutomatedTaskStatus(String taskName) {
        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            Task task = scheduledTask.getTask();
            if (task instanceof TriggerTask triggerTask
                && triggerTask.getRunnable() instanceof AutomatedTask automatedTask
                && automatedTask.getTaskName().equals(taskName)) {
                return automatedTask.getAutomatedTaskStatus();
            }
        }
        throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
    }

    @Override
    public List<AbstractLockableAutomatedTask> getAutomatedTasks() {
        return automatedTasks;
    }

    /**
     * Sets up the ProcessDailyListAutomatedTask and adds it to the task registrar which then makes it available to the
     * TaskScheduler.
     *
     * @param taskRegistrar Registers scheduled tasks
     */
    private void addProcessDailyListToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        ProcessDailyListAutomatedTask processDailyListAutomatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            dailyListProcessor,
            logApi
        );
        processDailyListAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(processDailyListAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(processDailyListAutomatedTask);
        taskRegistrar.addTriggerTask(processDailyListAutomatedTask, trigger);
        automatedTasks.add(processDailyListAutomatedTask);

    }

    private void addInboundAudioDeleterToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        InboundAudioDeleterAutomatedTask inboundAudioDeleterAutomatedTask = new InboundAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundAudioDeleterProcessor,
            logApi
        );
        inboundAudioDeleterAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(
            inboundAudioDeleterAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(inboundAudioDeleterAutomatedTask);
        taskRegistrar.addTriggerTask(inboundAudioDeleterAutomatedTask, trigger);
        automatedTasks.add(inboundAudioDeleterAutomatedTask);
    }

    /**
     * Sets up the OutboundAudioDeleter and adds it to the task registrar which then makes it available to the
     * TaskScheduler.
     *
     * @param taskRegistrar Registers scheduled tasks
     */
    private void addOutboundAudioDeleterToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        OutboundAudioDeleterAutomatedTask outboundAudioDeleterAutomatedTask = new OutboundAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            outboundAudioDeleterProcessor,
            logApi
        );
        outboundAudioDeleterAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(outboundAudioDeleterAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(outboundAudioDeleterAutomatedTask);
        taskRegistrar.addTriggerTask(outboundAudioDeleterAutomatedTask, trigger);
        automatedTasks.add(outboundAudioDeleterAutomatedTask);
    }

    private void addInboundToUnstructuredTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        InboundToUnstructuredAutomatedTask inboundToUnstructuredAutomatedTask = new InboundToUnstructuredAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundToUnstructuredProcessor,
            logApi
        );
        inboundToUnstructuredAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(inboundToUnstructuredAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(inboundToUnstructuredAutomatedTask);
        taskRegistrar.addTriggerTask(inboundToUnstructuredAutomatedTask, trigger);
        automatedTasks.add(inboundToUnstructuredAutomatedTask);
    }

    /**
     * Sets up the ExternalDataStoreDeleter and adds it to the task registrar which then makes it available to the
     * TaskScheduler.
     *
     * @param taskRegistrar Registers scheduled tasks
     */
    private void addExternalDataStoreDeleterToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask = new ExternalDataStoreDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundDataStoreDeleter,
            unstructuredDataStoreDeleter,
            outboundDataStoreDeleter,
            logApi
        );
        externalDataStoreDeleterAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(externalDataStoreDeleterAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(externalDataStoreDeleterAutomatedTask);
        taskRegistrar.addTriggerTask(externalDataStoreDeleterAutomatedTask, trigger);
        automatedTasks.add(externalDataStoreDeleterAutomatedTask);
    }

    private void addCloseNonCompletedTranscriptionsAutomatedTaskToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        CloseUnfinishedTranscriptionsAutomatedTask closeUnfinishedTranscriptionsAutomatedTask = new CloseUnfinishedTranscriptionsAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            transcriptionsProcessor,
            logApi
        );
        closeUnfinishedTranscriptionsAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(closeUnfinishedTranscriptionsAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(closeUnfinishedTranscriptionsAutomatedTask);
        taskRegistrar.addTriggerTask(closeUnfinishedTranscriptionsAutomatedTask, trigger);
        automatedTasks.add(closeUnfinishedTranscriptionsAutomatedTask);
    }

    private void addUnstructuredAudioDeleterAutomatedTaskToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        UnstructuredAudioDeleterAutomatedTask unstructuredAudioDeleterAutomatedTask = new UnstructuredAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            unstructuredAudioDeleterProcessor,
            logApi
        );
        unstructuredAudioDeleterAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(unstructuredAudioDeleterAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(unstructuredAudioDeleterAutomatedTask);
        taskRegistrar.addTriggerTask(unstructuredAudioDeleterAutomatedTask, trigger);
        automatedTasks.add(unstructuredAudioDeleterAutomatedTask);
    }

    private void addUnstructuredToArmTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask = new UnstructuredToArmAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            unstructuredToArmProcessor,
            logApi
        );
        unstructuredToArmAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(unstructuredToArmAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(unstructuredToArmAutomatedTask);
        taskRegistrar.addTriggerTask(unstructuredToArmAutomatedTask, trigger);
        automatedTasks.add(unstructuredToArmAutomatedTask);
    }

    private void addProcessArmResponseFilesTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesTask = new ProcessArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            armResponseFilesProcessor,
            logApi
        );
        processArmResponseFilesTask.setLastCronExpression(getAutomatedTaskCronExpression(processArmResponseFilesTask));
        Trigger trigger = createAutomatedTaskTrigger(processArmResponseFilesTask);
        taskRegistrar.addTriggerTask(processArmResponseFilesTask, trigger);
        automatedTasks.add(processArmResponseFilesTask);
    }

    private void addApplyRetentionToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        ApplyRetentionAutomatedTask applyRetentionAutomatedTask = new ApplyRetentionAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            applyRetentionProcessor,
            logApi
        );
        applyRetentionAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(applyRetentionAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(applyRetentionAutomatedTask);
        taskRegistrar.addTriggerTask(applyRetentionAutomatedTask, trigger);
        automatedTasks.add(applyRetentionAutomatedTask);
    }

    private void addCaseObjectApplyRetentionToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        var applyRetentionCaseAssociatedObjectsAutomatedTask = new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            applyRetentionCaseAssociatedObjectsProcessor,
            logApi
        );
        applyRetentionCaseAssociatedObjectsAutomatedTask.setLastCronExpression(
            getAutomatedTaskCronExpression(applyRetentionCaseAssociatedObjectsAutomatedTask)
        );
        Trigger trigger = createAutomatedTaskTrigger(applyRetentionCaseAssociatedObjectsAutomatedTask);
        taskRegistrar.addTriggerTask(applyRetentionCaseAssociatedObjectsAutomatedTask, trigger);
        automatedTasks.add(applyRetentionCaseAssociatedObjectsAutomatedTask);
    }

    private void addCleanupArmResponseFilesTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        CleanupArmResponseFilesAutomatedTask cleanupArmResponseFilesAutomatedTask = new CleanupArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            cleanupArmResponseFilesService,
            logApi
        );
        cleanupArmResponseFilesAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(cleanupArmResponseFilesAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(cleanupArmResponseFilesAutomatedTask);
        taskRegistrar.addTriggerTask(cleanupArmResponseFilesAutomatedTask, trigger);
        automatedTasks.add(cleanupArmResponseFilesAutomatedTask);
    }

    private void addCloseOldCasesTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        CloseOldCasesAutomatedTask closeOldCasesAutomatedTask = new CloseOldCasesAutomatedTask(automatedTaskRepository,
                                                                                               lockProvider,
                                                                                               automatedTaskConfigurationProperties,
                                                                                               closeOldCasesProcessor,
                                                                                               logApi);
        closeOldCasesAutomatedTask.setLastCronExpression(getAutomatedTaskCronExpression(closeOldCasesAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(closeOldCasesAutomatedTask);
        taskRegistrar.addTriggerTask(closeOldCasesAutomatedTask, trigger);
        automatedTasks.add(closeOldCasesAutomatedTask);
    }

    private void addDailyListHouseKeepingToTaskRegistrar(ScheduledTaskRegistrar taskRegistrar) {
        DailyListAutomatedTask dailyListTask = new DailyListAutomatedTask(automatedTaskRepository,
                                                                          lockProvider,
                                                                          automatedTaskConfigurationProperties,
                                                                          dailyListService,
                                                                          logApi);
        dailyListTask.setLastCronExpression(getAutomatedTaskCronExpression(dailyListTask));
        Trigger trigger = createAutomatedTaskTrigger(dailyListTask);
        taskRegistrar.addTriggerTask(dailyListTask, trigger);
        automatedTasks.add(dailyListTask);
    }


    private void addArmRetentionEventDateCalculatorToTaskRegister(ScheduledTaskRegistrar taskRegistrar) {
        ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask =
            new ArmRetentionEventDateCalculatorAutomatedTask(automatedTaskRepository,
                                                             lockProvider,
                                                             automatedTaskConfigurationProperties,
                                                             armRetentionEventDateProcessor,
                                                             logApi);
        armRetentionEventDateCalculatorAutomatedTask.setLastCronExpression(
            getAutomatedTaskCronExpression(armRetentionEventDateCalculatorAutomatedTask));
        Trigger trigger = createAutomatedTaskTrigger(armRetentionEventDateCalculatorAutomatedTask);
        taskRegistrar.addTriggerTask(armRetentionEventDateCalculatorAutomatedTask, trigger);
        automatedTasks.add(armRetentionEventDateCalculatorAutomatedTask);
    }

    private void rescheduleProcessDailyListAutomatedTask() {
        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(PROCESS_DAILY_LIST_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            ProcessDailyListAutomatedTask processDailyListAutomatedTask = new ProcessDailyListAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                dailyListProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(processDailyListAutomatedTask);
            taskScheduler.schedule(processDailyListAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleCloseNonCompletedTranscriptionsAutomatedTask() {
        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(
            CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            CloseUnfinishedTranscriptionsAutomatedTask closeUnfinishedTranscriptionsAutomatedTask = new CloseUnfinishedTranscriptionsAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                transcriptionsProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(closeUnfinishedTranscriptionsAutomatedTask);
            taskScheduler.schedule(closeUnfinishedTranscriptionsAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleInboundAudioDeleterAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(INBOUND_AUDIO_DELETER_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            InboundAudioDeleterAutomatedTask inboundAudioDeleterAutomatedTask = new InboundAudioDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                inboundAudioDeleterProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(inboundAudioDeleterAutomatedTask);
            taskScheduler.schedule(inboundAudioDeleterAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleOutboundAudioDeleterAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(OUTBOUND_AUDIO_DELETER_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            OutboundAudioDeleterAutomatedTask outboundAudioDeleterAutomatedTask = new OutboundAudioDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                outboundAudioDeleterProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(outboundAudioDeleterAutomatedTask);
            taskScheduler.schedule(outboundAudioDeleterAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleExternalDataStoreDeleterAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(EXTERNAL_DATASTORE_DELETER_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask = new ExternalDataStoreDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                inboundDataStoreDeleter,
                unstructuredDataStoreDeleter,
                outboundDataStoreDeleter,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(externalDataStoreDeleterAutomatedTask);
            taskScheduler.schedule(externalDataStoreDeleterAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleInboundToUnstructuredAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(INBOUND_TO_UNSTRUCTURED_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            InboundToUnstructuredAutomatedTask inboundToUnstructuredAutomatedTask = new InboundToUnstructuredAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                inboundToUnstructuredProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(inboundToUnstructuredAutomatedTask);
            taskScheduler.schedule(inboundToUnstructuredAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleUnstructuredAudioDeleterAutomatedTask() {
        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(UNSTRUCTURED_AUDIO_DELETER_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            UnstructuredAudioDeleterAutomatedTask unstructuredAudioDeleterAutomatedTask = new UnstructuredAudioDeleterAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                unstructuredAudioDeleterProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(unstructuredAudioDeleterAutomatedTask);
            taskScheduler.schedule(unstructuredAudioDeleterAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleUnstructuredToArmAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(UNSTRUCTURED_TO_ARM_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask = new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                unstructuredToArmProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(unstructuredToArmAutomatedTask);
            taskScheduler.schedule(unstructuredToArmAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleProcessArmResponseFilesAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(PROCESS_ARM_RESPONSE_FILES_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            ProcessArmResponseFilesAutomatedTask processArmResponseFilesTask = new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                armResponseFilesProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(processArmResponseFilesTask);
            taskScheduler.schedule(processArmResponseFilesTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleApplyRetentionAutomatedTask() {
        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(APPLY_RETENTION_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            ApplyRetentionAutomatedTask applyRetentionAutomatedTask = new ApplyRetentionAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                applyRetentionProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(applyRetentionAutomatedTask);
            taskScheduler.schedule(applyRetentionAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleCaseObjectApplyRetentionAutomatedTask() {
        var triggerAndAutomatedTask = getTriggerAndAutomatedTask(APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            var applyRetentionCaseAssociatedObjectsAutomatedTask = new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                applyRetentionCaseAssociatedObjectsProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(applyRetentionCaseAssociatedObjectsAutomatedTask);
            taskScheduler.schedule(applyRetentionCaseAssociatedObjectsAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleCleanupArmResponseFilesAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(CLEANUP_ARM_RESPONSE_FILES_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            CleanupArmResponseFilesAutomatedTask cleanupArmResponseFilesAutomatedTask = new CleanupArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                cleanupArmResponseFilesService,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(cleanupArmResponseFilesAutomatedTask);
            taskScheduler.schedule(cleanupArmResponseFilesAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleCloseOldCasesAutomatedTask() {

        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(CLOSE_OLD_CASES_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            CloseOldCasesAutomatedTask closeOldCasesAutomatedTask = new CloseOldCasesAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                closeOldCasesProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(closeOldCasesAutomatedTask);
            taskScheduler.schedule(closeOldCasesAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }


    private void rescheduleDailyListHousekeepingAutomatedTask() {

        var triggerAndAutomatedTask = getTriggerAndAutomatedTask(DAILY_LIST_HOUSEKEEPING_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {

            var dailyListAutomatedTask = new DailyListAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                dailyListService,
                logApi
            );
            var trigger = createAutomatedTaskTrigger(dailyListAutomatedTask);
            taskScheduler.schedule(dailyListAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private void rescheduleArmRetentionEventDateCalculatorAutomatedTask() {
        TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME.getTaskName());
        if (triggerAndAutomatedTask == null) {
            ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask = new ArmRetentionEventDateCalculatorAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                armRetentionEventDateProcessor,
                logApi
            );
            Trigger trigger = createAutomatedTaskTrigger(armRetentionEventDateCalculatorAutomatedTask);
            taskScheduler.schedule(armRetentionEventDateCalculatorAutomatedTask, trigger);
        } else {
            taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
        }
    }

    private TriggerAndAutomatedTask getTriggerAndAutomatedTask(String taskName) {
        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            Task task = scheduledTask.getTask();
            if (task instanceof TriggerTask triggerTask
                && triggerTask.getRunnable() instanceof AbstractLockableAutomatedTask automatedTask
                && automatedTask.getTaskName().equals(taskName)) {
                return TriggerAndAutomatedTask.builder()
                    .automatedTask(automatedTask)
                    .trigger(triggerTask.getTrigger())
                    .build();
            }
        }
        return null;
    }

    private boolean cancelTriggerTask(String taskName, ScheduledTask scheduledTask, TriggerTask triggerTask, boolean mayInterruptIfRunning) {
        if (triggerTask.getRunnable() instanceof AutomatedTask automatedTask && automatedTask.getTaskName().equals(taskName)) {
            log.info("About to cancel task: " + taskName);
            scheduledTask.cancel(mayInterruptIfRunning);
            return true;
        }
        return false;
    }

    private Trigger createAutomatedTaskTrigger(AutomatedTask automatedTask) {
        Trigger trigger = createCronTrigger(automatedTask);
        taskTriggers.put(automatedTask.getTaskName(), trigger);
        return trigger;
    }

    private Trigger createCronTrigger(AutomatedTask automatedTask) {
        return triggerContext -> {
            String cronExpression = getAutomatedTaskCronExpression(automatedTask);
            log.debug(
                "Creating trigger for task: {} with cron expression: {} ",
                automatedTask.getTaskName(),
                cronExpression
            );
            automatedTask.setLastCronExpression(cronExpression);
            CronTrigger crontrigger = new CronTrigger(cronExpression);
            return crontrigger.nextExecution(triggerContext);
        };
    }

}
