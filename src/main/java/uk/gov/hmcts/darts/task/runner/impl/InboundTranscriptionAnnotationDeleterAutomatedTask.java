package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundTranscriptionAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.InboundTranscriptionAnnotationDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME;

@Component
public class InboundTranscriptionAnnotationDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final InboundTranscriptionAnnotationDeleterProcessor annotationTranscriptionDeleterProcessor;

    @Value("${darts.automated.task.inbound-transcription-annotation-deleter.default-batch-size}")
    int defaultBatchSize;

    @Autowired
    public InboundTranscriptionAnnotationDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                              InboundTranscriptionAnnotationDeleterAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                              InboundTranscriptionAnnotationDeleterProcessor annotationTranscriptionDeleterProcessor,
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
        annotationTranscriptionDeleterProcessor.markForDeletion(getAutomatedTaskBatchSize(defaultBatchSize));
    }
}