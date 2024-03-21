package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLOSE_OLD_CASES;

@Slf4j
public class CloseOldCasesAutomatedTask extends AbstractLockableAutomatedTask {

    CloseOldCasesProcessor closeOldCasesProcessor;

    public CloseOldCasesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                      LockProvider lockProvider,
                                      AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                      CloseOldCasesProcessor closeOldCasesProcessor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.closeOldCasesProcessor = closeOldCasesProcessor;
    }

    @Override
    public String getTaskName() {
        return CLOSE_OLD_CASES.getTaskName();
    }

    @Override
    protected void runTask() {
        closeOldCasesProcessor.closeCases();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Error attempting to close cases: {}", exception.getMessage(), exception);
    }
}
