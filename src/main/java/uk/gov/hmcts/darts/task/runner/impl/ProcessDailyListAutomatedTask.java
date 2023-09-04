package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;

import java.time.LocalDate;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class ProcessDailyListAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = PROCESS_DAILY_LIST_TASK_NAME.getTaskName();
    private DailyListProcessor dailyListProcessor;


    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider) {
        super(automatedTaskRepository, lockProvider);
    }

    public ProcessDailyListAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider, DailyListProcessor processor) {
        super(automatedTaskRepository, lockProvider);
        this.dailyListProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        dailyListProcessor.processAllDailyLists(LocalDate.now());
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }

}
