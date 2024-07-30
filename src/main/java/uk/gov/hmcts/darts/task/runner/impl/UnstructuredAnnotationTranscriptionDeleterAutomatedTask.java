package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.UnstructuredTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;

public class UnstructuredAnnotationTranscriptionDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName();
    private UnstructuredTranscriptionAndAnnotationDeleterProcessor armDeletionProcessor;

    public UnstructuredAnnotationTranscriptionDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                              LockProvider lockProvider,
                                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                                   UnstructuredTranscriptionAndAnnotationDeleterProcessor armDeletionProcessor,
                                                              LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.armDeletionProcessor = armDeletionProcessor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        armDeletionProcessor.markForDeletion();
    }
}