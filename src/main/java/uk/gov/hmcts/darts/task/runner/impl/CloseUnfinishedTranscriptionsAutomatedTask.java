package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME;

@Slf4j
public class CloseUnfinishedTranscriptionsAutomatedTask extends AbstractLockableAutomatedTask {

    private final TranscriptionsApi transcriptionsApi;

    protected String taskName = CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME.getTaskName();

    public CloseUnfinishedTranscriptionsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
          LockProvider lockProvider,
          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
          TranscriptionsApi transcriptionsApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.transcriptionsApi = transcriptionsApi;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        transcriptionsApi.closeTranscriptions();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Error attempting to close transcriptions: {}", exception.getMessage());
    }
}
