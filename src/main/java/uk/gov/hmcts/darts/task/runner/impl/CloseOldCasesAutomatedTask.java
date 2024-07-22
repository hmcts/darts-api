package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLOSE_OLD_CASES_TASK_NAME;

@Slf4j
public class CloseOldCasesAutomatedTask extends AbstractLockableAutomatedTask {

    CloseOldCasesProcessor closeOldCasesProcessor;

    public CloseOldCasesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                      AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                      CloseOldCasesProcessor closeOldCasesProcessor, LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.closeOldCasesProcessor = closeOldCasesProcessor;
    }

    @Override
    public String getTaskName() {
        return CLOSE_OLD_CASES_TASK_NAME.getTaskName();
    }

    @Override
    protected void runTask() {
        closeOldCasesProcessor.closeCases();
    }
}
