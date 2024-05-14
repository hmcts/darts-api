package uk.gov.hmcts.darts.task.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
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

    private final AutomatedTaskRepository automatedTaskRepository;
    private final LockProvider lockProvider;
    private final AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    private final DailyListProcessor dailyListProcessor;
    private final OutboundAudioDeleterProcessor outboundAudioDeleterProcessor;
    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    private final UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;
    private final TranscriptionsProcessor transcriptionsProcessor;
    private final InboundAudioDeleterProcessor inboundAudioDeleterProcessor;
    private final ExternalInboundDataStoreDeleter inboundDataStoreDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDataStoreDeleter;
    private final ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;
    private final ApplyRetentionProcessor applyRetentionProcessor;
    private final ApplyRetentionCaseAssociatedObjectsProcessor applyRetentionCaseAssociatedObjectsProcessor;
    private final CleanupArmResponseFilesService cleanupArmResponseFilesService;
    private final CloseOldCasesProcessor closeOldCasesProcessor;
    private final DailyListService dailyListService;
    private final ArmRetentionEventDateProcessor armRetentionEventDateProcessor;
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
    }

    public List<AbstractLockableAutomatedTask> getAutomatedTasks() {
        return automatedTasks;
    }

    
    private void addProcessDailyListToTaskRegistrar() {
        ProcessDailyListAutomatedTask processDailyListAutomatedTask = new ProcessDailyListAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            dailyListProcessor,
            logApi
        );
        automatedTasks.add(processDailyListAutomatedTask);
    }

    private void addInboundAudioDeleterToTaskRegistrar() {
        InboundAudioDeleterAutomatedTask inboundAudioDeleterAutomatedTask = new InboundAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundAudioDeleterProcessor,
            logApi
        );
        automatedTasks.add(inboundAudioDeleterAutomatedTask);
    }

    private void addOutboundAudioDeleterToTaskRegistrar() {
        OutboundAudioDeleterAutomatedTask outboundAudioDeleterAutomatedTask = new OutboundAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            outboundAudioDeleterProcessor,
            logApi
        );
        automatedTasks.add(outboundAudioDeleterAutomatedTask);
    }

    private void addInboundToUnstructuredTaskRegistrar() {
        InboundToUnstructuredAutomatedTask inboundToUnstructuredAutomatedTask = new InboundToUnstructuredAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundToUnstructuredProcessor,
            logApi
        );
        automatedTasks.add(inboundToUnstructuredAutomatedTask);
    }

    private void addExternalDataStoreDeleterToTaskRegistrar() {
        ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask = new ExternalDataStoreDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            inboundDataStoreDeleter,
            unstructuredDataStoreDeleter,
            outboundDataStoreDeleter,
            logApi
        );
        automatedTasks.add(externalDataStoreDeleterAutomatedTask);
    }

    private void addCloseNonCompletedTranscriptionsAutomatedTaskToTaskRegistrar() {
        CloseUnfinishedTranscriptionsAutomatedTask closeUnfinishedTranscriptionsAutomatedTask = new CloseUnfinishedTranscriptionsAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            transcriptionsProcessor,
            logApi
        );
        automatedTasks.add(closeUnfinishedTranscriptionsAutomatedTask);
    }

    private void addUnstructuredAudioDeleterAutomatedTaskToTaskRegistrar() {
        UnstructuredAudioDeleterAutomatedTask unstructuredAudioDeleterAutomatedTask = new UnstructuredAudioDeleterAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            unstructuredAudioDeleterProcessor,
            logApi
        );
        automatedTasks.add(unstructuredAudioDeleterAutomatedTask);
    }

    private void addUnstructuredToArmTaskRegistrar() {
        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask = new UnstructuredToArmAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            automatedTaskProcessorFactory,
            logApi
        );
        automatedTasks.add(unstructuredToArmAutomatedTask);
    }

    private void addProcessArmResponseFilesTaskRegistrar() {
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesTask = new ProcessArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            automatedTaskProcessorFactory,
            logApi
        );
        automatedTasks.add(processArmResponseFilesTask);
    }

    private void addApplyRetentionToTaskRegistrar() {
        ApplyRetentionAutomatedTask applyRetentionAutomatedTask = new ApplyRetentionAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            applyRetentionProcessor,
            logApi
        );
        automatedTasks.add(applyRetentionAutomatedTask);
    }

    private void addCaseObjectApplyRetentionToTaskRegistrar() {
        var applyRetentionCaseAssociatedObjectsAutomatedTask = new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            applyRetentionCaseAssociatedObjectsProcessor,
            logApi
        );
        automatedTasks.add(applyRetentionCaseAssociatedObjectsAutomatedTask);
    }

    private void addCleanupArmResponseFilesTaskRegistrar() {
        CleanupArmResponseFilesAutomatedTask cleanupArmResponseFilesAutomatedTask = new CleanupArmResponseFilesAutomatedTask(
            automatedTaskRepository,
            lockProvider,
            automatedTaskConfigurationProperties,
            cleanupArmResponseFilesService,
            logApi
        );
        automatedTasks.add(cleanupArmResponseFilesAutomatedTask);
    }

    private void addCloseOldCasesTaskRegistrar() {
        CloseOldCasesAutomatedTask closeOldCasesAutomatedTask = new CloseOldCasesAutomatedTask(automatedTaskRepository,
                                                                                               lockProvider,
                                                                                               automatedTaskConfigurationProperties,
                                                                                               closeOldCasesProcessor,
                                                                                               logApi);
        automatedTasks.add(closeOldCasesAutomatedTask);
    }

    private void addDailyListHouseKeepingToTaskRegistrar() {
        DailyListAutomatedTask dailyListTask = new DailyListAutomatedTask(automatedTaskRepository,
                                                                          lockProvider,
                                                                          automatedTaskConfigurationProperties,
                                                                          dailyListService,
                                                                          logApi);
        automatedTasks.add(dailyListTask);
    }


    private void addArmRetentionEventDateCalculatorToTaskRegister() {
        ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask =
            new ArmRetentionEventDateCalculatorAutomatedTask(automatedTaskRepository,
                                                             lockProvider,
                                                             automatedTaskConfigurationProperties,
                                                             armRetentionEventDateProcessor,
                                                             logApi);
        automatedTasks.add(armRetentionEventDateCalculatorAutomatedTask);
    }

}
