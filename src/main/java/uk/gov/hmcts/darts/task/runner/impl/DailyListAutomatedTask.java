package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DAILY_LIST_HOUSEKEEPING_TASK_NAME;

@Slf4j
@Component
public class DailyListAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final DailyListService dailyListService;

    public DailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                  AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                  DailyListService dailyListService,
                                  LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.dailyListService = dailyListService;
    }

    @Override
    public String getTaskName() {
        return DAILY_LIST_HOUSEKEEPING_TASK_NAME.getTaskName();
    }

    @Override
    protected void runTask() {
        dailyListService.runHouseKeeping();
    }

    @Override
    public AbstractLockableAutomatedTask getAbstractLockableAutomatedTask() {
        return this;
    }
}
