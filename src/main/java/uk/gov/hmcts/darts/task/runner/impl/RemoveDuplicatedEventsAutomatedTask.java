package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.REMOVE_DUPLICATED_EVENTS_TASK_NAME;

@Slf4j
@Component
@SuppressWarnings({"squid:S1135"})
public class RemoveDuplicatedEventsAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor;

    @Autowired
    public RemoveDuplicatedEventsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                               AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                               RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor,
                                               LogApi logApi,
                                               LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.removeDuplicateEventsProcessor = removeDuplicateEventsProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return REMOVE_DUPLICATED_EVENTS_TASK_NAME;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(90);
    }

    @Override
    protected void runTask() {
        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();
    }
}
