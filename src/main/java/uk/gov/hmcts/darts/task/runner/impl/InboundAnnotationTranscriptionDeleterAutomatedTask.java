package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;

@Component
public class InboundAnnotationTranscriptionDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final InboundAnnotationTranscriptionDeleterProcessor annotationTranscriptionDeleterProcessor;

    @Autowired
    public InboundAnnotationTranscriptionDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                              InboundAnnotationTranscriptionDeleterProcessor annotationTranscriptionDeleterProcessor,
                                                              LogApi logApi,
                                                              LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.annotationTranscriptionDeleterProcessor = annotationTranscriptionDeleterProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;
    }

    @Override
    protected void runTask() {
        annotationTranscriptionDeleterProcessor.markForDeletion();
    }
}