package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.CloseUnfinishedTranscriptionsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsProcessor;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME;

@Slf4j
@Component
public class CloseUnfinishedTranscriptionsAutomatedTask 
    extends AbstractLockableAutomatedTask<CloseUnfinishedTranscriptionsAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final TranscriptionsProcessor transcriptionsProcessor;

    @Autowired
    public CloseUnfinishedTranscriptionsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                      CloseUnfinishedTranscriptionsAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                      TranscriptionsProcessor transcriptionsProcessor,
                                                      LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.transcriptionsProcessor = transcriptionsProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME;
    }

    @Override
    protected void runTask() {
        transcriptionsProcessor.closeTranscriptions(getAutomatedTaskBatchSize());
    }
}
