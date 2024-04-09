package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.DAILY_LIST_HOUSEKEEPING_TASK_NAME;

@Slf4j
public class DailyListAutomatedTask extends AbstractLockableAutomatedTask {

    private final DailyListService dailyListService;

    public DailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                  LockProvider lockProvider,
                                  AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                  DailyListService dailyListService) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
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
    protected void handleException(Exception exception) {
        log.error("Error attempting to run daily list housekeeping: {}", exception.getMessage(), exception);
    }
}
