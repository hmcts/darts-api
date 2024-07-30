package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.REMOVE_DUPLICATED_EVENTS_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class RemoveDuplicatedEventsAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = REMOVE_DUPLICATED_EVENTS_TASK_NAME.getTaskName();

    private final RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor;

    public RemoveDuplicatedEventsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                               LockProvider lockProvider,
                                               AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                               RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor,
                                               LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
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
