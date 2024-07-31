package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;

public class InboundAnnotationTranscriptionDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName();
    private InboundAnnotationTranscriptionDeleterProcessor armDeletionProcessor;

    public InboundAnnotationTranscriptionDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                              LockProvider lockProvider,
                                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                              InboundAnnotationTranscriptionDeleterProcessor armDeletionProcessor,
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