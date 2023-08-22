package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class ProcessDailyListAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = PROCESS_DAILY_LIST_TASK_NAME.getTaskName();


    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider) {
        super(automatedTaskRepository, lockProvider);
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        // TODO this is to be populated as a part of jira ticket https://tools.hmcts.net/jira/browse/DMP-183
    }

    @Override
    protected void handleException(Exception exception) {
        // TODO this is to be populated as a part of jira ticket https://tools.hmcts.net/jira/browse/DMP-183
        log.error("Exception: {}", exception.getMessage());
    }

}
