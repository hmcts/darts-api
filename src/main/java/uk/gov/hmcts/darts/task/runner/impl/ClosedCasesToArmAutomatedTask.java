package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.cases.service.ClosedCasesToArmProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLOSED_CASES_TO_ARM;

@Slf4j
public class ClosedCasesToArmAutomatedTask extends AbstractLockableAutomatedTask {

    ClosedCasesToArmProcessor closedCasesToArmProcessor;

    public ClosedCasesToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                         LockProvider lockProvider,
                                         AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                         ClosedCasesToArmProcessor closedCasesToArmProcessor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.closedCasesToArmProcessor = closedCasesToArmProcessor;
    }

    @Override
    public String getTaskName() {
        return CLOSED_CASES_TO_ARM.getTaskName();
    }

    @Override
    protected void runTask() {
        closedCasesToArmProcessor.closedCasesToArm();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Error attempting to send closed cases to ARM: {}", exception.getMessage(), exception);
    }
}
