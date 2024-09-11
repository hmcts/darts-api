package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;

@Component
public class InboundAnnotationTranscriptionDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    protected String taskName = INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME.getTaskName();
    private InboundAnnotationTranscriptionDeleterProcessor armDeletionProcessor;

    public InboundAnnotationTranscriptionDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                              InboundAnnotationTranscriptionDeleterProcessor armDeletionProcessor,
                                                              LogApi logApi,
                                                              LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
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