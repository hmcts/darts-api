package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;

@Slf4j
@Component
@SuppressWarnings({"squid:S1135"})
public class ProcessDailyListAutomatedTask
    extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    protected String taskName = PROCESS_DAILY_LIST_TASK_NAME.getTaskName();
    private DailyListProcessor dailyListProcessor;

    private final List<AutomatedTaskStatus> trackedStateChanges = new ArrayList<>();

    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                         AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                         LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
    }

    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                         AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                         DailyListProcessor processor, LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.dailyListProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public void runTask() {
        dailyListProcessor.processAllDailyLists();
    }

    @Override
    protected void setAutomatedTaskStatus(AutomatedTaskStatus automatedTaskStatus) {
        //TODO remove override of exceptions handling on the various tasks
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
