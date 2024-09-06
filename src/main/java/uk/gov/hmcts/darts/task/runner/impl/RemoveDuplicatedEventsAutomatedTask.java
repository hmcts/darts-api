package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.REMOVE_DUPLICATED_EVENTS_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class RemoveDuplicatedEventsAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = REMOVE_DUPLICATED_EVENTS_TASK_NAME.getTaskName();

    private final RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor;

    public RemoveDuplicatedEventsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                               AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                               RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor,
                                               LogApi logApi,
                                               LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.removeDuplicateEventsProcessor = removeDuplicateEventsProcessor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();
    }
}
