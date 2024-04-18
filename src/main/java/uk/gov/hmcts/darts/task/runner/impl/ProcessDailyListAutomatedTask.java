package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class ProcessDailyListAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = PROCESS_DAILY_LIST_TASK_NAME.getTaskName();
    private DailyListProcessor dailyListProcessor;

    private List<AutomatedTaskStatus> trackedStateChanges = new ArrayList<>();

    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                         AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties, LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
    }

    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                         AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                         DailyListProcessor processor, LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.dailyListProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        dailyListProcessor.processAllDailyLists();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }


    @Override
    protected void setAutomatedTaskStatus(AutomatedTaskStatus automatedTaskStatus) {
        super.setAutomatedTaskStatus(automatedTaskStatus);
        trackedStateChanges.add(automatedTaskStatus);
    }

    public boolean hasTransitionState(AutomatedTaskStatus automatedTaskStatus) {
        if (trackedStateChanges.isEmpty()) {
            trackedStateChanges.add(getAutomatedTaskStatus());
        }
        return trackedStateChanges.contains(automatedTaskStatus);
    }
}
