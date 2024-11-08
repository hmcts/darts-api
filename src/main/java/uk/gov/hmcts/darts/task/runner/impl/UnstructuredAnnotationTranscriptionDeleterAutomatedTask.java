package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.UnstructuredTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;

@Component
public class UnstructuredAnnotationTranscriptionDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final UnstructuredTranscriptionAndAnnotationDeleterProcessor armDeletionProcessor;

    @Autowired
    public UnstructuredAnnotationTranscriptionDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                                   AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                                   UnstructuredTranscriptionAndAnnotationDeleterProcessor armDeletionProcessor,
                                                                   LogApi logApi,
                                                                   LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.armDeletionProcessor = armDeletionProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(90);
    }

    @Override
    protected void runTask() {
        armDeletionProcessor.markForDeletion(getAutomatedTaskBatchSize());
    }
}