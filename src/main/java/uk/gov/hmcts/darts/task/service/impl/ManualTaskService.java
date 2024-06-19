package uk.gov.hmcts.darts.task.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ApplyRetentionAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ApplyRetentionCaseAssociatedObjectsAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ArmRetentionEventDateCalculatorAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.BatchCleanupArmResponseFilesAutomatedTask;
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
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsProcessor;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualTaskService {

    private final ApplyRetentionCaseAssociatedObjectsProcessor applyRetentionCaseAssociatedObjectsProcessor;
    private final ApplyRetentionProcessor applyRetentionProcessor;
    private final ArmRetentionEventDateProcessor armRetentionEventDateProcessor;
    private final AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;
    private final AutomatedTaskRepository automatedTaskRepository;
    private final BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService;
    private final CleanupArmResponseFilesService cleanupArmResponseFilesService;
    private final CloseOldCasesProcessor closeOldCasesProcessor;
    private final DailyListProcessor dailyListProcessor;
    private final DailyListService dailyListService;
    private final ExternalInboundDataStoreDeleter inboundDataStoreDeleter;
    private final ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDataStoreDeleter;
    private final InboundAudioDeleterProcessor inboundAudioDeleterProcessor;
    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    private final OutboundAudioDeleterProcessor outboundAudioDeleterProcessor;
    private final TranscriptionsProcessor transcriptionsProcessor;
    private final UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;

    private final LockProvider lockProvider;
    private final LogApi logApi;

    private final List<AbstractLockableAutomatedTask> automatedTasks = new ArrayList<>();

    @PostConstruct
    public void configureAndLoadAutomatedTasks() {
        addProcessDailyListToTaskRegistrar();
        addCloseNonCompletedTranscriptionsAutomatedTaskToTaskRegistrar();
        addOutboundAudioDeleterToTaskRegistrar();
        addInboundToUnstructuredTaskRegistrar();
        addInboundAudioDeleterToTaskRegistrar();
        addExternalDataStoreDeleterToTaskRegistrar();
        addUnstructuredAudioDeleterAutomatedTaskToTaskRegistrar();
        addUnstructuredToArmTaskRegistrar();
        addProcessArmResponseFilesTaskRegistrar();
        addApplyRetentionToTaskRegistrar();
        addCaseObjectApplyRetentionToTaskRegistrar();
        addCleanupArmResponseFilesTaskRegistrar();
        addCloseOldCasesTaskRegistrar();
        addDailyListHouseKeepingToTaskRegistrar();
        addArmRetentionEventDateCalculatorToTaskRegister();
        addBatchCleanupArmResponseFilesTaskRegistrar();
    }

    public List<AbstractLockableAutomatedTask> getAutomatedTasks() {
        return automatedTasks;
    }


    private void addProcessDailyListToTaskRegistrar() {
        var manualTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            dailyListProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addInboundAudioDeleterToTaskRegistrar() {
        var manualTask = new InboundAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundAudioDeleterProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addOutboundAudioDeleterToTaskRegistrar() {
        var manualTask = new OutboundAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            outboundAudioDeleterProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addInboundToUnstructuredTaskRegistrar() {
        var manualTask = new InboundToUnstructuredAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundToUnstructuredProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addExternalDataStoreDeleterToTaskRegistrar() {
        var manualTask = new ExternalDataStoreDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundDataStoreDeleter,
            unstructuredDataStoreDeleter,
            outboundDataStoreDeleter,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addCloseNonCompletedTranscriptionsAutomatedTaskToTaskRegistrar() {
        var manualTask = new CloseUnfinishedTranscriptionsAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            transcriptionsProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addUnstructuredAudioDeleterAutomatedTaskToTaskRegistrar() {
        var manualTask = new UnstructuredAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            unstructuredAudioDeleterProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addUnstructuredToArmTaskRegistrar() {
        var manualTask = new UnstructuredToArmAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            automatedTaskProcessorFactory,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addProcessArmResponseFilesTaskRegistrar() {
        var manualTask = new ProcessArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            automatedTaskProcessorFactory,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addApplyRetentionToTaskRegistrar() {
        var manualTask = new ApplyRetentionAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            applyRetentionProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addCaseObjectApplyRetentionToTaskRegistrar() {
        var manualTask = new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            applyRetentionCaseAssociatedObjectsProcessor,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addCleanupArmResponseFilesTaskRegistrar() {
        var manualTask = new CleanupArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            cleanupArmResponseFilesService,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addBatchCleanupArmResponseFilesTaskRegistrar() {
        var manualTask = new BatchCleanupArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            batchCleanupArmResponseFilesService,
            logApi
        );
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addCloseOldCasesTaskRegistrar() {
        var manualTask = new CloseOldCasesAutomatedTask(automatedTaskRepository,
                                                        lockProvider,
                                                        automatedTaskConfigurationProperties,
                                                        closeOldCasesProcessor,
                                                        logApi);
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

    private void addDailyListHouseKeepingToTaskRegistrar() {
        var manualTask = new DailyListAutomatedTask(automatedTaskRepository,
                                                    lockProvider,
                                                    automatedTaskConfigurationProperties,
                                                    dailyListService,
                                                    logApi);
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }


    private void addArmRetentionEventDateCalculatorToTaskRegister() {
        var manualTask = new ArmRetentionEventDateCalculatorAutomatedTask(automatedTaskRepository,
                                                                          lockProvider,
                                                                          automatedTaskConfigurationProperties,
                                                                          armRetentionEventDateProcessor,
                                                                          logApi);
        manualTask.setManualTask();
        automatedTasks.add(manualTask);
    }

}
